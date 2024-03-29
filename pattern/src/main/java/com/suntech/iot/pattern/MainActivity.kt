package com.suntech.iot.pattern

import android.content.*
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.base.BaseFragment
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.common.Constants
import com.suntech.iot.pattern.db.DBHelperForDesign
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.db.DBHelperForReport
import com.suntech.iot.pattern.db.DBHelperForTarget
import com.suntech.iot.pattern.popup.*
import com.suntech.iot.pattern.service.UsbService
import com.suntech.iot.pattern.util.OEEUtil
import com.suntech.iot.pattern.util.UtilFile
import com.suntech.iot.pattern.util.UtilLocalStorage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.layout_bottom_info_3.view.*
import kotlinx.android.synthetic.main.layout_side_menu.*
import kotlinx.android.synthetic.main.layout_top_menu.*
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*

class MainActivity : BaseActivity() {

    //    var countViewType = 1       // Count view 화면값 1=Total count, 2=Component count
    var workSheetToggle = false       // 워크시트를 토글할 것인지. 토글 시간(초)는 세팅 메뉴에서 설정
    var workSheetShow = false

    var _availability_rate = 0F
    var _quality_rate = 0F
    var _performance_rate = 0F
    var _oee_rate = 0F

    var _prepare_time = 0L            // 직전의 카운트와 새로 들어온 카운트 사이의 시간차 (밀리세컨, 처음엔 0)

    val _target_db = DBHelperForTarget(this)    // 날짜의 Shift별 정보, Target 수량 정보 저장
    val _report_db = DBHelperForReport(this)    // 날짜의 Shift별 한시간 간격의 Actual 수량 저장

    // 서버와 시간이 10초이상 차이나면 false / 10초 이하면 true
    // false 이면 30초마다 Toast 출력하고 재검사
    // true 이면 1시간 마다 다시 검사
    var _sync_time = false

//    var _first_count = false          // 앱 시작후 첫번째 카운트가 들어온 순간부터 Downtime 체크하기 위한 변수

    private val _broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
//                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
//                    btn_wifi_state.isSelected = true
//                } else {
//                    btn_wifi_state.isSelected = false
//                }
                btn_wifi_state?.isSelected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)
            }
            if (action.equals(Constants.BR_ADD_COUNT)) {
                handleData("{\"cmd\" : \"count\", \"value\" : 1}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppGlobal.instance.setContext(this)

        AppGlobal.instance.set_first_count(false)

        mHandler = MyHandler(this)

        // fragment & swipe
        val adapter = TabAdapter(supportFragmentManager)
        adapter.addFragment(HomeFragment(), "")
        adapter.addFragment(CountViewFragment(), "")
        vp_fragments.adapter = adapter
        adapter.notifyDataSetChanged()

        vp_fragments.setPagingEnabled(false)
        vp_fragments.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(state: Int) {
                (adapter.getItem(state) as BaseFragment).onSelected()
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageScrollStateChanged(position: Int) {}
        })

        btn_usb_state?.isSelected = false   // USB state init
        wv_view_main.setInitialScale(100)   // image view page init

        onClickEvent()  // buttons click event

        val work_idx = AppGlobal.instance.get_product_idx()     // 시작시 work_idx 값이 없으면 초기화 한다.
        if (work_idx == "") {
            endTodayWork()
        }

        start_timer()           // 스케줄 등록
        RemoveDownTimeData()    // 지난 DownTime 데이터 삭제
        checkDesignData()       // 지난 Design 데이터 삭제


        val filter = IntentFilter()
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(UsbService.ACTION_NO_USB)
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED)
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED)
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)
        registerReceiver(mUsbReceiver, filter)

        // 처리되지 않은 Downtime이 있으면 창을 띄운다.
        val down_db = DBHelperForDownTime(this)
        if (down_db.count_for_notcompleted() > 0) {
            startDowntimeActivity()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        unregisterReceiver(mUsbReceiver)
        cancel_timer()
    }

    public override fun onResume() {
        super.onResume()

        startService(UsbService::class.java, usbConnection, null) // Start UsbService(if it was not started before) and Bind it
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
        registerReceiver(_broadcastReceiver, IntentFilter(Constants.BR_ADD_COUNT))

        // Actual 값을 좌측에 표시
        val actual_cnt: Float = AppGlobal.instance.get_current_shift_actual_cnt()
        tv_report_count?.text = "$actual_cnt"

        // USB state
        btn_usb_state?.isSelected = AppGlobal.instance.get_usb_connect()
        btn_wifi_state?.isSelected = AppGlobal.instance.isOnline(this)

        // 화면 갱신될때마다 로드
        fetchRequiredData()
        fetchRequiredDataHour()
    }

    public override fun onPause() {
        super.onPause()
        unbindService(usbConnection)
        unregisterReceiver(_broadcastReceiver)
    }

    private var _doubleBackToExitPressedOnce = false

    override fun onBackPressed() {
        if (vp_fragments.currentItem != 0) {
            changeFragment(0)
            return
        }
        if (_doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this._doubleBackToExitPressedOnce = true
        ToastOut(this, "Please click BACK again to exit", true)
        Handler().postDelayed({ _doubleBackToExitPressedOnce = false }, 2000)
    }

    var watching_count = 0      // 디버깅 창용 변수

    fun onClickEvent() {
        // button click event
        if (AppGlobal.instance.get_long_touch()) {
            img_btn_home.setOnLongClickListener { changeFragment(0); true }
            img_btn_push_to_app.setOnLongClickListener {
                startActivity(Intent(this, PushActivity::class.java)); true
            }
            img_btn_actual_count_edit.setOnLongClickListener {
                startActivity(Intent(this, ActualCountEditActivity::class.java)); true
            }
            img_btn_downtime.setOnLongClickListener { startDowntimeActivity(); true }
            img_btn_defective_info.setOnLongClickListener {
                startActivity(Intent(this, DefectiveActivity::class.java)); true
            }
            img_btn_worksheet.setOnLongClickListener { startWorkSheetActivity(); true }
            img_btn_component.setOnLongClickListener { startComponentActivity(); true }

            img_btn_production_report.setOnLongClickListener {
                startActivity(Intent(this, ProductionReportActivity::class.java)); true
            }

            btn_worksheet_stop.setOnLongClickListener { stopWorkSheet(); true }

        } else {
            img_btn_home.setOnClickListener { changeFragment(0) }
            img_btn_push_to_app.setOnClickListener {
                startActivity(Intent(this, PushActivity::class.java))
            }
            img_btn_actual_count_edit.setOnClickListener {
                startActivity(Intent(this, ActualCountEditActivity::class.java))
            }
            img_btn_downtime.setOnClickListener { startDowntimeActivity() }
            img_btn_defective_info.setOnClickListener {
                startActivity(Intent(this, DefectiveActivity::class.java))
            }
            img_btn_worksheet.setOnClickListener { startWorkSheetActivity() }
            img_btn_component.setOnClickListener { startComponentActivity() }

            img_btn_production_report.setOnClickListener {
                startActivity(Intent(this, ProductionReportActivity::class.java))
            }

            btn_worksheet_stop.setOnClickListener { stopWorkSheet() }
        }

        // Stitch 발생
//        start_stitch?.setOnClickListener {
//            val buffer = "{\"cmd\":\"stitch\", \"value\":\"start\"}"
//            handleData(buffer)
//        }
//        start_count?.setOnClickListener {
//            val buffer = "{\"cmd\":\"count\", \"value\":\"1\", \"runtime\":\"\"}"
//            handleData(buffer)
//        }
//        delete_db?.setOnClickListener {
//            endTodayWork()
//        }
//        log_print?.setOnClickListener {
//            val design_db = DBHelperForDesign(this)
//            val dlist = design_db.gets()
//            Log.e("DEBUG INFO", "Design Data : ")
//            for (i in 0..((dlist?.size ?: 1) - 1)) {
//                val item = dlist?.get(i)
//                Log.e("DEBUG INFO", "Design Data : "+item.toString())
//            }
//
//            val down_db = DBHelperForDownTime(this)
//            val dnlist = down_db.gets()
//            Log.e("DEBUG INFO", "Downtime Data : ")
//            for (i in 0..((dnlist?.size ?: 1) - 1)) {
//                val item = dnlist?.get(i)
//                Log.e("DEBUG INFO", "Downtime Data : "+item.toString())
//            }
//        }


        // Watching debugging window
        top_logo.setOnClickListener {
            if (watching_count >= 4) {
                watching_count = 0
                startActivity(Intent(this, WatchingActivity::class.java))
            }
            watching_count++
            Handler().postDelayed({ watching_count = 0 }, 2000)
        }
    }

    fun stopWorkSheet() {
        workSheetToggle = false
        workSheetShow = false
        ll_worksheet_view?.visibility = View.GONE
        val cview = vp_fragments?.getChildAt(1)
        cview?.btn_toggle_sop?.visibility = View.VISIBLE
    }

    fun startWorkSheetActivity() {
        startActivity(Intent(this, WorkSheetActivity::class.java), { r, c, m, d ->
            if (r && d != null) {
                val file_url = d["file_url"]!!.toString()
                val ext = UtilFile.getFileExt(file_url)
                if (ext.toLowerCase() == "pdf") {
                    workSheetToggle = false
                    workSheetShow = false
                    ll_worksheet_view?.visibility = View.GONE
                    wv_view_main?.visibility = View.GONE
                } else {
                    workSheetToggle = true
                    workSheetShow = true
                    ll_worksheet_view?.visibility = View.VISIBLE
                    wv_view_main?.visibility = View.VISIBLE
                    val data =
                        "<html><head><title>Example</title></head><body style=\"margin:0; padding:0; text-align:center;\"><center><img width=\"100%\" src=\"${file_url}\" /></center></body></html>"
                    wv_view_main?.loadData(data, "text/html", null)
                    changeFragment(2)
                    val cview = vp_fragments?.getChildAt(1)
                    cview?.btn_toggle_sop?.visibility = View.GONE
                }
            }
        })
    }


    // 시작시 호출, 화면 갱신시 호출. 이후 10분에 한번씩 호출
    // 서버에 작업시간, 다운타임 기본시간, 색삭값을 호출
    private fun fetchRequiredData() {
        if (AppGlobal.instance.get_server_ip().trim() != "") {
            fetchWorkData2()         // 작업시간
            fetchDesignData()
            fetchDownTimeType()
            fetchColorData()
            fetchServerTargetData()
        }
    }
    // 시작시 호출, 화면 갱신시 호출, 이후 1시간마다 호출
    private fun fetchRequiredDataHour() {
        if (AppGlobal.instance.get_server_ip().trim() != "") {
            fetchPushData()         // Push Text 정보. 1시간마다 실행
            fetchComponentData()    // Parts Cycle Time. 처음 실행후 1시간마다 실행
            fetchDowntimeList()     // Downtime List가 팝업창에 안나오는 문제가 있어서 메인에서 미리 읽는다.
        }
    }

    private fun fetchManualShift(): JSONObject? {
        // manual 데이터가 있으면 가져온다.
        val manual = AppGlobal.instance.get_work_time_manual()
        if (manual != null && manual.length()>0) {
            val available_stime = manual.getString("available_stime") ?: ""
            val available_etime = manual.getString("available_etime") ?: ""
            var planned1_stime = manual.getString("planned1_stime") ?: ""
            var planned1_etime = manual.getString("planned1_etime") ?: ""

            if (available_stime != "" && available_etime != "") {
                if (planned1_stime == "" || planned1_etime == "") {
                    planned1_stime = ""
                    planned1_etime = ""
                }
                var shift3 = JSONObject()
                shift3.put("idx", "0")
//                shift3.put("date", dt.toString("yyyy-MM-dd"))
                shift3.put("available_stime", available_stime)
                shift3.put("available_etime", available_etime)
                shift3.put("planned1_stime", planned1_stime)
                shift3.put("planned1_etime", planned1_etime)
                shift3.put("planned2_stime", "")
                shift3.put("planned2_etime", "")
                shift3.put("planned3_stime", "")
                shift3.put("planned3_etime", "")
                shift3.put("over_time", "0")
//                shift3.put("line_idx", "0")
//                shift3.put("line_name", "")
                shift3.put("shift_idx", "3")
                shift3.put("shift_name", "SHIFT 3")
//                shift3.put("work_stime", "600")
//                shift3.put("work_etime", "600")
//                shift3.put("planned1_stime_dt", "600")
//                shift3.put("planned1_etime_dt", "600")
//                shift3.put("planned2_stime_dt", "600")
//                shift3.put("planned2_etime_dt", "600")
                return shift3
            }
        }
        return null
    }


    /*
     *  당일 작업시간 가져오기. 새벽이 지난 시간은 1일을 더한다.
     *  전일 작업이 끝나지 않았을수 있기 때문에 전일 데이터도 가져온다.
     *
     *  Result :
     *  {
            "code": "00",
            "msg": "조회성공",
            "item": [],
            "item1": [
                {
                    "idx": "1860",
                    "date": "2020-04-06",
                    "available_stime": "08:25",
                    "available_etime": "20:25",
                    "planned1_stime": "10:25",
                    "planned1_etime": "11:25",
                    "planned2_stime": "15:25",
                    "planned2_etime": "16:25",
                    "planned3_stime": "",
                    "planned3_etime": "",
                    "over_time": "0",
                    "line_idx": "1",
                    "line_name": "CELL 41",
                    "shift_idx": "1",
                    "shift_name": "SHIFT 1",
                    "target": "600"
                },
                {
                    "idx": "1861",
                    "date": "2020-04-06",
                    "available_stime": "20:25",
                    "available_etime": "08:25",
                    "planned1_stime": "23:25",
                    "planned1_etime": "00:25",
                    "planned2_stime": "04:25",
                    "planned2_etime": "05:25",
                    "planned3_stime": "",
                    "planned3_etime": "",
                    "over_time": "0",
                    "line_idx": "1",
                    "line_name": "CELL 41",
                    "shift_idx": "2",
                    "shift_name": "SHIFT 2",
                    "target": "600"
                }
            ],
            "item2": []
        }
     */
    private fun fetchWorkData2() {

        val dt = DateTime()
        val shift3: JSONObject? = fetchManualShift()      // manual 데이터가 있으면 가져온다.
        val line_idx = if (AppGlobal.instance.get_line_idx() != "") AppGlobal.instance.get_line_idx() else "0"

        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "work_time2",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx(),
            "line_idx" to line_idx,
            "today" to dt.toString("yyyy-MM-dd"),
            "yesterday" to dt.minusDays(1).toString("yyyy-MM-dd"))  // 전일 데이터

//        val uri = "/hwi/query.php"
//        val params = listOf(
//            "code" to "get_workTime",
//            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
//            "factory_idx" to AppGlobal.instance.get_zone_idx(),
//            "line_idx" to line_idx,
//            "today" to dt.toString("yyyy-MM-dd"),
//            "yesterday" to dt.minusDays(1).toString("yyyy-MM-dd"))  // 전일 데이터

//        OEEUtil.LogWrite(params.toString(), "Shift worktime Request params")

        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                var list1 = result.getJSONArray("item1")
                var list2 = result.getJSONArray("item2")
                if (shift3 != null) {
                    val today_shift = shift3
                    if (list1.length()>0) {
                        val item = list1.getJSONObject(0)
                        today_shift.put("date", item["date"])
                        today_shift.put("line_idx", item["line_idx"])
                        today_shift.put("line_name", item["line_name"])
                    } else {
                        today_shift.put("date", dt.toString("yyyy-MM-dd"))
                        today_shift.put("line_idx", line_idx)
                        today_shift.put("line_name", "Manual")
                    }
                    list1.put(today_shift)

                    val yester_shift = shift3
                    if (list2.length()>0) {
                        val item = list2.getJSONObject(0)
                        yester_shift.put("date", item["date"])
                        yester_shift.put("line_idx", item["line_idx"])
                        yester_shift.put("line_name", item["line_name"])
                    } else {
                        yester_shift.put("date", dt.minusDays(1).toString("yyyy-MM-dd"))
                        yester_shift.put("line_idx", line_idx)
                        yester_shift.put("line_name", "Manual")
                    }
                    list2.put(yester_shift)
                }
                list1 = OEEUtil.handleWorkData(list1)
                list2 = OEEUtil.handleWorkData(list2)

                AppGlobal.instance.set_today_work_time(list1)
                AppGlobal.instance.set_prev_work_time(list2)

                // Log 확인
//                OEEUtil.LogWrite(list1.toString(), "Today Shift info")
//                OEEUtil.LogWrite(list2.toString(), "Yester Shift info")

                btn_work_info?.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_work_info?.setBackgroundColor(Color.parseColor("#3f8cd6"))

                compute_work_shift()

            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    /*
     *  Shift 전환을 위한 변수를 미리 세팅한다.
     *  현재 Shift의 idx, 종료시간과 다음 Shift의 시작 시간을 미리 구해놓는다. (매초마다 검사를 하기 때문에 최대한 작업을 단순화하기 위함)
     */
    private var is_loop :Boolean = false        // 처리 중일때 중복 처리를 하지 않기 위함
    var _current_shift_etime_millis = 0L        // 현재 Shift 의 종료 시간 저장
                                                // 종료 시간이 있으면 아래 시작 시간은 구할 필요가 없음 (종료되면 로직이 다시 실행되기 때문)
    var _next_shift_stime_millis = 0L           // 다음 Shift 의 시작 시간 저장 (위의 종료 시간이 0L 일때만 세팅된다.)
    var _last_working = false

    // fetchWorkData() 에서 호출되므로 10분마다 실행되지만,
    // 예외적으로 시프트가 끝나거나 새로운 시프트 시작될때도 호출된다.
    private fun compute_work_shift() {
        if (is_loop) return
        is_loop = true

        val list = AppGlobal.instance.get_current_work_time()

        // 현재 쉬프트의 종료 시간을 구한다. 자동 종료를 위해
        // 종료 시간이 있으면 다음 시작 시간을 구할 필요없음. 종료되면 이 로직이 실행되므로 자동으로 구해지기 때문..

        if (list.length() > 0) {

            // DB에 Shift & Target 정보를 저장한다.
            // Production report를 위해서 그날의 Target 정보를 만들어 놓는다.
            // CountViewFragment 에서 같은 작업을 하고 있지만 거기서는 현 Shift만 검사하고,
            // 여기서는 모든 Shift 의 빈 DB도 만들어 놓는다. 리포트 페이지에 빈 테이블도 보여야 하기 때문.
            for (i in 0..(list.length() - 1)) {
                val work_item = list.getJSONObject(i)
                val row = _target_db.get(work_item["date"].toString(), work_item["shift_idx"].toString())
                if (row == null) {
                    _target_db.add(work_item["date"].toString(), work_item["shift_idx"].toString(), work_item["shift_name"].toString(), 0f,
                        work_item["work_stime"].toString(), work_item["work_etime"].toString())
                }
            }

            val now_millis = DateTime().millis

            for (i in 0..(list.length() - 1)) {
                val item = list.getJSONObject(i)
                var shift_stime = OEEUtil.parseDateTime(item["work_stime"].toString()).millis
                var shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString()).millis

                if (shift_stime <= now_millis && now_millis < shift_etime) {
                    // 타이틀 변경
                    tv_title?.setText(item["shift_name"].toString() + "   " +
                            OEEUtil.parseDateTime(item["work_stime"].toString()).toString("HH:mm") + " - " +
                            OEEUtil.parseDateTime(item["work_etime"].toString()).toString("HH:mm"))

                    // 이전 Shift와 현재 Shift가 다르다면 Actual 초기화
                    val shift_info = item["date"].toString() + item["shift_idx"].toString()
                    if (shift_info != AppGlobal.instance.get_last_shift_info()) {
//                        AppGlobal.instance.set_current_shift_actual_cnt(0)      // 토탈 Actual 초기화
                        AppGlobal.instance.set_last_shift_info(shift_info)      // 현재 Shift 정보 저장
                    }

                    _current_shift_etime_millis = shift_etime
                    _next_shift_stime_millis = 0L

                    // 마지막 레코드라면 그날의 마지막 작업이므로 마지막을 위한 플래그 세팅
                    if (i == list.length()-1) {
                        _last_working = true
                    } else {
                        _last_working = false
                    }

                    Log.e("compute_work_shift", "shift_idx=" + item["shift_idx"].toString() + ", shift_name=" + item["shift_name"].toString() +
                            ", work time=" + item["work_stime"].toString() + "~" + item["work_etime"].toString() + " ===> Current shift end millis = " + _current_shift_etime_millis)

                    val br_intent = Intent("need.refresh")
                    this.sendBroadcast(br_intent)

                    is_loop = false
                    return
                }
            }
        }

        // 루프를 빠져나왔다는 것은 현재 작업중인 Shift 가 없다는 의미이므로 다음 Shift 의 시작 시간을 구한다.
        // 만약 해당일의 모든 Shift 가 끝났으며 다음 시작 시간은 0L 로 저장한다.
        // 다음날 Shift 시작 정보는 10분마다 로딩하므로 구할 필요없음

        tv_title.setText("No shift")

//        AppGlobal.instance.set_current_shift_actual_cnt(0)      // 토탈 Actual 초기화

//        AppGlobal.instance.set_current_shift_idx("-1")
//        AppGlobal.instance.set_current_shift_name("No-shift")

        _current_shift_etime_millis = 0L
        _next_shift_stime_millis = 0L

        // 종료 시간이 없다는 것은 작업 시간이 아니라는 의미이므로 다음 시작 시간을 구한다.
        if (list.length() > 0) {
            val now_millis = DateTime().millis
            for (i in 0..(list.length() - 1)) {
                val item = list.getJSONObject(i)
                var shift_stime = OEEUtil.parseDateTime(item["work_stime"].toString()).millis

                if (shift_stime > now_millis) {
                    _next_shift_stime_millis = shift_stime
                    break
                }
            }
        }

        Log.e("compute_work_shift", "shift_idx=-1, shift_name=No-shift ===> Next shift start millis = " + _next_shift_stime_millis)

        val br_intent = Intent("need.refresh")
        this.sendBroadcast(br_intent)

        is_loop = false
    }

    private fun sendOeeGraphData() {
        if (AppGlobal.instance.get_server_ip().trim() == "") return
        var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
        if (item == null) return

        val uri = "/Sgraph.php"
        var params = listOf(
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
            "category" to "P",
            "availability" to _availability_rate.toString(),
            "performance" to _performance_rate.toString(),
            "quality" to _quality_rate.toString(),
            "oee" to _oee_rate.toString()
        )
        request(this, uri, true, false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    /*
     *  칼라코드 가져오기
     *  color_name = 'yellow'
     *  color_code = 'FFBC34'
     */
    private fun fetchColorData() {
        val uri = "/getlist1.php"
        val params = listOf("code" to "color")
        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                AppGlobal.instance.set_color_code(result.getJSONArray("item"))
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    /*
     *  downtime check time
     *  select_yn = 'Y' 것만 가져온다.
     *  etc_yn = 'Y' 이면 second 값, 'N' 이면 name 값이 리턴된다. (1800)
     */
    private fun fetchDownTimeType() {
        val uri = "/getlist1.php"
        val params = listOf("code" to "check_time")
        request(this, uri, false, params, { result ->
            Log.e("value", "check_time = " + result.toString())
            val code = result.getString("code")
            if (code == "00") {
                var value = result.getString("value")
                if (value == "Cycle Time") {
                    AppGlobal.instance.set_downtime_type("Cycle Time")
                    AppGlobal.instance.set_downtime_sec_for_stitch("0")      // count 가 들어왔을때 계산되는 초
                } else {
                    AppGlobal.instance.set_downtime_type("")
                    AppGlobal.instance.set_downtime_sec_for_stitch(value)   // count 가 들어왔을때 계산되는 초
                }
/*  바뀐 기준 : stitch 일때 디자인의 초, count 일때 서버에서 받아온 check time 으로 검사함.
                if (value == "Cycle Time") {
                    AppGlobal.instance.set_downtime_type("Cycle Time")
                    val sec = AppGlobal.instance.get_cycle_time()
                    value = if (sec<=0) "600" else sec.toString()

                    AppGlobal.instance.set_downtime_sec_for_stitch("")      // count 가 들어왔을때 계산되는 초
                } else {
                    AppGlobal.instance.set_downtime_type("")

                    AppGlobal.instance.set_downtime_sec_for_stitch(value)   // count 가 들어왔을때 계산되는 초
                }
                AppGlobal.instance.set_downtime_sec(value)
 */
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    fun fetchDowntimeList() {
        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "down_time",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx())

        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                val list = result.getJSONArray("item")
                AppGlobal.instance.set_downtime_list(list)
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    fun fetchPushData() {
        val uri = "/getlist1.php"
        val params = listOf("code" to "text")
        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if(code == "00"){
                val list = result.getJSONArray("item")
                AppGlobal.instance.set_push_data(list)
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    // 서버에서 설정한 현시프트의 타겟 가져오기
    fun fetchServerTargetData() {
        if (AppGlobal.instance.get_server_ip().trim() == "") return

        val dt = DateTime()
        val shift_idx = AppGlobal.instance.get_current_shift_idx()
        val uri = "/getlist1.php"
        val params = listOf(
            "code" to "target",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "date" to dt.toString("yyyy-MM-dd"),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to shift_idx)

        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                val daytargetsum = result.getString("daytargetsum").toString()
                AppGlobal.instance.set_target_server_shift(shift_idx, daytargetsum)
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    // getlist1.php 에서 hwi/query.php 바뀜 요청에 의해
    // 2021-08-16
    private fun fetchDesignData() {
        //val uri = "/getlist1.php"
        val uri = "/hwi/query.php"
        val params = listOf(
            //"code" to "design",
            "code" to "get_designP",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx())

        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if (code == "00"){
                val list = result.getJSONArray("item")
                AppGlobal.instance.set_design_info(list)
                btn_component_info?.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_component_info?.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOrange))
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    // Parts cycle time 이라는 기능
    // 남은 시간이 10시간 미만인 콤포넌트가 있으면 푸시 발송. 갯수만큼
    private fun fetchComponentData() {
        val uri = "/getlist1.php"
        val params = listOf(
            "code" to "component",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx())

        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                val list = result.getJSONArray("item")
                AppGlobal.instance.set_comopnent_data(list)

                var notified_component_set = UtilLocalStorage.getStringSet(this, "notified_component_set")

                var cnt = 0
                var is_popup = false
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    val idx = item.getString("idx").toString()
                    val total_cycle_time = item.getString("total_cycle_time").toInt()
                    val now_cycle_time = item.getString("now_cycle_time").toInt()
                    val rt = total_cycle_time - now_cycle_time
                    if (rt <= 10) {
                        if (!notified_component_set.contains(idx)) {
                            notified_component_set = notified_component_set.plus(idx)
                            is_popup = true
                            sendPush("SYS: PCT", item.getString("name").toString())
                        }
                        cnt++
                    }
                }
                if (cnt == 0) {
                    tv_component_count.visibility = View.GONE
                } else {
                    tv_component_count.visibility = View.VISIBLE
                    tv_component_count.setText(""+cnt)
                }
                if (is_popup) {
                    UtilLocalStorage.setStringSet(this, "notified_component_set", notified_component_set)
                    startComponentActivity()
                }
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun checkServerTime() {
        if (AppGlobal.instance.get_server_ip().trim() == "") return

        val uri = "/getlist1.php"
        val params = listOf("code" to "current_time")
        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                val server_time = result.getString("curdatetime")?: ""
                if (server_time != "") {
                    val server_dt = OEEUtil.parseDateTime(server_time).millis
                    val device_dt = DateTime.now().millis
                    if (server_dt > device_dt) {
                        _sync_time =
                            if ((server_dt - device_dt) > 10000) {
                                ToastOut(this, R.string.msg_time_mismatch, true)
                                false
                            } else {
                                true
                            }
                    } else {
                        _sync_time =
                            if ((device_dt - server_dt) > 10000) {
                                ToastOut(this, R.string.msg_time_mismatch, true)
                                false
                            } else {
                                true
                            }
                    }
                }
            }
        })
    }

    private fun sendPing() {
        if (AppGlobal.instance.get_server_ip() == "") return

        val currentTimeMillisStart = System.currentTimeMillis()
        val uri = "/ping.php"
        request(this, uri, false, false, null, { result ->
            val currentTimeMillisEnd = System.currentTimeMillis()
            val millis = currentTimeMillisEnd - currentTimeMillisStart
            val code = result.getString("code")
            if (code == "00") {
                btn_server_state.isSelected = true
                AppGlobal.instance.set_server_connect(true)
                tv_ms.text = "" + millis + " ms"

                val br_intent = Intent("need.refresh.server.state")
                br_intent.putExtra("state", "Y")
                this.sendBroadcast(br_intent)
            } else {
                ToastOut(this, result.getString("msg"))
            }
        }, {
            btn_server_state.isSelected = false
            val br_intent = Intent("need.refresh.server.state")
            br_intent.putExtra("state", "N")
            this.sendBroadcast(br_intent)
        })
    }

    fun endTodayWork() {
//        AppGlobal.instance.set_work_idx("")
//        AppGlobal.instance.set_worker_no("")
//        AppGlobal.instance.set_worker_name("")
//        AppGlobal.instance.set_compo_size("")
//        AppGlobal.instance.set_compo_target(0)

        tv_report_count?.text = "0"                              // 좌측 Report 버튼의 Actual 값도 0으로 초기화
        tv_defective_count?.text = "0"                           // 카운트 뷰의 Defective 값도 0으로 초기화

        pieces_qty = 0
        pairs_qty = 0f

        _prepare_time = 0L

        AppGlobal.instance.set_last_count_received("")            // 다운타임 검사용 변수 초기화
        AppGlobal.instance.set_downtime_idx("")

        AppGlobal.instance.set_design_info_idx("")
        AppGlobal.instance.set_model("")
        AppGlobal.instance.set_article("")
        AppGlobal.instance.set_material_way("")
        AppGlobal.instance.set_component("")
        AppGlobal.instance.set_cycle_time(0)
        AppGlobal.instance.reset_product_idx()

//        AppGlobal.instance.set_current_shift_actual_cnt(0)

        val donw_db = DBHelperForDownTime(this)
        donw_db.delete()

        val design_db = DBHelperForDesign(this)
        design_db.delete()

        // 기존 다운타임 화면이 열려있으면 닫는다.
        val br_intent = Intent("start.downtime")
        this.sendBroadcast(br_intent)

        ToastOut(this, R.string.msg_exit_automatically)
    }

    /*
     *  Shift 전환을 위한 실시간 검사. 매초마다 실행됨
     *  현재 작업중인 Shift 가 있으면 종료되는 시간을 검사해서 종료 시간이 되었다면 다음 쉬프트를 계산한다. (_current_shift_etime_millis)
     *  현재 작업중인 Shift 가 없으면 일하는 시간이 아니므로 다음 시작 시간을 검사하고, 시작 시간이라면 Shift의 종료시간을 계산한다. (_next_shift_stime_millis)
     */
    fun checkCurrentShiftEndTime() {
        // 현재 Shift 끝남
        if (_current_shift_etime_millis != 0L) {
            if (_current_shift_etime_millis <= DateTime().millis) {
                Log.e("checkCurrentShiftEnd", "end time . finish shift work =============================> need reload")
//                // 마지막 작업이 끝났으면 완전 초기화
//                if (_last_working == true) {
//                    endTodayWork()
//                    _last_working = false
//                }
                // shift만 바뀌어도 모두 삭제

                // 시프트가 끝났을때 현재까지의 카운트 데이터를 먼저 전송하고 종료 작업을 한다.
                // 0 값이 간다고 하여 일단 막아놓음.
//                val cnt = AppGlobal.instance.get_current_shift_actual_cnt()
//                sendCountData("0", 0F, cnt, "0")  // 서버에 카운트 정보 전송

                endTodayWork()
                compute_work_shift()
                if (AppGlobal.instance.get_target_by_group()) fetchServerTargetData()     // 특정 업체를 위한 서버 타겟값 가져오기
            }

        } else {
            // 다음 Shift 시작됨
            if (_next_shift_stime_millis != 0L) {
                if (_next_shift_stime_millis <= DateTime().millis) {
                    Log.e("checkCurrentShiftEnd", "start time . start shift work =============================> need reload")
                    compute_work_shift()
                    if (AppGlobal.instance.get_target_by_group()) fetchServerTargetData()     // 특정 업체를 위한 서버 타겟값 가져오기
                }
            }
        }
    }

    // 이미지 파일을 지정 시간마다 토글
    private fun checkToggle() {
        if (vp_fragments.currentItem == 1) {
            if (workSheetToggle) {
                val time = AppGlobal.instance.get_worksheet_display_time()
                if ((DateTime().millis / 1000) % time == 0L) {
//                (activity as MainActivity).countViewType = 3 - (activity as MainActivity).countViewType
                    if (workSheetShow) {
                        workSheetShow = false
                        ll_worksheet_view.visibility = View.GONE
                    } else {
                        workSheetShow = true
                        ll_worksheet_view.visibility = View.VISIBLE
                    }
                }
            }
        } else {
            ll_worksheet_view.visibility = View.GONE
        }
    }

    /////// 쓰레드
    private val _downtime_timer = Timer()
    private val _timer_task2 = Timer()          // 작업시간, 다운타입, 칼라 Data 가져오기 (workdata, designdata, downtimetype, color)
    private val _timer_task3 = Timer()          // 30초마다. 그래프 그리기 위한 태스크
    private val _timer_task4 = Timer()          // 1시간마다. 서버로 타겟값 전송 => 타겟값에 변화가 있을때마다 전송으로 변경됨. (CountViewFragment 에서 처리함)

    private fun start_timer() {

        // 매초
        val downtime_task = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    checkCurrentShiftEndTime()
                    checkDownTime()
                    checkToggle()   // 이미지 선택한 파일 토글하기
//                    checkExit()
                }
            }
        }
        _downtime_timer.schedule(downtime_task, 500, 1000)

        // 10분마다
        val task2 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    fetchRequiredData()
                }
            }
        }
        _timer_task2.schedule(task2, 600000, 600000)

        // 30초마다
        val task3 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    sendPing()
                    RemoveDownTimeData()    // Shift가 지난 다운타임 데이터를 삭제한다.
                    if (!_sync_time) checkServerTime()  // 서버와 시간이 맞는지 검사
                }
            }
        }
        _timer_task3.schedule(task3, 3000, 30000)

        // 1시간마다
        val task4 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    sendOeeGraphData()
                    fetchRequiredDataHour()
                    if (_sync_time) checkServerTime()   // 정상적인 상태일때는 시간 검사를 1시간 마다
                }
            }
        }
        _timer_task4.schedule(task4, 1200000, 3600000)
    }
    private fun cancel_timer () {
        _downtime_timer.cancel()
        _timer_task2.cancel()
        _timer_task3.cancel()
        _timer_task4.cancel()
    }

    ////////// USB
    private val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbService.ACTION_USB_PERMISSION_GRANTED // USB PERMISSION GRANTED
                -> ToastOut(context, "USB Ready")
                UsbService.ACTION_USB_PERMISSION_NOT_GRANTED // USB PERMISSION NOT GRANTED
                -> ToastOut(context, "USB Permission not granted")
                UsbService.ACTION_NO_USB // NO USB CONNECTED
                -> ToastOut(context, "No USB connected")
                UsbService.ACTION_USB_DISCONNECTED // USB DISCONNECTED
                -> ToastOut(context, "USB disconnected")
                UsbService.ACTION_USB_NOT_SUPPORTED // USB NOT SUPPORTED
                -> ToastOut(context, "USB device not supported")
            }
            when (intent.action) {
                UsbService.ACTION_USB_PERMISSION_GRANTED // USB PERMISSION GRANTED
                -> {
                    btn_usb_state.isSelected = true
                    AppGlobal.instance.set_usb_connect(true)
                    tv_usb?.setTextColor(Color.parseColor("#f8ad13"))
                }
                else -> {
                    btn_usb_state.isSelected = false
                    AppGlobal.instance.set_usb_connect(false)
                    tv_usb?.setTextColor(Color.parseColor("#EEEEEE"))
                }
            }
        }
    }
    
    private var usbService: UsbService? = null
    private var mHandler: MyHandler? = null

    private val usbConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            usbService = (arg1 as UsbService.UsbBinder).service
            usbService!!.setHandler(mHandler)
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            usbService = null
        }
    }
    private fun startService(service: Class<*>, serviceConnection: ServiceConnection, extras: Bundle?) {
        if (!UsbService.SERVICE_CONNECTED) {
            val startService = Intent(this, service)
            if (extras != null && !extras.isEmpty) {
                val keys = extras.keySet()
                for (key in keys) {
                    val extra = extras.getString(key)
                    startService.putExtra(key, extra)
                }
            }
            startService(startService)
        }
        val bindingIntent = Intent(this, service)
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    private class MyHandler(activity: MainActivity) : Handler() {
        private val mActivity: WeakReference<MainActivity>
        init {
            mActivity = WeakReference(activity)
        }
        override fun handleMessage(msg: Message) {
//            Log.e("USB Handler", "start -> " + msg.obj.toString())
            when (msg.what) {
                UsbService.MESSAGE_FROM_SERIAL_PORT -> {
                    val data = msg.obj as String
                    mActivity.get()?.handleData(data)
                }
                UsbService.CTS_CHANGE -> Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show()
                UsbService.DSR_CHANGE -> Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show()
            }
        }
    }

    /*
        {"cmd":"barcode", "value":"1003"}
        {"cmd":"stitch", "value":"start"}
        {"cmd":"count", "value":"", "runtime":""}
        {"cmd":"T", "value":"", "runtime":""}
    */
    private var recvBuffer = ""
    fun handleData(data:String) {
        if (data.indexOf("{") >= 0)  recvBuffer = ""

        recvBuffer += data

        val pos_end = recvBuffer.indexOf("}")
        if (pos_end < 0) return

        if (isJSONValid(recvBuffer)) {
            try {
                val parser = JsonParser()
                val element = parser.parse(recvBuffer)
                val cmd = element?.asJsonObject?.get("cmd")?.asString ?: ""
                val value = element.asJsonObject.get("value")
                val runtime = element?.asJsonObject?.get("runtime")?.asString ?: ""

                ToastOut(this, element.toString())
                Log.e("USB handle", "usb = " + recvBuffer)

                saveRowData(cmd, value, runtime)

            } catch(e: JsonSyntaxException) {
//                LogWrite(e.toString(), "USB Input Error")
            }
        } else {
            ToastOut(this, "usb parsing error! = " + recvBuffer)
            Log.e("USB handel", "usb parsing error! = " + recvBuffer)
        }
    }
    private fun isJSONValid(test: String): Boolean {
        try {
            JSONObject(test)
        } catch (ex: JSONException) {
            try {
                JSONArray(test)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }

    var pieces_qty = 0
    var pairs_qty = 0f
    var runtime_total = 0

    private fun saveRowData(cmd: String, value: JsonElement, runtime: String) {

        if (AppGlobal.instance.get_sound_at_count()) AppGlobal.instance.playSound(this)

        if (cmd=="barcode") {
            val arr = value.asJsonArray
            val didx = if (arr.size() > 1) value.asJsonArray[0].asString else arr[0].asString

            val list = AppGlobal.instance.get_design_info()

            for (i in 0..(list.length() - 1)) {
                val item = list.getJSONObject(i)
                val idx = item.getString("idx")
                if (idx == didx) {
                    val cycle_time = item.getString("ct").toInt()
                    val model = item.getString("model").toString()
                    val article = item.getString("article").toString()
                    val stitch = item.getString("stitch").toString()
                    val material_way = item.getString("material_way").toString()
                    val component = item.getString("component").toString()

                    startNewProduct(didx, cycle_time, model, article, stitch, material_way, component)
                    return
                }
            }
            Toast.makeText(this, getString(R.string.msg_no_design), Toast.LENGTH_SHORT).show()

        } else if (cmd == "stitch") {

            sendEndDownTimeForce()      // 처리안된 Downtime 강제 완료
            AppGlobal.instance.set_last_count_received()    // Set now time

            // 다운타임 초기화 및 첫 카운트 들어오기 기다림
//            AppGlobal.instance.set_first_count(false)
            AppGlobal.instance.set_first_count(true)        // stitch 도 다운타임 검사하게 바뀜. 2021-01-20
            AppGlobal.instance.set_stitch_type(true)        // 다운타임을 stitch 모드로 검사 (=서버에서 전달된 시간으로 검사)
                                                            // 바뀜. stitch 일때 선택한 디자인의 cycle time 기준

        } else if (cmd == "T" || cmd == "count") {

            AppGlobal.instance.set_first_count(true)
            AppGlobal.instance.set_stitch_type(false)       // 다운타임을 원래 검사하던 방식으로 검사
                                                            // 바뀜. count 일때 서버에서 받아온 check time 기준

            // Operator 선택 확인
            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                ToastOut(this, R.string.msg_no_operator, true)
                return
            }

            val work_idx = AppGlobal.instance.get_product_idx()
            val cur_shift: JSONObject ?= AppGlobal.instance.get_current_shift_time()

            // 선택한 Design이 있는지 확인
            if (work_idx == "") {
                ToastOut(this, R.string.msg_select_design, true)
                return
            }
            // 작업 시간인지 확인
            if (cur_shift == null) {
                ToastOut(this, R.string.msg_not_start_work, true)
                return
            }
            // 휴식시간인지 확인. 휴식 시간이면 Actual Count 가능한지 체크
            if (!AppGlobal.instance.get_planned_count_process()) {
                // 설정되어 있는 휴식 시간
                val planned1_stime = OEEUtil.parseDateTime(cur_shift["planned1_stime_dt"].toString())
                val planned1_etime = OEEUtil.parseDateTime(cur_shift["planned1_etime_dt"].toString())
                val planned2_stime = OEEUtil.parseDateTime(cur_shift["planned2_stime_dt"].toString())
                val planned2_etime = OEEUtil.parseDateTime(cur_shift["planned2_etime_dt"].toString())
                val now_millis = DateTime().millis

                // 워크 타임안에 있으면서 휴식 시간 안에 있다면,
                if ((planned1_stime.millis < now_millis && now_millis < planned1_etime.millis) ||
                    (planned2_stime.millis < now_millis && now_millis < planned2_etime.millis)) {
                    ToastOut(this, R.string.msg_cannot_work_planned_time, true)
                    return
                }
            }

            // Pieces, Pairs값
            val pieces_value = AppGlobal.instance.get_pieces_value()
            val pairs_value = AppGlobal.instance.get_pairs_value()

            if (pieces_value == 0 || pairs_value == 0f) {
                ToastOut(this, R.string.msg_layer_not_selected, true)
                return
            }

            val shift_idx = cur_shift["shift_idx"]      // 현재 작업중인 Shift
            var inc_count = 0f

            val target_type = AppGlobal.instance.get_target_type()          // setting menu 메뉴에서 선택한 타입

            if (target_type.substring(0, 6) == "cycle_") {
                inc_count = pairs_value

            } else {
                val max_pieces = AppGlobal.instance.get_pieces_value()
//                val max_pairs = AppGlobal.instance.get_pairs_value()

                pieces_qty++

                if (pieces_qty >= max_pieces) {

                    pieces_qty = pieces_qty - max_pieces
                    inc_count = pairs_value
//                    pairs_qty += 1f
//                    if (pairs_qty > max_pairs) {
//                        pairs_qty = 0f
//                        inc_count = 1f
//                    } else {
//                        return
//                    }
                }
            }

            if (runtime != "") runtime_total += runtime.toInt()

            if (inc_count <= 0f) return

            val cnt = AppGlobal.instance.get_current_shift_actual_cnt() + inc_count
            tv_report_count?.setText(cnt.toString())

            // Downtime 초기화
            sendEndDownTimeForce()                          // 처리안된 Downtime 강제 완료

            // 다운타임 체크시간
            AppGlobal.instance.set_last_count_received()    // Set now time


            // DB에 Actual 저장
            val design_db = DBHelperForDesign(this)
            val row = design_db.get(work_idx)
            if (row != null) {
                val actual = row!!["actual"].toString().toFloat() + inc_count
                val actual_no = row!!["actual_no"].toString().toInt() + 1
                design_db.updateWorkActual(work_idx, actual, actual_no)
            }

            // 서버 호출 (장치에서 들어온값, 증가치, Actual total, runtime)
            sendCountData(value.toString(), inc_count, cnt, runtime_total.toString())  // 서버에 카운트 정보 전송
            runtime_total = 0

            // Production Report를 위한 DB저장
            val date = cur_shift["date"].toString()
            val houly = DateTime().toString("HH")
            val rep = _report_db.get(date, houly, shift_idx.toString())
            if (rep == null) {
                _report_db.add(date, houly, shift_idx.toString(), inc_count)
            } else {
                val idx = rep!!["idx"].toString()
                val actual = rep!!["actual"].toString().toFloat() + inc_count
                _report_db.updateActual(idx, actual)
            }
        }
    }

    // 장치에서 들어온값, 증가치, Actual total, runtime
    private fun sendCountData(count:String, inc_count:Float, sum_count:Float, runtime: String) {
        if (AppGlobal.instance.get_server_ip()=="") return

        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") return

        val design_db = DBHelperForDesign(this)
        val row = design_db.get(work_idx)
//        val actual = row!!["actual"].toString().toInt()

        var seq = 0
        var now_target = 0F
        var now_actual = 0F

        if (row != null) {
            seq = row["seq"].toString().toInt()
            now_target = row["target"].toString().toFloat()  // 현디자인의 타겟
            now_actual = row["actual"].toString().toFloat()  // 현디자인의 액추얼
        }

        // 현재시간
        val now_millis = DateTime.now().millis

        // 서버에서 새로운 데이터를 요청해서 생겨난 로직 (쓸데없이 시간 지연됨)
        // 2019-10-11
        var count_target = design_db.sum_target_count()            // 총 타겟
        val count_defective = design_db.sum_defective_count()      // 현재 디펙티브 값

        // Downtime
        var shift_total_time = 0
        var planned_time = 0
        var param_runtime = 0
        var work_time = 0

        val shift_time = AppGlobal.instance.get_current_shift_time()
        val shift_idx =
            if (shift_time != null) {
                // 시프트 시작/끝
                val shift_stime_millis = OEEUtil.parseDateTime(shift_time["work_stime"].toString()).millis
                val shift_etime_millis = OEEUtil.parseDateTime(shift_time["work_etime"].toString()).millis

                // 휴식시간
                val planned1_stime_millis = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString()).millis
                val planned1_etime_millis = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString()).millis
                val planned2_stime_millis = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString()).millis
                val planned2_etime_millis = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString()).millis

                val planned1_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
                val planned2_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned2_stime_millis, planned2_etime_millis)

                shift_total_time = ((shift_etime_millis-shift_stime_millis) / 1000 ).toInt()
                planned_time = (((planned1_etime_millis-planned1_stime_millis) + (planned2_etime_millis-planned2_stime_millis)) / 1000).toInt()
//                planned_time = planned1_time + planned2_time

                // 현재까지의 작업시간
                work_time = ((now_millis - shift_stime_millis) / 1000).toInt() - planned1_time - planned2_time

                // Downtime
                val down_db = DBHelperForDownTime(this)
                val down_time = down_db.sum_real_millis_count()
                val down_target = down_db.sum_target_count()

                if (down_target > 0f) count_target -= down_target

                param_runtime = work_time - down_time

                shift_time["shift_idx"].toString()
                // ctO 구하기 (현시점까지 작업시간 - 다운타임 시간)의 타겟
            } else {
                "0"
            }

        // 서버에서 새로운 데이터를 요청해서 생겨난 로직 끝.
        // 2019-10-11

        // Cutting 과는 다르게 콤포넌트가 필수 선택사항이 아니므로
        // 선택되었을 경우에만 seq 값을 구하고 아니면, 디폴트 1을 전송한다.

        val prepare_time = if (_prepare_time == 0L) 0L else (now_millis - _prepare_time)
        _prepare_time = now_millis

        // 신서버용
        // runtime : downtime 을 뺀 근무시간
        // actualO : 현 시프트의 총 Target
        // ct0 : 퍼포먼스 계산할 때 타겟 값 (현시점까지 작업시간 - 다운타임 시간)의 타겟
        // "actualO" to count_target.toString(),
        val uri = "/hwi/query.php"
        val params = listOf(
            "code" to "send_count",
            "mac" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "count_sum" to sum_count,
            "shift_idx" to  shift_idx,
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "worktime" to work_time,
            "runtime" to param_runtime.toString(),
            "actualO" to sum_count.toString(),
            "ctO" to count_target.toString(),
            "defective" to count_defective.toString(),
            "worker" to AppGlobal.instance.get_worker_no(),
            "available_time" to (shift_total_time/60).toString(),
            "planned_stop_time" to (planned_time/60).toString(),
            "target" to now_target.toString(),
            "actual" to now_actual.toString())

        ToastOut(this, design_db.sum_target_count().toString() + " " +sum_count.toString(), true)

//        val uri = "/Scount.php"
//        val params = listOf(
//            "mac_addr" to AppGlobal.instance.getMACAddress(),
//            "didx" to AppGlobal.instance.get_design_info_idx(),
//            "count" to inc_count.toString(),
//            "total_count" to sum_count,
//            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
//            "factory_idx" to AppGlobal.instance.get_zone_idx(),
//            "line_idx" to AppGlobal.instance.get_line_idx(),
//            "shift_idx" to  shift_idx,
//            "seq" to seq,
//            "runtime" to param_rumtime.toString(),
//            "stitching_count" to AppGlobal.instance.get_stitch(),
//            "curing_sec" to runtime,
//            "prepare_time" to prepare_time.toString(),
//            "actualO" to sum_count.toString(),
//            "ctO" to count_target.toString(),
//            "defective" to count_defective.toString(),
//            "worker" to AppGlobal.instance.get_worker_no())

        request(this, uri, true,false, params, { result ->
            val code = result.getString("code")
            if(code != "00") {
                ToastOut(this, result.getString("msg"), true)
            }
        })

        // 베트남 특별한 경우 때문에 생긴 기능이지만 현재는 무조건 다 보낸다.
        if (runtime != "") {
//            ToastOut(this, R.string.msg_runtime_not_enterd)
            return
        } else {
            val sensing_id = DateTime.now().millis
            val uri2 = "/Hcount.php"
            val params2 = listOf(
                "sensing_id" to sensing_id,
                "machine_no" to AppGlobal.instance.get_mc_no(),
                "stitching_count" to AppGlobal.instance.get_stitch(),
                "curing_sec" to runtime,
                "prepare_time" to prepare_time.toString(),
                "piece_yn" to "1",
                "factory_cd" to AppGlobal.instance.get_factory_idx(),
                "line_cd" to AppGlobal.instance.get_line_idx(),
                "factory_nm" to AppGlobal.instance.get_factory(),
                "line_nm" to AppGlobal.instance.get_line()
            )
//            Toast.makeText(this, "sensing_id="+sensing_id+", curing_sec="+runtime, Toast.LENGTH_SHORT).show()

            request(this, uri2, true, false, params2, { result ->
                val code = result.getString("code")
                if (code != "00") {
                    ToastOut(this, result.getString("msg"), true)
                }
            })
        }
    }

    fun startNewProduct(didx:String, cycle_time:Int, model:String, article:String, stitch:String, material_way:String, component:String) {

        // 이전 작업과 동일한 디자인 번호이면 새작업이 아님
        val prev_didx = AppGlobal.instance.get_design_info_idx()
        val prev_work_idx = "" + AppGlobal.instance.get_product_idx()

        AppGlobal.instance.set_design_info_idx(didx)
        AppGlobal.instance.set_cycle_time(cycle_time)
        AppGlobal.instance.set_model(model)
        AppGlobal.instance.set_article(article)
        AppGlobal.instance.set_stitch(stitch)
        AppGlobal.instance.set_material_way(material_way)
        AppGlobal.instance.set_component(component)

        val pieces_info = AppGlobal.instance.get_pieces_text()
        val pairs_info = AppGlobal.instance.get_pairs_text()

        // 서버에서 받은 다운타임 타입이 초단위가 아니고 "Cycle Time" 이면 선택된 디자인의 Cycle Time 으로 세팅된다.
        val downtime_type = AppGlobal.instance.get_downtime_type()

        /* downtime_type에 상관없이 무조건 선택한 디자인의 시간으 세팅한다.
           바뀐 기준 : stitch 일때 디자인의 초, count 일때 서버에서 받아온 check time 으로 검사함.
        if (downtime_type=="Cycle Time") {
            AppGlobal.instance.set_downtime_sec(cycle_time.toString())
        }
         */
        AppGlobal.instance.set_downtime_sec(cycle_time.toString())

        val design_db = DBHelperForDesign(this)
        val item = design_db.get(prev_work_idx)

        val shift_time = AppGlobal.instance.get_current_shift_time()

        if (didx == prev_didx) {    // 같은 디자인이 선택된 경우
            if (item != null) {
                val shift_idx = shift_time?.getString("shift_idx") ?: ""
                val shift_name = shift_time?.getString("shift_name") ?: ""

                design_db.updateDesignInfo(prev_work_idx, shift_idx, shift_name, didx, cycle_time, pieces_info, pairs_info)

                recomputeDowntime(prev_work_idx)  // Downtime 재계산
            }
            return
        }

        // 다운타임 초기화 및 첫 카운트 들어오기 기다림
        AppGlobal.instance.set_first_count(false)
        AppGlobal.instance.set_last_count_received()    // Set now time


        // 이전 디자인의 Actual이 0이면 (작업이 하나도 없는 경우, 실수로 선택한 경우 등)
        // 해당 디자인을 지우고 시작 시간을 새 디자인의 시작 시간으로 업데이트한다.
        if (item != null) {
            val actual_cnt = item["actual"].toString().toFloat()
            if (actual_cnt == 0f) {
//                start_dt = item["start_dt"].toString()        // 시작 시간을 이전 디자인의 시작 시간으로 재설정
//                design_db.deleteWorkIdx(prev_work_idx)        // 이전 디자인 삭제

                // 이전 디자인을 삭제하지 않고 그위에 새 디자인 정보를 덮어쓴다.

                val shift_idx = item.get("shift_idx")?.toString() ?: shift_time?.getString("shift_idx") ?: ""
                val shift_name = item.get("shift_name")?.toString() ?: shift_time?.getString("shift_name") ?: ""

                design_db.updateDesignInfo(prev_work_idx, shift_idx, shift_name, didx, cycle_time, pieces_info, pairs_info)

                recomputeDowntime(prev_work_idx)  // Downtime 재계산

//                val down_db = DBHelperForDownTime(this)
//                val down_list = down_db.gets(prev_work_idx)
//
//                // From Server / From Device 에서 활용
//                val one_item_sec = AppGlobal.instance.get_current_maketime_per_piece()
//
//                for (i in 0..((down_list?.size ?: 1) - 1)) {
//                    val down_item = down_list?.get(i)
//                    val down_idx = down_item?.get("idx").toString()
//                    val down_real_millis = down_item?.get("real_millis").toString().toFloat()
//                    if (down_real_millis > 0) {
//                        if (target_type.substring(0, 6) == "cycle_") {
//                            val pieces = AppGlobal.instance.get_pieces_value()
//                            val pairs = AppGlobal.instance.get_pairs_value()
//                            val new_target = OEEUtil.computeTarget(down_real_millis, cycle_time, pieces, pairs)
//
//                            down_db.updateDidxTarget(down_idx, didx, new_target)
//
//                        } else {
//                            if (one_item_sec != 0F) {
//                                val new_target = down_real_millis / one_item_sec
//                                down_db.updateDidxTarget(down_idx, didx, new_target)
//                            } else {
//                                down_db.updateDidxTarget(down_idx, didx, 0f)
//                            }
//                        }
//                    } else {
//                        down_db.updateDidxTarget(down_idx, didx, 0f)
//                    }
//                }
                return
            }
        }

        if (prev_work_idx != "") design_db.updateWorkEnd(prev_work_idx)    // 이전 작업 완료 처리

        AppGlobal.instance.set_product_idx()

        val max_seq = design_db.max_seq()
        val seq = max_seq + 1

        var start_dt = DateTime().toString("yyyy-MM-dd HH:mm:ss")       // 새 디자인 시작시간

        // 처음 시작이면 Start 시간을 Shift 시작 시간으로 세팅
        if (seq == 1) {
            if (shift_time != null) {
                start_dt = shift_time["work_stime"].toString()
            }
        }

        val work_idx = "" + AppGlobal.instance.get_product_idx()

        val now = DateTime().millis
        var shift_idx = shift_time?.getString("shift_idx") ?: ""
        var shift_name = shift_time?.getString("shift_name") ?: ""

        if (shift_time == null) {
            // 현재 시프트가 없으므로 다가올 시프트 정보를 구한다.
            val list = AppGlobal.instance.get_current_work_time()
            for (i in 0..(list.length() - 1)) {
                val work_time = list.getJSONObject(i)
                val shift_stime = (OEEUtil.parseDateTime(work_time["work_stime"].toString())).millis

                if (now <= shift_stime) {
                    shift_idx = work_time?.getString("shift_idx") ?: ""
                    shift_name = work_time?.getString("shift_name") ?: ""
                    break
                }
            }
        }

        design_db.add(work_idx, start_dt, didx, shift_idx, shift_name, cycle_time, pieces_info, pairs_info,0f, 0f, 0, seq)


//        val dlist = design_db.gets()
//        for (i in 0..((dlist?.size ?: 1) - 1)) {
//            val item = dlist?.get(i)
//            Log.e("Design Data", "Design Data2 : "+item.toString())
//        }

//        val br_intent = Intent("need.refresh")
//        this.sendBroadcast(br_intent)
    }

    // Downtime 재계산
    fun recomputeDowntime(work_idx: String) {
        val didx = AppGlobal.instance.get_design_info_idx()
        val cycle_time = AppGlobal.instance.get_cycle_time()
        val target_type = AppGlobal.instance.get_target_type()

        if (work_idx == "" || didx == "" || cycle_time == 0) return

        // From Server / From Device 에서 활용
        val one_item_sec = AppGlobal.instance.get_current_maketime_per_piece()

        val down_db = DBHelperForDownTime(this)
        val down_list = down_db.gets(work_idx)

        for (i in 0..((down_list?.size ?: 1) - 1)) {
            val down_item = down_list?.get(i)
            val down_idx = down_item?.get("idx").toString()
            val down_real_millis = down_item?.get("real_millis").toString().toFloat()
            if (down_real_millis > 0) {
                if (target_type.substring(0, 6) == "cycle_") {
                    val pieces = AppGlobal.instance.get_pieces_value()
                    val pairs = AppGlobal.instance.get_pairs_value()
                    val new_target = OEEUtil.computeTarget(down_real_millis, cycle_time, pieces, pairs)

                    down_db.updateDidxTarget(down_idx, didx, new_target)

                } else {
                    if (one_item_sec != 0F) {
                        val new_target = down_real_millis / one_item_sec
                        down_db.updateDidxTarget(down_idx, didx, new_target)
                    } else {
                        down_db.updateDidxTarget(down_idx, didx, 0f)
                    }
                }
            } else {
                down_db.updateDidxTarget(down_idx, didx, 0f)
            }
        }
    }

    // downtime 발생시 푸시 발송
    fun sendPush(push_text: String, add_text: String = "", progress: Boolean=false) {
        val uri = "/pushcall.php"
        var params = listOf(
            "code" to "push_text_list",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
            "machine_no" to AppGlobal.instance.get_mc_no(),
            "mc_model" to AppGlobal.instance.get_mc_model(),
            "seq" to "0",
            "text" to push_text,
            "add_text" to add_text)

        request(this, uri, progress, params, { result ->
            var code = result.getString("code")
//            var msg = result.getString("msg")
            if(code != "00"){
//                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    var _is_call = false

    private fun sendStartDownTime(dt: DateTime) {
        if (_is_call) return
        _is_call = true

        if (AppGlobal.instance.get_server_ip() == "") {
            _is_call = false
            return
        }

        val down_db = DBHelperForDownTime(this)
        if (down_db.count_for_notcompleted() > 0) {
            _is_call = false
            return
        }

        val start_dt = dt.toString("yyyy-MM-dd HH:mm:ss")

        val cnt = down_db.count_start_dt(start_dt)      // 같은 시간대가 저장되어 있는지 검사
        if (cnt > 0) {
            _is_call = false
            return
        }

        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") {
            _is_call = false
            return
        }

        val work_info = AppGlobal.instance.get_current_shift_time()
        val shift_idx = work_info?.getString("shift_idx") ?: ""
        val shift_name = work_info?.getString("shift_name") ?: ""
        val didx = AppGlobal.instance.get_design_info_idx()

        val local_db_idx = down_db.add("", work_idx, didx, shift_idx, shift_name, start_dt)     // 새 다운타임 등록

        AppGlobal.instance.set_downtime_idx(local_db_idx.toString())

        startDowntimeInputActivity(local_db_idx.toString(), start_dt)
        sendPush("SYS: DOWNTIME")

        _is_call = false

//        val cnt_all = down_db.count_all()
//
//        val uri = "/downtimedata.php"
//        var params = listOf(
//            "code" to "start",
//            "mac_addr" to AppGlobal.instance.getMACAddress(),
//            "didx" to didx,
//            "sdate" to dt.toString("yyyy-MM-dd"),
//            "stime" to dt.toString("HH:mm:ss"),
//            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
//            "factory_idx" to AppGlobal.instance.get_zone_idx(),
//            "line_idx" to AppGlobal.instance.get_line_idx(),
//            "shift_idx" to  shift_idx,
//            "worker" to AppGlobal.instance.get_worker_no(),
//            "seq" to cnt_all)
//
//        request(this, uri, true,false, params, { result ->
//            var code = result.getString("code")
//            if (code == "00") {
//                val server_last_idx = result.getString("idx")
//                AppGlobal.instance.set_downtime_idx(server_last_idx)
//
//                down_db.updateLastId(local_db_idx.toString(), server_last_idx)
//
////                startDowntimeActivity()
//                startDowntimeInputActivity(server_last_idx, start_dt)
//                sendPush("SYS: DOWNTIME")
//            } else {
//                ToastOut(this, result.getString("msg"), true)
//            }
//            _is_call = false
//        },{
//            _is_call = false
//        })
    }

    private fun sendEndDownTimeForce() {

        // 기존 다운타임 화면이 열려있으면 일단 닫고 시작
        val br_intent = Intent("start.downtime")
        this.sendBroadcast(br_intent)

        if (AppGlobal.instance.get_server_ip() == "") return

        val db = DBHelperForDownTime(this)

        if (AppGlobal.instance.get_downtime_idx() == "") {
            // 걸려있는 다운타임이 없으나, 지난 다운타임중 처리 안된 다운타임이 있으면 삭제한다.
//            if (db.count_for_notcompleted() > 0) {
//                db.delete_for_notcompleted()
//            }
            return
        }

        val down_idx = AppGlobal.instance.get_downtime_idx()
//        if (down_idx == "") return
        val item = db.getLocalIdx(down_idx)
        if (item == null || item.size() == 0) return

        val now = DateTime()
        val now_millis = now.millis
        val start_dt2 = OEEUtil.parseDateTime(item["start_dt"].toString())
        val down_start_millis = start_dt2.millis

        var planned1_time = 0
        var planned2_time = 0

        val shift_time = AppGlobal.instance.get_current_shift_time()

        if (shift_time != null) {
            val planned1_stime_millis = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString()).millis
            val planned1_etime_millis = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString()).millis
            val planned2_stime_millis = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString()).millis
            val planned2_etime_millis = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString()).millis

            planned1_time = AppGlobal.instance.compute_time_millis(down_start_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
            planned2_time = AppGlobal.instance.compute_time_millis(down_start_millis, now_millis, planned2_stime_millis, planned2_etime_millis)
        }
        val down_time = ((now_millis - down_start_millis) / 1000).toInt()
        val real_down_time = down_time - planned1_time - planned2_time

        val target =
            if (real_down_time > 0) {
                val target_type = AppGlobal.instance.get_target_type()

                if (target_type.substring(0, 6) == "cycle_") {
                    val cycle_time = AppGlobal.instance.get_cycle_time()
                    val pieces = AppGlobal.instance.get_pieces_value()
                    val pairs = AppGlobal.instance.get_pairs_value()
                    OEEUtil.computeTarget(real_down_time.toFloat(), cycle_time, pieces, pairs)
                } else {
                    val one_item_sec = AppGlobal.instance.get_current_maketime_per_piece()
                    if (one_item_sec > 0F) {
                        real_down_time.toFloat() / one_item_sec
                    } else {
                        0f
                    }
                }
            } else {
                0f
            }

        db.updateEndLocalIdx(down_idx, "ignored", now.toString("yyyy-MM-dd HH:mm:ss"), down_time, real_down_time, target)

        AppGlobal.instance.set_downtime_idx("")
        AppGlobal.instance.set_last_count_received()    // 현재시간으로 리셋



//        val downtime = "5"
//        val uri = "/downtimedata.php"
//        val params = listOf(
//            "code" to "end",
//            "idx" to down_idx,
//            "downtime" to downtime,
//            "worker" to AppGlobal.instance.get_worker_no(),
//            "edate" to now.toString("yyyy-MM-dd"),
//            "etime" to now.toString("HH:mm:ss"))

        val cnt_all = db.count_all()
        val downtime = "9"

        val shift_idx = shift_time?.getString("shift_idx") ?: ""

        val uri = "/hwi/query.php"
        val params = listOf(
            "code" to "send_downtime",
            "mac" to AppGlobal.instance.getMACAddress(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "seq" to cnt_all,
            "downtime_idx" to downtime,
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "sdate" to start_dt2.toString("yyyy-MM-dd"),
            "stime" to start_dt2.toString("HH:mm:ss"),
            "edate" to now.toString("yyyy-MM-dd"),
            "etime" to now.toString("HH:mm:ss"),
            "worker" to AppGlobal.instance.get_worker_no(),
            "shift_idx" to shift_idx,
            "downtime_second" to real_down_time)

        request(this, uri, true,false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                // 성공
            } else if (code == "99") {
                // 오류라도 무시
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        }, {
            ToastOut(this, "Unknown error", true)
        })
    }

    // 작업 시간일때, 작업 시작 시간보다 작은 시간의 DownTime 을 삭제한다.(=지난 Shift의 DownTime)
    // Downtime 팝업창에서 처리한다. (10초마다 검사함)
    fun RemoveDownTimeData() {
        val item = AppGlobal.instance.get_current_shift_time()
        if (item != null) {
            val db = DBHelperForDownTime(this)
            db.deleteOldData(item["work_stime"].toString())
        }
    }

    // 지난 시프트의 디자인 정보가 있으면 삭제
    fun checkDesignData() {
        val db_design = DBHelperForDesign(this)
        val count = db_design.counts_for_ids()
        if (count == 0) {
            AppGlobal.instance.reset_product_idx()                  // work idx 초기화
            AppGlobal.instance.set_design_info_idx("")
            AppGlobal.instance.set_model("")
            AppGlobal.instance.set_article("")
            AppGlobal.instance.set_material_way("")
            AppGlobal.instance.set_component("")
            AppGlobal.instance.set_cycle_time(0)
        } else {
            val item = AppGlobal.instance.get_current_shift_time()
            if (item != null) {
                db_design.deleteLastDate(item["work_stime"].toString())
            } else {
                AppGlobal.instance.reset_product_idx()                  // work idx 초기화
                AppGlobal.instance.set_design_info_idx("")
                AppGlobal.instance.set_model("")
                AppGlobal.instance.set_article("")
                AppGlobal.instance.set_material_way("")
                AppGlobal.instance.set_component("")
                AppGlobal.instance.set_cycle_time(0)

                db_design.delete()
            }
        }
    }

    private var _is_down_loop: Boolean = false

    // 신버전
    private fun checkDownTime() {

        if (!AppGlobal.instance.get_first_count()) return       // 앱시작후 첫번째 count 들어와야 다운타임 계산을 시작한다. (stitch도 추가됨)

        if (_is_down_loop) return
        _is_down_loop = true

        val db = DBHelperForDownTime(this)
        if (db.count_for_notcompleted() > 0) {
            AppGlobal.instance.set_last_count_received()    // Set now time(reset time)
            _is_down_loop = false
            return
        }

        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") {
            _is_down_loop = false
            return
        }

        // get_reverse_downtime_check() 값이 false (기존 디폴트) 일때
        //   get_stitch_type() 값이
        //     = true 이면 "stitch" 가 들어온 상태이므로 디자인의 cycle time 으로 검사 (원래 검사 기준)
        //     = false 이면 "count" 가 들어온 상태이므로 서버에서 받아온 check time 으로 검사
        // get_reverse_downtime_check() 값이 true 이면 반대로
        //   get_stitch_type() 값이
        //     = true 이면 "count" 가 들어온 상태이므로 서버에서 받아온 check time 으로 검사
        //     = false 이면 "stitch" 가 들어온 상태이므로 디자인의 cycle time 으로 검사 (원래 검사 기준)
        //

        var downtime_time = ""

        if (AppGlobal.instance.get_reverse_downtime_check() == false) {     // 원래기준
            if (AppGlobal.instance.get_stitch_type() == false) {
                val stitch_downtime_time = AppGlobal.instance.get_downtime_sec_for_stitch()   // stitch 모드의 downtime 지정시간
                if (stitch_downtime_time != "" && stitch_downtime_time != "0") {
                    downtime_time = stitch_downtime_time
                } else {
                    ToastOut(this, R.string.warning_downtime_0)
                    _is_down_loop = false
                    return
                }
            } else {
                downtime_time = AppGlobal.instance.get_downtime_sec()   // downtime 지정시간 (Stitch 일때 선택한 디자인 초)
            }

        } else {    // 반대기준으로 검사

            if (AppGlobal.instance.get_stitch_type() == false) {
                downtime_time = AppGlobal.instance.get_downtime_sec()
            } else {
                val stitch_downtime_time = AppGlobal.instance.get_downtime_sec_for_stitch()
                if (stitch_downtime_time != "" && stitch_downtime_time != "0") {
                    downtime_time = stitch_downtime_time
                } else {
                    ToastOut(this, R.string.warning_downtime_0)
                    _is_down_loop = false
                    return
                }
            }
        }

//        downtime_time = AppGlobal.instance.get_downtime_sec()   // downtime 지정시간 (Stitch 일때 선택한 디자인 초)
//
//
//
//        if (AppGlobal.instance.get_stitch_type() == false) {
//            val stitch_downtime_time = AppGlobal.instance.get_downtime_sec_for_stitch()   // stitch 모드의 downtime 지정시간
//            if (stitch_downtime_time != "" && stitch_downtime_time != "0") {
//                downtime_time = stitch_downtime_time
//            } else {
//                ToastOut(this, R.string.warning_downtime_0)
//                _is_down_loop = false
//                return
//            }
//        }

        if (downtime_time == "") {
            ToastOut(this, R.string.msg_no_downtime)
            _is_down_loop = false
            return
        }

        val downtime_time_sec =
            if (AppGlobal.instance.get_downtime_type() == "Cycle Time") {
                val pieces_cnt = AppGlobal.instance.get_pieces_value()
                downtime_time.toInt() * pieces_cnt
            } else {
                downtime_time.toInt()
            }

        val item = AppGlobal.instance.get_current_shift_time()
        if (item == null) {
            _is_down_loop = false
            return
        }

        val now = DateTime()
        val now_millis = now.millis

        val work_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
        val work_stime_millis = work_stime.millis
        val work_etime_millis = OEEUtil.parseDateTime(item["work_etime"].toString()).millis
        val planned1_stime_millis = OEEUtil.parseDateTime(item["planned1_stime_dt"].toString()).millis
        val planned1_etime_millis = OEEUtil.parseDateTime(item["planned1_etime_dt"].toString()).millis
        val planned2_stime_millis = OEEUtil.parseDateTime(item["planned2_stime_dt"].toString()).millis
        val planned2_etime_millis = OEEUtil.parseDateTime(item["planned2_etime_dt"].toString()).millis

        var last_received_time = work_stime    // downtime 값이 "" 이면 처음이므로 Shift 시작 시간으로 저장

        var chk = AppGlobal.instance.get_last_count_received()
        if (chk != "") {
            if (OEEUtil.parseDateTime(chk).millis < work_stime_millis) {    // downtime 시작 시간이 Shift의 시작 시간보다 작다면 초기화
                chk = item["work_stime"].toString()
                AppGlobal.instance.set_last_count_received(chk)
            }
            last_received_time = OEEUtil.parseDateTime(chk)
        }

        val last_received_time_millis = last_received_time.millis

        // 워크 타임안에 있는 경우
        if (work_stime_millis < now_millis && work_etime_millis > now_millis) {

            // 휴식 시간이 아닐때
            if (!(planned1_stime_millis < now_millis && planned1_etime_millis > now_millis ) &&
                !(planned2_stime_millis < now_millis && planned2_etime_millis > now_millis ) && downtime_time_sec > 0) {

                // 다운타임 안의 휴식시간
                val d1 = AppGlobal.instance.compute_time_millis(last_received_time_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
                val d2 = AppGlobal.instance.compute_time_millis(last_received_time_millis, now_millis, planned2_stime_millis, planned2_etime_millis)

                val cur_down_time = ((now_millis - last_received_time_millis) / 1000) - d1 - d2     // 휴식시간을 뺀 실제 다운타임

                // Downtime 시간 체크값 로그
//                Log.d("Downtime check", "cur_down_time = " + cur_down_time + ", downtime_time_sec = " + downtime_time_sec + ", last_received_time = " + last_received_time)

                // 지정된 downtime이 지났으면 downtime을 발생시킨다.
                if (cur_down_time > downtime_time_sec) {
                    //sendStartDownTime(last_received_time)       // 지난 시간의 시작 시간
                    sendStartDownTime(now)
                }
            }

        } else {
            // 워크 타임이 아니면 downtime 시작 시간 초기화
            AppGlobal.instance.set_last_count_received("")
        }
        _is_down_loop = false
    }

    private fun startComponentActivity() {
        val br_intent = Intent("start.component")
        this.sendBroadcast(br_intent)
        startActivity(Intent(this, ComponentActivity::class.java))
    }

    private fun startDowntimeActivity() {
        val br_intent = Intent("start.downtime")
        this.sendBroadcast(br_intent)
        val intent = Intent(this, DownTimeActivity::class.java)
        startActivity(intent)
    }

    private fun startDowntimeInputActivity(local_idx: String = "", start_dt: String = "") {
        if (start_dt == "") return
        val intent = Intent(this, DownTimeInputActivity::class.java)
        intent.putExtra("idx", local_idx)
        intent.putExtra("start_dt", start_dt)
        startActivity(intent)
    }
//    private fun startDowntimeInputActivity(idx: String = "", start_dt: String = "") {
//        if (idx == "" || start_dt == "") return
//        val intent = Intent(this, DownTimeInputActivity::class.java)
//        intent.putExtra("idx", idx)
//        intent.putExtra("start_dt", start_dt)
//        startActivity(intent)
//    }


    fun changeFragment(pos:Int) {
        vp_fragments.setCurrentItem(pos, true)
    }

    private class TabAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        private val mFragments = ArrayList<Fragment>()
        private val mFragmentTitles = ArrayList<String>()

        override fun getCount(): Int { return mFragments.size }
        fun addFragment(fragment: Fragment, title: String) {
            mFragments.add(fragment)
            mFragmentTitles.add(title)
        }
        override fun getItem(position: Int): Fragment {
            return mFragments.get(position)
        }
        override fun getItemPosition(`object`: Any?): Int {
            return PagerAdapter.POSITION_NONE
        }
        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitles.get(position)
        }
    }
}