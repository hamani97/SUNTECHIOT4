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
import com.suntech.iot.pattern.db.*
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
    var workSheetToggle = false     // 워크시트를 토글할 것인지. 토글 시간(초)는 세팅 메뉴에서 설정
    var workSheetShow = false

    var _availability_rate = 0F
    var _quality_rate = 0F
    var _performance_rate = 0F
    var _oee_rate = 0F

    var _prepare_time = 0L            // 직전의 카운트와 새로 들어온 카운트 사이의 시간 (밀리세컨, 처음엔 0)

    //    val _stitch_db = DBHelperForCount(this)     // Count 정보
    val _target_db = DBHelperForTarget(this)    // 날짜의 Shift별 정보, Target 수량 정보 저장
    val _report_db = DBHelperForReport(this)    // 날짜의 Shift별 한시간 간격의 Actual 수량 저장

    private var _doubleBackToExitPressedOnce = false
    private var _last_count_received_time = DateTime()

    var _is_call = false

    var watching_count = 0      // 디버깅 창용 변수

    private val _broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    btn_wifi_state.isSelected = true
                } else {
                    btn_wifi_state.isSelected = false
                }
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

        // USB state
        btn_usb_state?.isSelected = false

        // 시작시 work_idx 값이 없으면 초기화 한다.
        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") {
            endTodayWork()
//            pieces_qty = 0
//            pairs_qty = 0
//
//            AppGlobal.instance.set_last_received("")                // 다운타임 검사용 변수도 초기화
//            AppGlobal.instance.set_downtime_idx("")
//
//            AppGlobal.instance.set_design_info_idx("")
//            AppGlobal.instance.set_model("")
//            AppGlobal.instance.set_article("")
//            AppGlobal.instance.set_material_way("")
//            AppGlobal.instance.set_component("")
//            AppGlobal.instance.set_cycle_time(0)
//            AppGlobal.instance.reset_product_idx()
        }

        mHandler = MyHandler(this)

        wv_view_main.setInitialScale(100)

        // button click event
        if (AppGlobal.instance.get_long_touch()) {
            btn_home.setOnLongClickListener { changeFragment(0); true }
            img_btn_home.setOnLongClickListener { changeFragment(0); true }

            btn_push_to_app.setOnLongClickListener {
                startActivity(
                    Intent(
                        this,
                        PushActivity::class.java
                    )
                ); true
            }
            img_btn_push_to_app.setOnLongClickListener {
                startActivity(
                    Intent(
                        this,
                        PushActivity::class.java
                    )
                ); true
            }

            btn_actual_count_edit.setOnLongClickListener {
                startActivity(
                    Intent(
                        this,
                        ActualCountEditActivity::class.java
                    )
                ); true
            }
            img_btn_actual_count_edit.setOnLongClickListener {
                startActivity(
                    Intent(
                        this,
                        ActualCountEditActivity::class.java
                    )
                ); true
            }

            btn_downtime.setOnLongClickListener { startDowntimeActivity(); true }
            img_btn_downtime.setOnLongClickListener { startDowntimeActivity(); true }

            btn_defective_info.setOnLongClickListener {
                startActivity(
                    Intent(
                        this,
                        DefectiveActivity::class.java
                    )
                ); true
            }
            img_btn_defective_info.setOnLongClickListener {
                startActivity(
                    Intent(
                        this,
                        DefectiveActivity::class.java
                    )
                ); true
            }

            btn_worksheet.setOnLongClickListener { startWorkSheetActivity(); true }
            img_btn_worksheet.setOnLongClickListener { startWorkSheetActivity(); true }

            btn_production_report.setOnLongClickListener {
                startActivity(
                    Intent(
                        this,
                        ProductionReportActivity::class.java
                    )
                ); true
            }
            img_btn_production_report.setOnLongClickListener {
                startActivity(
                    Intent(
                        this,
                        ProductionReportActivity::class.java
                    )
                ); true
            }

            btn_component.setOnLongClickListener { startComponentActivity(); true }
            img_btn_component.setOnLongClickListener { startComponentActivity(); true }

            btn_worksheet_stop.setOnLongClickListener {
                workSheetToggle = false
                workSheetShow = false
                ll_worksheet_view.visibility = View.GONE
//                wv_view_main.visibility = View.GONE
                val cview = vp_fragments?.getChildAt(1)
                cview?.btn_toggle_sop?.visibility = View.VISIBLE
                true
            }

        } else {
            btn_home.setOnClickListener { changeFragment(0) }
            img_btn_home.setOnClickListener { changeFragment(0) }

            btn_push_to_app.setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        PushActivity::class.java
                    )
                )
            }
            img_btn_push_to_app.setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        PushActivity::class.java
                    )
                )
            }

            btn_actual_count_edit.setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        ActualCountEditActivity::class.java
                    )
                )
            }
            img_btn_actual_count_edit.setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        ActualCountEditActivity::class.java
                    )
                )
            }

            btn_downtime.setOnClickListener { startDowntimeActivity() }
            img_btn_downtime.setOnClickListener { startDowntimeActivity() }

            btn_defective_info.setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        DefectiveActivity::class.java
                    )
                )
            }
            img_btn_defective_info.setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        DefectiveActivity::class.java
                    )
                )
            }

            btn_worksheet.setOnClickListener { startWorkSheetActivity() }
            img_btn_worksheet.setOnClickListener { startWorkSheetActivity() }

            btn_production_report.setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        ProductionReportActivity::class.java
                    )
                )
            }
            img_btn_production_report.setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        ProductionReportActivity::class.java
                    )
                )
            }

            btn_component.setOnClickListener { startComponentActivity() }
            img_btn_component.setOnClickListener { startComponentActivity() }

            btn_worksheet_stop.setOnClickListener {
                workSheetToggle = false
                workSheetShow = false
                ll_worksheet_view?.visibility = View.GONE
//                wv_view_main?.visibility = View.GONE
                val cview = vp_fragments?.getChildAt(1)
                cview?.btn_toggle_sop?.visibility = View.VISIBLE
            }
        }

        // 디버깅용 창
        top_logo.setOnClickListener {
            if (watching_count >= 4) {
                watching_count = 0
                startActivity(Intent(this, WatchingActivity::class.java))
            }
            watching_count++
//            Log.e("watching", "" + watching_count)
            Handler().postDelayed({ watching_count = 0 }, 2000)
        }

/*
        {"cmd":"barcode", "value":"1003"}
        {"cmd":"stitch", "value":"start"}
        {"cmd":"count", "value":"1", "runtime":""}
        {"cmd":"T", "value":"2", "runtime":""}
*/
        // Stitch 발생
//        start_stitch.setOnClickListener {
//            val buffer = "{\"cmd\":\"stitch\", \"value\":\"start\"}"
//            handleData(buffer)
//        }
//        start_count.setOnClickListener {
//            val buffer = "{\"cmd\":\"count\", \"value\":\"1\", \"runtime\":\"\"}"
//            handleData(buffer)
//        }

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

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageScrollStateChanged(position: Int) {}
        })

        // 지난 DownTime과 Design이 있으면 삭제한다.
        RemoveDownTimeData()
        checkDesignData()
        fetchPushData()

        start_timer()

        val filter = IntentFilter()
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(UsbService.ACTION_NO_USB)
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED)
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED)
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)
        registerReceiver(mUsbReceiver, filter)
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
//                        wv_view_main.loadUrl(file_url)
                    changeFragment(2)
                    val cview = vp_fragments?.getChildAt(1)
                    cview?.btn_toggle_sop?.visibility = View.GONE
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    public override fun onResume() {
        super.onResume()

//        val filter = IntentFilter()
//        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED)
//        filter.addAction(UsbService.ACTION_NO_USB)
//        filter.addAction(UsbService.ACTION_USB_DISCONNECTED)
//        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED)
//        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)
//        registerReceiver(mUsbReceiver, filter)

        startService(UsbService::class.java, usbConnection, null) // Start UsbService(if it was not started before) and Bind it
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
        registerReceiver(_broadcastReceiver, IntentFilter(Constants.BR_ADD_COUNT))

        // Actual 값을 좌측에 표시
        tv_report_count.text = "" + AppGlobal.instance.get_current_shift_actual_cnt()

        // USB state
        btn_usb_state.isSelected = AppGlobal.instance._usb_state

        updateView()
        fetchRequiredData()
        fetchComponentData()    // Parts Cycle Time. 처음 실행후 1시간마다 실행
    }

    public override fun onPause() {
        super.onPause()
//        unregisterReceiver(mUsbReceiver)
        unbindService(usbConnection)
        unregisterReceiver(_broadcastReceiver)
    }

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

    private fun updateView() {
        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false
    }

    fun changeFragment(pos:Int) {
        vp_fragments.setCurrentItem(pos, true)
    }

    // 시작시 호출
    // 이후 10분에 한번씩 호출
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
                return shift3
            }
        }
        return null
    }

    private fun fetchDesignData() {
        if (AppGlobal.instance.get_server_ip() == "") return

        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "design",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx())

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            if (code == "00"){
                var list = result.getJSONArray("item")
                AppGlobal.instance.set_design_info(list)

                btn_component_info?.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_component_info?.setBackgroundColor(Color.parseColor("#f8ad13"))

            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    /*
     *  당일 작업시간 가져오기. 새벽이 지난 시간은 1일을 더한다.
     *  전일 작업이 끝나지 않았을수 있기 때문에 전일 데이터도 가져온다.
     */
    private fun fetchWorkData2() {

        var dt = DateTime()
        val shift3: JSONObject? = fetchManualShift()      // manual 데이터가 있으면 가져온다.

        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "work_time2",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "today" to dt.toString("yyyy-MM-dd"),
            "yesterday" to dt.minusDays(1).toString("yyyy-MM-dd"))  // 전일 데이터

//        OEEUtil.LogWrite(params.toString(), "Shift worktime Request params")

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var list1 = result.getJSONArray("item1")
                var list2 = result.getJSONArray("item2")
                if (shift3 != null) {
                    var today_shift = shift3
                    if (list1.length()>0) {
                        val item = list1.getJSONObject(0)
                        today_shift.put("date", item["date"])
                        today_shift.put("line_idx", item["line_idx"])
                        today_shift.put("line_name", item["line_name"])
                    } else {
                        today_shift.put("date", dt.toString("yyyy-MM-dd"))
                        today_shift.put("line_idx", "0")
                        today_shift.put("line_name", "Manual")
                    }
                    list1.put(today_shift)

                    var yester_shift = shift3
                    if (list2.length()>0) {
                        val item = list2.getJSONObject(0)
                        yester_shift.put("date", item["date"])
                        yester_shift.put("line_idx", item["line_idx"])
                        yester_shift.put("line_name", item["line_name"])
                    } else {
                        yester_shift.put("date", dt.minusDays(1).toString("yyyy-MM-dd"))
                        yester_shift.put("line_idx", "0")
                        yester_shift.put("line_name", "Manual")
                    }
                    list2.put(yester_shift)
                }
                list1 = handleWorkData(list1)
                list2 = handleWorkData(list2)
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
    private fun fetchWorkData() {
        // 당일과 전일 데이터를 모두 불러왔는지 체크하기 위한 변수 (2가 되면 모두 읽어옴)
        var _load_work_data_cnt = 0

        var dt = DateTime()
        val shift3: JSONObject? = fetchManualShift()      // manual 데이터가 있으면 가져온다.

        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "work_time",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "date" to dt.toString("yyyy-MM-dd"))

        OEEUtil.LogWrite(params.toString(), "Shift worktime Request params")

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var list1 = result.getJSONArray("item")
                if (shift3 != null) {
                    var today_shift = shift3
                    if (list1.length()>0) {
                        val item = list1.getJSONObject(0)
                        today_shift.put("date", item["date"])
                        today_shift.put("line_idx", item["line_idx"])
                        today_shift.put("line_name", item["line_name"])
                    } else {
                        today_shift.put("date", dt.toString("yyyy-MM-dd"))
                        today_shift.put("line_idx", "0")
                        today_shift.put("line_name", "Manual")
                    }
                    list1.put(today_shift)
                }
                list1 = handleWorkData(list1)
                AppGlobal.instance.set_today_work_time(list1)
//Log.e("today shift", list1.toString())
                _load_work_data_cnt++
                if (_load_work_data_cnt >= 2) compute_work_shift()
            } else {
                ToastOut(this, msg, true)
            }
        })

        // 전날짜 데이터 가져오기
        var prev_params = listOf(
            "code" to "work_time",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "date" to dt.minusDays(1).toString("yyyy-MM-dd"))

        request(this, uri, false, prev_params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var list2 = result.getJSONArray("item")
                if (shift3 != null) {
                    var yester_shift = shift3
                    if (list2.length()>0) {
                        val item = list2.getJSONObject(0)
                        yester_shift.put("date", item["date"])
                        yester_shift.put("line_idx", item["line_idx"])
                        yester_shift.put("line_name", item["line_name"])
                    } else {
                        yester_shift.put("date", dt.minusDays(1).toString("yyyy-MM-dd"))
                        yester_shift.put("line_idx", "0")
                        yester_shift.put("line_name", "Manual")
                    }
                    list2.put(yester_shift)
                }
                list2 = handleWorkData(list2)
                AppGlobal.instance.set_prev_work_time(list2)
//Log.e("yester shift", list2.toString())
                _load_work_data_cnt++
                if (_load_work_data_cnt >= 2) compute_work_shift()
            } else {
                ToastOut(this, msg, true)
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
//        Log.e("current work time", list.toString())

        // 현재 쉬프트의 종료 시간을 구한다. 자동 종료를 위해
        // 종료 시간이 있으면 다음 시작 시간을 구할 필요없음. 종료되면 이 로직이 실행되므로 자동으로 구해지기 때문..

        if (list.length() > 0) {

            // DB에 Shift 정보를 저장한다.
            // Production report 때문에 그날의 정보를 모두 저장해야 함.
            var target_type = AppGlobal.instance.get_target_type()

            for (i in 0..(list.length() - 1)) {

                val work_item = list.getJSONObject(i)
                var target = "0"
                var target_int = 0

                if (target_type.substring(0, 6) == "cycle_") {

                    val shift_time = AppGlobal.instance.get_current_shift_time()    // 현 시프트
                    var current_cycle_time =
                        AppGlobal.instance.get_cycle_time()    // 현재 선택된 디자인의 사이클 타임

                    if (shift_time != null && current_cycle_time > 0) {
                        val shift_stime = OEEUtil.parseDateTime(shift_time["work_stime"].toString())

                        var work_idx = AppGlobal.instance.get_product_idx()         // 현재 선택된 디자인
                        if (work_idx == "") work_idx = "0"

                        val design_db = DBHelperForDesign(this)
                        val db_list = design_db.gets()

                        val work_etime = shift_time["work_etime"].toString()        // 현 시프트의 종료시간

                        for (i in 0..((db_list?.size ?: 1) - 1)) {
                            val design_item = db_list?.get(i)
                            val work_idx2 = design_item?.get("work_idx").toString()

                            if (work_idx == work_idx2) {        // 현재 진행중인 디자인

                                var start_dt =
                                    OEEUtil.parseDateTime(design_item?.get("start_dt").toString())      // 디자인의 시작시간
                                val shift_end_dt =
                                    OEEUtil.parseDateTime(work_etime)                        // 시프트의 종료 시간

                                if (start_dt < shift_stime) start_dt = shift_stime

                                // 설정되어 있는 휴식 시간 정보
                                val _planned1_stime =
                                    OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString())
                                val _planned1_etime =
                                    OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString())
                                val _planned2_stime =
                                    OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString())
                                val _planned2_etime =
                                    OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString())

                                // 휴식시간 초. 끝나는 시간까지 계산 (시프트의 총 타겟수를 구하기 위해 무조건 계산함)
                                val d1 = AppGlobal.instance.compute_time(
                                    start_dt,
                                    shift_end_dt,
                                    _planned1_stime,
                                    _planned1_etime
                                )
                                val d2 = AppGlobal.instance.compute_time(
                                    start_dt,
                                    shift_end_dt,
                                    _planned2_stime,
                                    _planned2_etime
                                )

                                // 디자인의 시작부터 시프트 종료시간까지 (초)
                                val start_at_target = AppGlobal.instance.get_start_at_target()

                                val work_time =
                                    ((shift_end_dt.millis - start_dt.millis) / 1000) - d1 - d2 - start_at_target
                                target_int += ((work_time / current_cycle_time).toInt() + start_at_target) // 현 시간에 만들어야 할 갯수

                            } else {        // 지난 디자인
                                target_int += design_item?.get("target").toString().toInt()
                            }
                        }
                        target = target_int.toString()
//                        target = AppGlobal.instance.get_target_server_shift(item["shift_idx"].toString())
                    }
                } else if (target_type.substring(0, 6) == "server") {
                    // AppGlobal.instance.get_target_by_group() 값이 참일 경우,
                    // 몇몇 업체에서 이 옵션이 선택되었을 경우 getlist1.php -> 'target' 에서 'daytargetsum' 값을 참조함.
                    //
                    target = if (AppGlobal.instance.get_target_by_group()) AppGlobal.instance.get_target_server_shift(work_item["shift_idx"].toString())
                    else AppGlobal.instance.get_target_manual_shift(work_item["shift_idx"].toString())
                } else {
                    target = AppGlobal.instance.get_target_manual_shift(work_item["shift_idx"].toString())
                }

                if (target == null || target == "") target = "0"

                val row = _target_db.get(work_item["date"].toString(), work_item["shift_idx"].toString())

                if (row == null) { // insert
//                    Log.e("db info", "===> " + item["date"].toString() + " : " + item["shift_idx"].toString() + " : null")
                    _target_db.add(work_item["date"].toString(), work_item["shift_idx"].toString(), work_item["shift_name"].toString(), target,
                        work_item["work_stime"].toString(), work_item["work_etime"].toString())
                } else {           // update
//                    Log.e("db info", "===> " + item["date"].toString() + " : " + item["shift_idx"].toString() + " : " + row.toString())
                    if (target != row?.get("target").toString()) {
                        _target_db.update(row["idx"].toString(), work_item["shift_name"].toString(), target,
                            work_item["work_stime"].toString(), work_item["work_etime"].toString()
                        )
                    }
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

    /*
     *  작업 시간을 검사한다.
     *  첫 작업 시간보다 작은 시간이 보일경우 하루가 지난것이므로 1일을 더한다.
     */
    private fun handleWorkData(list: JSONArray) : JSONArray {
        var shift_stime = DateTime()
        for (i in 0..(list.length() - 1)) {
            var item = list.getJSONObject(i)

            val over_time = item["over_time"]   // 0
            val date = item["date"].toString()  // 2019-04-05
            if (i==0) { // 첫시간 기준
                shift_stime = OEEUtil.parseDateTime(date + " " + item["available_stime"] + ":00")   // 2019-04-05 06:01:00  (available_stime = 06:01)
            }

            var work_stime = OEEUtil.parseDateTime(date + " " + item["available_stime"] + ":00")    // 2019-04-05 06:01:00
            var work_etime = OEEUtil.parseDateTime(date + " " + item["available_etime"] + ":00")    // 2019-04-05 14:00:00
            work_etime = work_etime.plusHours(over_time.toString().toInt())

            val planned1_stime_txt = date + " " + if (item["planned1_stime"] == "") "00:00:00" else item["planned1_stime"].toString() + ":00"   // 2019-04-05 11:30:00
            val planned1_etime_txt = date + " " + if (item["planned1_etime"] == "") "00:00:00" else item["planned1_etime"].toString() + ":00"   // 2019-04-05 13:00:00
            val planned2_stime_txt = date + " " + if (item["planned2_stime"] == "") "00:00:00" else item["planned2_stime"].toString() + ":00"   // 2019-04-05 00:00:00
            val planned2_etime_txt = date + " " + if (item["planned2_etime"] == "") "00:00:00" else item["planned2_etime"].toString() + ":00"   // 2019-04-05 00:00:00

            var planned1_stime_dt = OEEUtil.parseDateTime(planned1_stime_txt)
            var planned1_etime_dt = OEEUtil.parseDateTime(planned1_etime_txt)
            var planned2_stime_dt = OEEUtil.parseDateTime(planned2_stime_txt)
            var planned2_etime_dt = OEEUtil.parseDateTime(planned2_etime_txt)

            // 첫 시작시간 보다 작은 값이면 하루가 지난 날짜임
            // 종료 시간이 시작 시간보다 작은 경우도 하루가 지난 날짜로 처리
            if (shift_stime.secondOfDay > work_stime.secondOfDay) work_stime = work_stime.plusDays(1)
            if (shift_stime.secondOfDay > work_etime.secondOfDay || work_stime.secondOfDay > work_etime.secondOfDay) work_etime = work_etime.plusDays(1)
            if (shift_stime.secondOfDay > planned1_stime_dt.secondOfDay) planned1_stime_dt = planned1_stime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned1_etime_dt.secondOfDay || planned1_stime_dt.secondOfDay > planned1_etime_dt.secondOfDay) planned1_etime_dt = planned1_etime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned2_stime_dt.secondOfDay) planned2_stime_dt = planned2_stime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned2_etime_dt.secondOfDay || planned2_stime_dt.secondOfDay > planned2_etime_dt.secondOfDay) planned2_etime_dt = planned2_etime_dt.plusDays(1)

            item.put("work_stime", work_stime.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("work_etime", work_etime.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned1_stime_dt", planned1_stime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned1_etime_dt", planned1_etime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned2_stime_dt", planned2_stime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned2_etime_dt", planned2_etime_dt.toString("yyyy-MM-dd HH:mm:ss"))
//            Log.e("new list", ""+item.toString())
        }
        return list
    }

    /*
     *  downtime check time
     *  select_yn = 'Y' 것만 가져온다.
     *  etc_yn = 'Y' 이면 second 값, 'N' 이면 name 값이 리턴된다. (1800)
     */
    private fun fetchDownTimeType() {
        val uri = "/getlist1.php"
        var params = listOf("code" to "check_time")

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var value = result.getString("value")
                if (value == "Cycle Time") {
                    AppGlobal.instance.set_downtime_type("Cycle Time")
                    val sec = AppGlobal.instance.get_cycle_time()
                    value = if (sec==0 || sec==null) "600" else sec.toString()
                } else {
                    AppGlobal.instance.set_downtime_type("")
                }
                AppGlobal.instance.set_downtime_sec(value)

                OEEUtil.LogWrite(AppGlobal.instance.get_downtime_type() + " = " + value, "Down time Sec")
//                val s = value.toInt()
//                if (s > 0) {
//                }
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
            var code = result.getString("code")
            if (code == "00") {
                AppGlobal.instance.set_color_code(result.getJSONArray("item"))
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun sendOeeGraphData() {
        if (AppGlobal.instance.get_server_ip().trim() == "") return
        var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
        if (item == null) return

        val uri = "/Sgraph.php"
        var params = listOf(
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
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

//    private fun fetchOEEGraph() {
//        if (AppGlobal.instance.get_server_ip().trim() == "") return
//
//        var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
//        if (item == null) {
//            AppGlobal.instance.set_availability("0")
//            AppGlobal.instance.set_performance("0")
//            AppGlobal.instance.set_quality("0")
//            return
//        }
//
//        val work_date = item["date"].toString()
//
//        val uri = "/getoee.php"
//        var params = listOf(
//            "mac_addr" to AppGlobal.instance.getMACAddress(),
//            "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
//            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
//            "factory_idx" to AppGlobal.instance.get_room_idx(),
//            "line_idx" to AppGlobal.instance.get_line_idx(),
//            "work_date" to work_date
//        )
//
////Log.e("oeegraph", "mac_addr="+AppGlobal.instance.getMACAddress()+"&shift_idx="+AppGlobal.instance.get_current_shift_idx()+"&" +
////        "factory_parent_idx="+AppGlobal.instance.get_factory_idx()+"&factory_idx="+AppGlobal.instance.get_room_idx()+"&line_idx="+AppGlobal.instance.get_line_idx()+
////        "&work_date="+work_date)
//
//        request(this, uri, false, false, params, { result ->
//            var code = result.getString("code")
//            if (code == "00") {
//                val availability = result.getString("availability").toString()
//                val performance = result.getString("performance").toString()
//                val quality = result.getString("quality").toString()
//
////Log.e("oeegraph", "avail="+availability+", performance="+performance+", quality="+quality)
//
////                Log.e("fetchOEEGraph", "availability = "+availability)
////                Log.e("fetchOEEGraph", "performance = "+performance)
////                Log.e("fetchOEEGraph", "quality = "+quality)
//
//                val app_perform = AppGlobal.instance.get_performance()
//
//                AppGlobal.instance.set_availability(availability)
//                AppGlobal.instance.set_performance(performance)
//                AppGlobal.instance.set_quality(quality)
//
//                var old_perform = 0.0f
//                var new_perform = 0.0f
//
//                try {
//                    old_perform = app_perform as Float
//                    new_perform = performance as Float
//                } catch (e:Exception) {
//                }
//
//                // performance가 100%를 넘었으면 푸시를 보낸다. 단, 이전에 100% 이전인 경우만..
//                if (new_perform >= 100.0f) {
//                    if (old_perform < 100.0f) {
//                        Log.e("fetchOEEGraph", "push send")
//                        sendPush("SYS: PERFORMANCE")
//                    }
//                }
//            } else {
//                ToastOut(this, result.getString("msg"), true)
//            }
//        })
//    }

    fun fetchPushData() {
        val uri = "/getlist1.php"
        var params = listOf("code" to "text")

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
        val dt = DateTime()
        val shift_idx = AppGlobal.instance.get_current_shift_idx()
        val uri = "/getlist1.php"
        var params = listOf("code" to "target",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "date" to dt.toString("yyyy-MM-dd"),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to shift_idx)

        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                val daytargetsum = result.getString("daytargetsum").toString()
                AppGlobal.instance.set_target_server_shift(shift_idx, daytargetsum)
//                Log.e("HomeFrag", "tarbygr 2=" + AppGlobal.instance.get_target_by_group())
//                Log.e("HomeFrag", "tarbygr 2=" + daytargetsum)
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    // Parts cycle time 이라는 기능
    // 남은 시간이 10시간 미만인 콤포넌트가 있으면 푸시 발송. 갯수만큼
    private fun fetchComponentData() {
//        UtilLocalStorage.setStringSet(this, "notified_component_set", setOf())
        val uri = "/getlist1.php"
        var params = listOf("code" to "component",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx())

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var list = result.getJSONArray("item")
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
                    tv_component_count.text = "" + cnt
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

    private fun sendPing() {
        tv_ms.text = "-" + " ms"
        if (AppGlobal.instance.get_server_ip() == "") return

        val currentTimeMillisStart = System.currentTimeMillis()
        val uri = "/ping.php"

        request(this, uri, false, false, null, { result ->
            val currentTimeMillisEnd = System.currentTimeMillis()
            val millis = currentTimeMillisEnd - currentTimeMillisStart

            var code = result.getString("code")
            if (code == "00") {
                btn_server_state.isSelected = true
                AppGlobal.instance._server_state = true
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

    // 30분마다 현재 target을 서버에 저장
    // 작업 시간이 아닐경우는 Pass
    private fun updateCurrentWorkTarget() {
        var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
        if (item != null) {
            var _total_target = 0
            var target_type = AppGlobal.instance.get_target_type()
            if (target_type.substring(0, 6) == "server") {
                // 신서버용
                val work_idx = AppGlobal.instance.get_product_idx()

                // 전체 디자인을 가져온다.
                var db = DBHelperForDesign(this)
                var db_list = db.gets()
                for (i in 0..((db_list?.size ?: 1) - 1)) {
                    val item = db_list?.get(i)
                    val work_idx2 = item?.get("work_idx").toString()
                    val target2 = item?.get("target").toString().toInt()
                    val start_dt2 =
                        OEEUtil.parseDateTime(item?.get("start_dt").toString())    // 디자인의 시작시간

                    if (work_idx == work_idx2) {    // 현재 진행중인 디자인
                        val current_cycle_time = AppGlobal.instance.get_cycle_time()
                        val shift_time = AppGlobal.instance.get_current_shift_time()

                        if (shift_time != null && current_cycle_time > 0) {
                            val work_etime = shift_time["work_etime"].toString()
                            val shift_end_dt = OEEUtil.parseDateTime(work_etime)    // 시프트의 종료 시간

                            // 설정되어 있는 휴식 시간
                            val _planned1_stime =
                                OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString())
                            val _planned1_etime =
                                OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString())
                            val _planned2_stime =
                                OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString())
                            val _planned2_etime =
                                OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString())

                            val d1 = AppGlobal.instance.compute_time(
                                start_dt2,
                                shift_end_dt,
                                _planned1_stime,
                                _planned1_etime
                            )
                            val d2 = AppGlobal.instance.compute_time(
                                start_dt2,
                                shift_end_dt,
                                _planned2_stime,
                                _planned2_etime
                            )

                            // 디자인의 시작부터 시프트 종료시간까지 (초)
                            val start_at_target = AppGlobal.instance.get_start_at_target()

                            val work_time =
                                ((shift_end_dt.millis - start_dt2.millis) / 1000) - d1 - d2 - start_at_target
                            _total_target += (work_time / current_cycle_time).toInt() + start_at_target // 현 시간에 만들어야 할 갯수
                        }
                    } else {        // 지난 디자인
                        _total_target += target2
                    }
                }
                // 구서버용
//                when (item["shift_idx"]) {
//                    "1" -> _total_target = AppGlobal.instance.get_target_server_shift("1").toInt()
//                    "2" -> _total_target = AppGlobal.instance.get_target_server_shift("2").toInt()
//                    "3" -> _total_target = AppGlobal.instance.get_target_server_shift("3").toInt()
//                }
            } else if (target_type.substring(0, 6) == "device") {
                when (item["shift_idx"]) {
                    "1" -> _total_target = AppGlobal.instance.get_target_manual_shift("1").toInt()
                    "2" -> _total_target = AppGlobal.instance.get_target_manual_shift("2").toInt()
                    "3" -> _total_target = AppGlobal.instance.get_target_manual_shift("3").toInt()
                }
            }
            Log.e("updateCurrentWorkTarget", "target_type=" + target_type + ", _total_target=" + _total_target)
            if (_total_target > 0) {
                // 구서버용
//                val uri = "/sendtarget.php"
//                var params = listOf(
//                    "mac_addr" to AppGlobal.instance.getMACAddress(),
//                    "date" to item["date"].toString(),
//                    "shift_idx" to  item["shift_idx"],     // AppGlobal.instance.get_current_shift_idx()
//                    "target_count" to _total_target)

                // 신서버용
                val uri = "/Starget.php"
                var params = listOf(
                    "mac_addr" to AppGlobal.instance.getMACAddress(),
                    "didx" to AppGlobal.instance.get_design_info_idx(),
                    "target" to _total_target,
                    "shift_idx" to  item["shift_idx"]     // AppGlobal.instance.get_current_shift_idx()
                    )

                request(this, uri, true,false, params, { result ->
                    val code = result.getString("code")
//                    Log.e("Starget result", "= " + msg.toString())
                    if(code != "00"){
                        ToastOut(this, result.getString("msg"), true)
                    }
                })
            }
        }
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
        pairs_qty = 0

        _prepare_time = 0L

        AppGlobal.instance.set_last_received("")                // 다운타임 검사용 변수도 초기화
        AppGlobal.instance.set_downtime_idx("")

        AppGlobal.instance.set_design_info_idx("")
        AppGlobal.instance.set_model("")
        AppGlobal.instance.set_article("")
        AppGlobal.instance.set_material_way("")
        AppGlobal.instance.set_component("")
        AppGlobal.instance.set_cycle_time(0)
        AppGlobal.instance.reset_product_idx()

//        AppGlobal.instance.set_current_shift_actual_cnt(0)

        var db = SimpleDatabaseHelperBackup(this)
        db.delete()

        var db2 = DBHelperForDownTime(this)
        db2.delete()

        var db3 = DBHelperForCount(this)
        db3.delete()

//        var db4 = DBHelperForComponent(this)
//        db4.delete()

        var db5 = DBHelperForDesign(this)
        db5.delete()

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
//                AppGlobal.instance.set_current_shift_actual_cnt(0)      // 토탈 Actual 초기화
//                AppGlobal.instance.set_last_received("")                // 다운타임 검사용 변수도 초기화
//                AppGlobal.instance.set_downtime_idx("")
//                tv_report_count.text = "0"                              // 좌측 Report 버튼의 Actual 값도 0으로 초기화
//                tv_defective_count.text = "0"                           // 카운트 뷰의 Defective 값도 0으로 초기화
//
//                AppGlobal.instance.set_design_info_idx("")
//                AppGlobal.instance.set_model("")
//                AppGlobal.instance.set_article("")
//                AppGlobal.instance.set_material_way("")
//                AppGlobal.instance.set_component("")
//                AppGlobal.instance.set_cycle_time(0)
//                AppGlobal.instance.reset_product_idx()                  // work idx 초기화
//
//                AppGlobal.instance.set_worker_no("")                    // 작업자도 리셋
//                AppGlobal.instance.set_worker_name("")
//
//                var db5 = DBHelperForDesign(this)                       // DB 선택된 디자인 idx 값도 초기화
//                db5.delete()
//
//                // 마지막 작업이 끝났으면 완전 초기화
//                if (_last_working == true) {
//                    endTodayWork()
//                    _last_working = false
//                }
                // shift만 바뀌어도 모두 삭제
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
//    private val _timer_task1 = Timer()          // 서버 접속 체크 Ping test. Shift의 Target 정보
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

        // 10초마다
//        val task1 = object : TimerTask() {
//            override fun run() {
//                runOnUiThread {
//                    RemoveDownTimeData()    // Shift가 지난 다운타임 데이터를 삭제한다.
//                }
//            }
//        }
//        _timer_task1.schedule(task1, 2000, 10000)

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
//                    fetchOEEGraph()
                    RemoveDownTimeData()    // Shift가 지난 다운타임 데이터를 삭제한다.
                }
            }
        }
        _timer_task3.schedule(task3, 3000, 30000)

        // 1시간마다
        val task4 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    fetchComponentData()    // Parts Cycle Time
                    sendOeeGraphData()
                    fetchPushData()
//                    updateCurrentWorkTarget()
                }
            }
        }
        _timer_task4.schedule(task4, 1200000, 3600000)
    }
    private fun cancel_timer () {
        _downtime_timer.cancel()
//        _timer_task1.cancel()
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
                    AppGlobal.instance._usb_state = true
                    tv_usb.setTextColor(Color.parseColor("#f8ad13"))
                }
                else -> {
                    btn_usb_state.isSelected = false
                    AppGlobal.instance._usb_state = false
                    tv_usb.setTextColor(Color.parseColor("#EEEEEE"))
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
                Log.e("USB handel", "usb = " + recvBuffer)

                saveRowData(cmd, value, runtime)

            } catch(e: JsonSyntaxException) {
                OEEUtil.LogWrite(e.toString(), "USB Input Error")
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
    var pairs_qty = 0
    var runtime_total = 0

    private fun saveRowData(cmd: String, value: JsonElement, runtime: String) {

        if (AppGlobal.instance.get_sound_at_count()) AppGlobal.instance.playSound(this)

        if (cmd=="barcode") {
            val arr = value.asJsonArray
            var didx = arr[0].asString      // design idx
//            var number = -1

            if (arr.size() > 1) {
                didx = value.asJsonArray[0].asString
//                val value2 = value.asJsonArray[1].asString
//                number = value2.replace("[^0-9]", "").toInt()
            }
            var list = AppGlobal.instance.get_design_info()

            for (i in 0..(list.length() - 1)) {
                val item = list.getJSONObject(i)
                val idx = item.getString("idx")
                if (idx==didx) {
//                    if (number > 0) AppGlobal.instance.set_pieces_info(number.toString())

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

            // 이제 count 에서 downtime 처리 안하고 여기서만 함. 2020-01-16
            _last_count_received_time = DateTime()      // downtime 시간 초기화 (구)
            AppGlobal.instance.set_last_received(DateTime().toString("yyyy-MM-dd HH:mm:ss")) // Downtime 초기화 (신)
            sendEndDownTimeForce()      // 처리안된 Downtime 강제 완료

        } else if (cmd == "T" || cmd == "count") {

            var current_actual_cnt = AppGlobal.instance.get_current_shift_actual_cnt()

            // 작업 시간인지 확인
            val cur_shift: JSONObject ?= AppGlobal.instance.get_current_shift_time()
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
                if ((planned1_stime.millis < now_millis && planned1_etime.millis > now_millis ) ||
                    (planned2_stime.millis < now_millis && planned2_etime.millis > now_millis )) {
                    ToastOut(this, R.string.msg_cannot_work_planned_time, true)
                    return
                }
            }

            // Operator 선택 확인
            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                ToastOut(this, R.string.msg_no_operator, true)
                return
            }

            // 선택한 Design이 있는지 확인
            val work_idx = AppGlobal.instance.get_product_idx()
            if (work_idx == "") {
                ToastOut(this, R.string.msg_select_design, true)
                return
            }

            // Pieces와 Pairs값 선택 확인
            val pieces_value = AppGlobal.instance.get_pieces_info()
            val pairs_value = AppGlobal.instance.get_pairs_info()
            if (pieces_value == "" || pairs_value == "") {
                ToastOut(this, R.string.msg_layer_not_selected, true)
                return
            }


            val shift_idx = cur_shift["shift_idx"]      // 현재 작업중인 Shift
            var inc_count = 0
//            var inc_count = value.toString().toInt()


            val max_pieces = AppGlobal.instance.get_pieces_info()
            val max_pairs = AppGlobal.instance.get_pairs_info()

            pieces_qty++

            if (pieces_qty >= max_pieces.toInt()) {
                pieces_qty = pieces_qty - max_pieces.toInt()
                pairs_qty++

                var pairs_int = 1
                var tmp_count = 1

                when (max_pairs) {
                    "1/8" -> pairs_int = 8
                    "1/7" -> pairs_int = 7
                    "1/6" -> pairs_int = 6
                    "1/5" -> pairs_int = 5
                    "1/4" -> pairs_int = 4
                    "1/3" -> pairs_int = 3
                    "1/2" -> pairs_int = 2
                    "1" -> tmp_count = 1
                    "2" -> tmp_count = 2
                    "3" -> tmp_count = 3
                    "4" -> tmp_count = 4
                    "5" -> tmp_count = 5
                    "6" -> tmp_count = 6
                    "7" -> tmp_count = 7
                    "8" -> tmp_count = 8
                    "9" -> tmp_count = 9
                    "10" -> tmp_count = 10
                    else -> return
                }
                if (pairs_qty >= pairs_int) {
                    pairs_qty = 0
                    inc_count = tmp_count
                }
            }

            if (runtime != null && runtime != "") runtime_total += runtime.toInt()

            if (inc_count <= 0) return

            // total count
//            val cnt = AppGlobal.instance.get_current_shift_actual_cnt() + inc_count
//            AppGlobal.instance.set_current_shift_actual_cnt(cnt)

            val cnt = current_actual_cnt + inc_count

            tv_report_count.text = "" + cnt

            // cmd = "stitch" 코맨드에서 처리하는걸로 바뀜. 2020-01-16
            _last_count_received_time = DateTime()      // downtime 시간 초기화 (구)
            AppGlobal.instance.set_last_received(DateTime().toString("yyyy-MM-dd HH:mm:ss")) // Downtime 초기화 (신)
            sendEndDownTimeForce()      // 처리안된 Downtime 강제 완료

            // 서버 호출 (장치에서 들어온 값, 증분값, 총수량)
            sendCountData(value.toString(), inc_count, cnt, runtime_total.toString())  // 서버에 카운트 정보 전송

            runtime_total = 0

            // DB에 Actual 저장
            var db = DBHelperForDesign(this)
            val row = db.get(work_idx)
            if (row != null) {
                val actual = (row!!["actual"].toString().toInt() + inc_count)
                db.updateWorkActual(work_idx, actual)
            }

//            _stitch_db.add(work_idx, value.toString())

            // Production Report를 위한 DB저장
            //val now = DateTime()
            val now = cur_shift["date"]
            val date = now.toString()
            val houly = DateTime().toString("HH")

            val rep = _report_db.get(date, houly, shift_idx.toString())
            if (rep == null) {
                _report_db.add(date, houly, shift_idx.toString(), inc_count)
            } else {
                val idx = rep!!["idx"].toString()
                val actual = rep!!["actual"].toString().toInt() + inc_count
                _report_db.updateActual(idx, actual)
            }
        }
    }

    // 장치에서 들어온 값, 증분값, 총수량
    private fun sendCountData(count:String, inc_count:Int, sum_count:Int, runtime: String) {
        if (AppGlobal.instance.get_server_ip()=="") return

        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") return

        var db = DBHelperForDesign(this)
        val row = db.get(work_idx)
//        val actual = row!!["actual"].toString().toInt()
        val seq = row!!["seq"].toString().toInt()


        // 서버에서 새로운 데이터를 요청해서 생겨난 로직 (쓸데없이 시간 지연됨)
        // 2019-10-11
        var count_actual = 0                                // 총 Actual
        var count_target = 0                                // 총 타겟

        var db_list = db.gets()
        for (i in 0..((db_list?.size ?: 1) - 1)) {
            val item = db_list?.get(i)
            val actual2 = item?.get("actual").toString().toInt()
            val target2 = item?.get("target").toString().toInt()
            count_actual += actual2
            count_target += target2
        }

        val count_defective = db.sum_defective_count()      // 현재 디펙티브 값

        // Downtime
        var down_time = 0
        var down_target = 0

        var work_time = 0

        var shift_idx = AppGlobal.instance.get_current_shift_idx()

        if (shift_idx == "") {
            shift_idx = "0"
        } else {
            val shift_time = AppGlobal.instance.get_current_shift_time()

            if (shift_time != null) {
                val now = DateTime()
                val now_millis = now.millis

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

                // 현재까지의 작업시간
                work_time = ((now_millis - shift_stime_millis) / 1000).toInt() - planned1_time - planned2_time

                // Downtime
                val down_db = DBHelperForDownTime(this)
                val down_list = down_db.gets()
                down_list?.forEach { item ->
                    down_time += item["real_millis"].toString().toInt()
                    down_target += item["target"].toString().toInt()
                }

                // ctO 구하기 (현시점까지 작업시간 - 다운타임 시간)의 타겟
            }
        }
        // 서버에서 새로운 데이터를 요청해서 생겨난 로직 끝.
        // 2019-10-11


        // Cutting 과는 다르게 콤포넌트가 필수 선택사항이 아니므로
        // 선택되었을 경우에만 seq 값을 구하고 아니면, 디폴트 1을 전송한다.

        // 구서버용
//        val uri = "/senddata1.php"
//        var params = listOf(
//            "mac_addr" to AppGlobal.instance.getMACAddress(),
//            "didx" to AppGlobal.instance.get_design_info_idx(),
//            "count" to inc_count.toString(),
//            "total_count" to sum_count,
//            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
//            "factory_idx" to AppGlobal.instance.get_room_idx(),
//            "line_idx" to AppGlobal.instance.get_line_idx(),
//            "shift_idx" to  shift_idx,
//            "seq" to seq,
//            "max_rpm" to "",
//            "avr_rpm" to "")

        val now_millis = DateTime().millis
        val prepare_time = if (_prepare_time == 0L) 0L else (now_millis - _prepare_time)
        _prepare_time = now_millis


        // 신서버용
        // runtime : downtime 을 뺀 근무시간
        // actualO : 현 시프트의 총 Target
        // ct0 : 퍼포먼스 계산할 때 타겟 값 (현시점까지 작업시간 - 다운타임 시간)의 타겟
        // "actualO" to count_target.toString(),
        val uri = "/Scount.php"
        var params = listOf(
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "count" to inc_count.toString(),
            "total_count" to sum_count,
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  shift_idx,
            "seq" to seq,
            "runtime" to (work_time-down_time).toString(),
            "stitching_count" to AppGlobal.instance.get_stitch(),
            "curing_sec" to runtime,
            "prepare_time" to prepare_time.toString(),
            "actualO" to sum_count.toString(),
            "ctO" to (count_target-down_target).toString(),
            "defective" to count_defective.toString(),
            "worker" to AppGlobal.instance.get_worker_no())

//Log.e("Scount params", params.toString())

        request(this, uri, true,false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            Log.e("Scount result", "= "+msg.toString())
            if(code != "00") {
                ToastOut(this, result.getString("msg"), true)
            }
        })

        // 베트남 특별한 경우
        // 무조건 보내는 걸로 변경
//        if (AppGlobal.instance.get_send_stitch_count()) {

            if (runtime=="") {
                ToastOut(this, R.string.msg_runtime_not_enterd, true)
                return
            } else {
                val sensing_id = DateTime.now().millis
                val uri = "/Hcount.php"
                var params = listOf(
                    "sensing_id" to sensing_id,
                    "machine_no" to AppGlobal.instance.get_mc_no1(),
                    "stitching_count" to AppGlobal.instance.get_stitch(),
                    "curing_sec" to runtime,
                    "prepare_time" to prepare_time.toString(),
                    "piece_yn" to "1",
                    "factory_cd" to AppGlobal.instance.get_factory_idx(),
                    "line_cd" to AppGlobal.instance.get_line_idx(),
                    "factory_nm" to AppGlobal.instance.get_factory(),
                    "line_nm" to AppGlobal.instance.get_line()
                )
                Toast.makeText(this, "sensing_id="+sensing_id+", curing_sec="+runtime, Toast.LENGTH_SHORT).show()
                // AppGlobal.instance.get_mc_no1()

                Log.e("Hcount params", "= " + params.toString())

                request(this, uri, true, false, params, { result ->
                    var code = result.getString("code")
                    if (code != "00") {
                        ToastOut(this, result.getString("msg"), true)
                    }
                })
            }
//        }
    }

//    fun startComponent(wosno:String, styleno:String, model:String, size:String, target:String, actual:String) {
//
//        var db = DBHelperForDesign(this)
//
//        val work_info = AppGlobal.instance.get_current_shift_time()
//        val shift_idx = work_info?.getString("shift_idx") ?: ""
//        val shift_name = work_info?.getString("shift_name") ?: ""
//
//        val row = db.get(wosno, size)
//
//        if (row == null) {
//            val s = db.counts_for_didx()
//            val seq = s + 1
//
//            db.add(wosno, shift_idx, shift_name, styleno, model, size, target.toInt(), 0, 0, seq)
//            val row2 = db.get(wosno, size)
//            if (row2 == null) {
//                Log.e("work_idx", "none")
//                AppGlobal.instance.set_work_idx("")
//            } else {
//                AppGlobal.instance.set_work_idx(row2["work_idx"].toString())
//                Log.e("work_idx", row2["work_idx"].toString())
//            }
//        } else {
//            AppGlobal.instance.set_work_idx(row["work_idx"].toString())
//            Log.e("work_idx", row["work_idx"].toString())
//        }
//        val br_intent = Intent("need.refresh")
//        this.sendBroadcast(br_intent)
//
//        // 작업시작할때 현재 쉬프트의 날짜를 기록해놓음
//        val current = AppGlobal.instance.get_current_work_time()
//        if (current.length() > 0) {
//            val shift = current.getJSONObject(0)
//            var shift_stime = OEEUtil.parseDateTime(shift["work_stime"].toString())
//            AppGlobal.instance.set_current_work_day(shift_stime.toString("yyyy-MM-dd"))
//        }
//
//        // downtime sec 초기화
//        // 새로 선택한 상품이 있으므로 이 값을 초기화 한다. 기존에 없던 부분
//        _last_count_received_time = DateTime()
//
//        // 현재 shift의 첫생산인데 지각인경우 downtime 처리
//    }

    fun startNewProduct(didx:String, cycle_time:Int, model:String, article:String, stitch:String, material_way:String, component:String) {

        // 이전 작업과 동일한 디자인 번호이면 새작업이 아님
        val prev_didx = AppGlobal.instance.get_design_info_idx()
        val prev_work_idx = "" + AppGlobal.instance.get_product_idx()

        var start_dt = DateTime().toString("yyyy-MM-dd HH:mm:ss")       // 새 디자인 시작시간

        AppGlobal.instance.set_design_info_idx(didx)
        AppGlobal.instance.set_model(model)
        AppGlobal.instance.set_article(article)
        AppGlobal.instance.set_stitch(stitch)
        AppGlobal.instance.set_material_way(material_way)
        AppGlobal.instance.set_component(component)
        AppGlobal.instance.set_cycle_time(cycle_time)

        val pieces_info = AppGlobal.instance.get_pieces_info()
        val pairs_info = AppGlobal.instance.get_pairs_info()

        // 서버에서 받은 다운타임 타입이 초단위가 아니고 "Cycle Time" 이면 선택된 디자인의 Cycle Time 으로 세팅된다.
        val target_type = AppGlobal.instance.get_target_type()
        val downtime_type = AppGlobal.instance.get_downtime_type()

        if (downtime_type=="Cycle Time") {
            AppGlobal.instance.set_downtime_sec(cycle_time.toString())
            OEEUtil.LogWrite(downtime_type + " = " + cycle_time.toString(), "Reset Downtime Sec")
        }

        val db = DBHelperForDesign(this)
        val item = db.get(prev_work_idx)

        if (didx == prev_didx) {
            if (item != null) {
                val work_info = AppGlobal.instance.get_current_shift_time()
                val shift_idx = work_info?.getString("shift_idx") ?: ""
                val shift_name = work_info?.getString("shift_name") ?: ""

                OEEUtil.LogWrite("work_idx=" + prev_work_idx + ", shift_idx="+shift_idx+", stitch="+stitch+", shift_name="+shift_name+", didx="+didx+", cycle_time="+cycle_time, "startNewProduct")

                db.updateDesignInfo(prev_work_idx, shift_idx, shift_name, cycle_time, pieces_info, pairs_info)
                return
            }
        } else {
            if (prev_work_idx != "") db.updateWorkEnd(prev_work_idx)    // 이전 작업 완료 처리
        }

        // 이전 디자인의 Actual이 0이면 (작업이 하나도 없는 경우, 실수로 선택한 경우 등)
        // 해당 디자인을 지우고 시작 시간을 새 디자인의 시작 시간으로 업데이트한다.
        if (item != null) {
            val actual_cnt = item!!["actual"].toString().toInt()
            if (actual_cnt == 0) {
                start_dt = item!!["start_dt"].toString()        // 시작 시간을 이전 디자인의 시작 시간으로 재설정
                db.deleteWorkIdx(prev_work_idx)                 // 이전 디자인 삭제

                // Downtime 재계산
                val down_db = DBHelperForDownTime(this)
                val down_list = down_db.gets()

                // From Server / From Device 에서 활용
                val one_item_sec = AppGlobal.instance.get_current_maketime_per_piece()

                for (i in 0..((down_list?.size ?: 1) - 1)) {
                    val item = down_list?.get(i)
                    val item_real_millis = item?.get("real_millis").toString().toInt()

                    val start_dt_millis = OEEUtil.parseDateTime(start_dt).millis
                    val item_start_dt_millis = OEEUtil.parseDateTime(item?.get("start_dt").toString()).millis

                    if (item_start_dt_millis >= start_dt_millis) {
                        val item_idx = item?.get("idx").toString()
                        if (item_real_millis > 0) {
                            if (target_type.substring(0, 6) == "cycle_") {
                                if (cycle_time != 0) {
                                    val new_target = item_real_millis / cycle_time
                                    down_db.updateDidxTarget(item_idx, didx, new_target)
                                } else {
                                    down_db.updateDidxTarget(item_idx, didx, 0)
                                }
                            } else {
                                if (one_item_sec != 0F) {
                                    val new_target = item_real_millis / one_item_sec
                                    down_db.updateDidxTarget(item_idx, didx, new_target.toInt())
                                } else {
                                    down_db.updateDidxTarget(item_idx, didx, 0)
                                }
                            }
                        } else {
                            down_db.updateDidxTarget(item_idx, didx, 0)
                        }
                    }
                }
            }
        }

        AppGlobal.instance.set_product_idx()

        val max_seq = db.max_seq()
        val seq = max_seq + 1
        Log.e("test", "seq = " + seq)

        // 처음 시작이면 Start 시간을 Shift 시작 시간으로 세팅
        if (seq == 1) {
            val shift_time = AppGlobal.instance.get_current_shift_time()
            if (shift_time != null) {
                start_dt = shift_time["work_stime"].toString()
            }
        }

        val work_idx = "" + AppGlobal.instance.get_product_idx()

        val now = DateTime().millis
        val work_info = AppGlobal.instance.get_current_shift_time()
        var shift_idx = work_info?.getString("shift_idx") ?: ""
        var shift_name = work_info?.getString("shift_name") ?: ""

        if (work_info == null) {
            // 현재 시프트가 없으므로 다가올 시프트 정보를 구한다.
            val list = AppGlobal.instance.get_current_work_time()
            for (i in 0..(list.length() - 1)) {
                val work_time = list.getJSONObject(i)
                var shift_stime = (OEEUtil.parseDateTime(work_time["work_stime"].toString())).millis

                if (now <= shift_stime) {
                    shift_idx = work_time?.getString("shift_idx") ?: ""
                    shift_name = work_time?.getString("shift_name") ?: ""
                    break
                }
            }
        }

        OEEUtil.LogWrite("work_idx=" + work_idx + ", shift_idx="+shift_idx+", stitch="+stitch+", shift_name="+shift_name+", didx="+didx+", cycle_time="+cycle_time, "startNewProduct")

        db.add(work_idx, start_dt, didx, shift_idx, shift_name, cycle_time, pieces_info, pairs_info,0, 0, 0, seq)

//        val br_intent = Intent("need.refresh")
//        this.sendBroadcast(br_intent)
    }

    // downtime 발생시 푸시 발송
    fun sendPush(push_text: String, add_text: String = "", progress: Boolean=false) {
        val uri = "/pushcall.php"
        var params = listOf(
            "code" to "push_text_list",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
            "machine_no" to AppGlobal.instance.get_mc_no1(),
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

    private fun sendStartDownTime(dt: DateTime) {

        if (AppGlobal.instance.get_server_ip() == "") return

        val work_idx = "" + AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        if (_is_call) return
        _is_call = true
/*
        var db = SimpleDatabaseHelper(this)
        val row = db.get(work_idx)
        val seq = row!!["seq"].toString().toInt() + 1
*/
        var down_db = DBHelperForDownTime(this)
        val count = down_db.counts_for_notcompleted()
        if (count > 0) return

        val list = down_db.gets()

        val uri = "/downtimedata.php"
        var params = listOf(
            "code" to "start",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "sdate" to dt.toString("yyyy-MM-dd"),
            "stime" to dt.toString("HH:mm:ss"),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
            "seq" to (list?.size ?: 0) + 1)

        request(this, uri, true,false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var idx = result.getString("idx")
                AppGlobal.instance.set_downtime_idx(idx)

                val didx = AppGlobal.instance.get_design_info_idx()
                val work_info = AppGlobal.instance.get_current_shift_time()
                val shift_idx = work_info?.getString("shift_idx") ?: ""
                val shift_name = work_info?.getString("shift_name") ?: ""

                val start_dt = dt.toString("yyyy-MM-dd HH:mm:ss")

                // 같은 시간대가 저장되어 있는지 검사
                val cnt = down_db.count_start_dt(start_dt)

                if (cnt <= 0) {
                    down_db.add(idx, work_idx, didx, shift_idx, shift_name, start_dt)

//                startDowntimeActivity()
                    startDowntimeInputActivity(idx, start_dt)

                    sendPush("SYS: DOWNTIME")
                }

            } else {
                ToastOut(this, result.getString("msg"), true)
            }
            _is_call = false
        },{
            _is_call = false
        })
    }

    private fun sendEndDownTimeForce() {
        if (AppGlobal.instance.get_server_ip() == "") return
        if (AppGlobal.instance.get_downtime_idx() == "") return

        var db = DBHelperForDownTime(this)

        val idx = AppGlobal.instance.get_downtime_idx()
        if (idx == "") return
        val item = db.get(idx)
        if (item == null || item.size() == 0) return

        val now = DateTime()
        val now_millis = now.millis
        val down_start_millis = OEEUtil.parseDateTime(item["start_dt"].toString()).millis

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

        val ct = AppGlobal.instance.get_cycle_time()
        val target = if (ct > 0) real_down_time / ct else 0


        val downtime = "5"
        val uri = "/downtimedata.php"
        var params = listOf(
            "code" to "end",
            "idx" to AppGlobal.instance.get_downtime_idx(),
            "downtime" to downtime,
            "edate" to now.toString("yyyy-MM-dd"),
            "etime" to now.toString("HH:mm:ss"))

        request(this, uri, true,false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                val idx = AppGlobal.instance.get_downtime_idx()
                AppGlobal.instance.set_downtime_idx("")

                db.updateEnd(idx, "ignored", now.toString("yyyy-MM-dd HH:mm:ss"), down_time, real_down_time, target)

                // 기존 다운타임 화면이 열려있으면 닫고
                val br_intent = Intent("start.downtime")
                this.sendBroadcast(br_intent)

                // 카운트뷰로 이동
//                if (vp_fragments.currentItem != 1) changeFragment(1)

            } else if (code == "99") {
                // ?
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
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
        if (_is_down_loop) return
        _is_down_loop = true

        var db = DBHelperForDownTime(this)
        val count = db.counts_for_notcompleted()
        if (count > 0) {
            AppGlobal.instance.set_last_received(DateTime().toString("yyyy-MM-dd HH:mm:ss"))
            _is_down_loop = false
            return
        }

        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") {
            _is_down_loop = false
            return
        }

        val downtime_time = AppGlobal.instance.get_downtime_sec()   // downtime 지정시간
        if (downtime_time == "") {
            ToastOut(this, R.string.msg_no_downtime)
            _is_down_loop = false
            return
        }
        val downtime_time_sec = downtime_time.toInt()

        val item = AppGlobal.instance.get_current_shift_time()
        if (item == null) {
            _is_down_loop = false
            return
        }

        val work_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
        val work_stime_millis = work_stime.millis
        val work_etime_millis = OEEUtil.parseDateTime(item["work_etime"].toString()).millis
        val planned1_stime_millis = OEEUtil.parseDateTime(item["planned1_stime_dt"].toString()).millis
        val planned1_etime_millis = OEEUtil.parseDateTime(item["planned1_etime_dt"].toString()).millis
        val planned2_stime_millis = OEEUtil.parseDateTime(item["planned2_stime_dt"].toString()).millis
        val planned2_etime_millis = OEEUtil.parseDateTime(item["planned2_etime_dt"].toString()).millis

        val now = DateTime()
        val now_millis = now.millis

        var last_received_time = work_stime    // downtime 값이 "" 이면 처음이므로 Shift 시작 시간으로 저장

        var chk = AppGlobal.instance.get_last_received()
        if (chk != "") {
            if (OEEUtil.parseDateTime(chk).millis < work_stime_millis) {    // downtime 시작 시간이 Shift의 시작 시간보다 작다면 초기화
                chk = item["work_stime"].toString()
                AppGlobal.instance.set_last_received(chk)
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

                // 지정된 downtime 이 지났으면 downtime을 발생시킨다.
                if (cur_down_time > downtime_time_sec) {
                    sendStartDownTime(last_received_time)
                }
            }

        } else {
            // 워크 타임이 아니면 downtime 시작 시간 초기화
            AppGlobal.instance.set_last_received("")
        }

        // 위의 로직으로 변경됨 (다운타임 시간에서 휴식 시간을 빼고 계산)
        // 워크 타임안에 있으면서 휴식 시간이 아니고,
        // 지정된 downtime 이 지났으면 downtime을 발생시킨다.
//        if (work_stime.millis < now_millis && work_etime.millis > now_millis &&
//            !(planned1_stime_dt.millis < now_millis && planned1_etime_dt.millis > now_millis ) &&
//            !(planned2_stime_dt.millis < now_millis && planned2_etime_dt.millis > now_millis ) &&
//            downtime_time_sec > 0 && now_millis - last_received_time.millis > downtime_time_sec * 1000) {
//            sendStartDownTime(last_received_time)
//        }

        // 위의 로직으로 변경됨 (다운타임 시간에서 휴식 시간을 빼고 계산)
        // 워크 타임이 아니면 downtime 시작 시간을 현재 시간으로 초기화
//        if (work_stime.millis > now_millis || work_etime.millis < now_millis) {
//            AppGlobal.instance.set_last_received("")
//            // downtime 시간 초기화 하기 전에 "" 값이면 초기화를 하지 않는다. (Shift 시작시 지각인지 체크하기 위함)
//            if (AppGlobal.instance.get_last_received() != "") {
//                AppGlobal.instance.set_last_received("")
//            }
//        }

        // 휴식 시간이면 downtime 시작 시간을 현재 시간으로 초기화
        // 휴식 동안에도 초기화 안하기로 결정. 2019-09-19
//        if ((planned1_stime_dt.millis < now_millis && planned1_etime_dt.millis > now_millis ) ||
//            (planned2_stime_dt.millis < now_millis && planned2_etime_dt.millis > now_millis )) {
//            // downtime 시간 초기화 하기 전에 "" 값이면 초기화를 하지 않는다.
//            // 처음 Shift 시작시 지각인지 체크하기 위함.
//            if (AppGlobal.instance.get_last_received() != "") {
//                AppGlobal.instance.set_last_received(now.toString("yyyy-MM-dd HH:mm:ss"))
////            Log.e("downtime chk", "planned time")
//            }
//        }
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

    private fun startDowntimeInputActivity(idx: String = "", start_dt: String = "") {
//        val br_intent = Intent("start.downtime")
//        this.sendBroadcast(br_intent)
        if (idx == "" || start_dt == "") return

        val intent = Intent(this, DownTimeInputActivity::class.java)
        intent.putExtra("idx", idx)
        intent.putExtra("start_dt", start_dt)
        startActivity(intent)
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