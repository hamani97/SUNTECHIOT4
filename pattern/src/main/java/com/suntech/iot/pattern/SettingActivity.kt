package com.suntech.iot.pattern

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.layout_setting_etc.*
import kotlinx.android.synthetic.main.layout_setting_system.*
import kotlinx.android.synthetic.main.layout_setting_target.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*


class SettingActivity : BaseActivity() {

    private var usb_state = false

    private var tab_pos: Int = 1
    private var _selected_target_type: String = "cycle_"
    private var _selected_blink_color: String = AppGlobal.instance.get_blink_color()

    private var _selected_trim_pair: String = ""

    private var _selected_factory_idx: String = ""
    private var _selected_room_idx: String = ""
    private var _selected_line_idx: String = ""
    private var _selected_mc_no_idx: String = ""
    private var _selected_mc_model_idx: String = ""

    private var _server_time = -99999999L

    private var is_loop = true

    // Update 정보
//    private var device_version = ""
//    private var latest_version = ""
//    private var latest_url = ""
//    private var latest_path = ""
//    private var latest_file = ""

    val _broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.getAction()
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false))
                    btn_wifi_state.isSelected = true
                else
                    btn_wifi_state.isSelected = false

            } else if (action.equals(Intent.ACTION_TIME_CHANGED)) {
                //

            } else if (action.equals("need.refresh.server.state")) {
                val state = intent.getStringExtra("state")
                if (state == "Y") {
                    btn_server_state.isSelected = true
                } else btn_server_state.isSelected = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initView()
    }

    fun parentSpaceClick(view: View) {
        var v = this.currentFocus
        if (v != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    public override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
        registerReceiver(_broadcastReceiver, filter)
        is_loop = true
        startHandler()
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(_broadcastReceiver)
        is_loop = false
    }

    fun startHandler() {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                checkUSB()
                if (tab_pos == 3) updateView()
                startHandler()
            }
        }, 1000)
    }

    private fun checkUSB() {
        if (usb_state != AppGlobal.instance.get_usb_connect()) {
            usb_state = AppGlobal.instance.get_usb_connect()
            btn_usb_state2.isSelected = usb_state
        }
    }

    private fun updateView() {
        val now = DateTime.now()
        tv_setting_time?.text = now.toString("yyyy-MM-dd HH:mm:ss")
        if (_server_time == -99999999L) {
            tv_setting_server_time?.text = "Failed to get server time"
        } else if (_server_time == 0L) {
            tv_setting_server_time?.text = tv_setting_time?.text
        } else {
            val dt = now.millis + _server_time
            tv_setting_server_time?.text = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").print(dt)
        }
    }

    private fun initView() {

        tv_title.setText(R.string.title_setting)

        // system setting
        // set hidden value
        _selected_factory_idx = AppGlobal.instance.get_factory_idx()
        _selected_room_idx = AppGlobal.instance.get_zone_idx()
        _selected_line_idx = AppGlobal.instance.get_line_idx()
        _selected_mc_no_idx = AppGlobal.instance.get_mc_no_idx()
        _selected_mc_model_idx = AppGlobal.instance.get_mc_model_idx()

        // widget
        tv_setting_wifi.text = AppGlobal.instance.getWiFiSSID(this)
        tv_setting_ip.text = AppGlobal.instance.get_local_ip()
        tv_setting_mac.text = AppGlobal.instance.getMACAddress()
        tv_setting_factory.text = AppGlobal.instance.get_factory()
        tv_setting_room.text = AppGlobal.instance.get_zone()
        tv_setting_line.text = AppGlobal.instance.get_line()
        tv_setting_mc_model.text = AppGlobal.instance.get_mc_model()
        tv_setting_mc_no1.setText(AppGlobal.instance.get_mc_no())
        et_setting_mc_serial.setText(AppGlobal.instance.get_mc_serial())

        et_setting_server_ip?.setText(AppGlobal.instance.get_server_ip())
        et_setting_port?.setText(AppGlobal.instance.get_server_port())

        // 워크시트 토글 시간(초). 0일때는 5로 초기화
        val sec = if (AppGlobal.instance.get_worksheet_display_time()==0) "10" else AppGlobal.instance.get_worksheet_display_time().toString()
        et_setting_worksheet_display_time?.setText(sec)

        et_setting_sop_name?.setText(AppGlobal.instance.get_sop_name())

        sw_long_touch.isChecked = AppGlobal.instance.get_long_touch()
        sw_message_enable.isChecked = AppGlobal.instance.get_message_enable()
        sw_sound_at_count.isChecked = AppGlobal.instance.get_sound_at_count()
//        sw_without_component.isChecked = AppGlobal.instance.get_without_component()
        sw_screen_blink_effect.isChecked = AppGlobal.instance.get_screen_blink()
//        sw_send_stitch_count.isChecked = AppGlobal.instance.get_send_stitch_count()
        sw_planned_count_process.isChecked = AppGlobal.instance.get_planned_count_process()
        sw_target_stop_when_downtime.isChecked = AppGlobal.instance.get_target_stop_when_downtime()
        sw_ask_when_clicking_defective.isChecked = AppGlobal.instance.get_ask_when_clicking_defective()
        sw_target_by_group.isChecked = AppGlobal.instance.get_target_by_group()
        sw_reverse_downtime_check.isChecked = AppGlobal.instance.get_reverse_downtime_check()

        val start_target = AppGlobal.instance.get_start_at_target()
        if (start_target==0) sw_start_at_target_1.isChecked = false
        else sw_start_at_target_1.isChecked = true

        // 깜박임 기능. 0일때는 10으로 초기화
//        val remain = if (AppGlobal.instance.get_remain_number()==0) "10" else AppGlobal.instance.get_remain_number().toString()
//        et_remain_number.setText(remain)

        if (_selected_blink_color == "") _selected_blink_color = "f8ad13"
        blinkColorChange(_selected_blink_color)

        blink_color_f8ad13.setOnClickListener {
            blinkColorChange("f8ad13")
        }
        blink_color_ff0000.setOnClickListener {
            blinkColorChange("ff0000")
        }
        blink_color_0079BA.setOnClickListener {
            blinkColorChange("0079BA")
        }
        blink_color_888888.setOnClickListener {
            blinkColorChange("888888")
        }

//        tv_trim_qty.setText(AppGlobal.instance.get_trim_qty())
//        tv_trim_pairs.setText(AppGlobal.instance.get_trim_pairs())

//        tv_trim_pairs.setOnClickListener { selectTrimPair() }

        // target setting
        if (AppGlobal.instance.get_target_type() == "") targetTypeChange("cycle_per_accumulate")
        else targetTypeChange(AppGlobal.instance.get_target_type())

        tv_shift_1.setText(AppGlobal.instance.get_target_manual_shift("1"))
        tv_shift_2.setText(AppGlobal.instance.get_target_manual_shift("2"))
        tv_shift_3.setText(AppGlobal.instance.get_target_manual_shift("3"))


        // click listener
        // Tab button
        btn_setting_system.setOnClickListener { tabChange(1) }
        btn_setting_target.setOnClickListener { tabChange(2) }
        btn_setting_etc.setOnClickListener { tabChange(3) }

        // System setting button listener
        tv_setting_factory.setOnClickListener { fetchDataForFactory() }
        tv_setting_room.setOnClickListener { fetchDataForRoom() }
        tv_setting_line.setOnClickListener { fetchDataForLine() }
        tv_setting_mc_model.setOnClickListener { fetchDataForMCModel() }

        // Target setting button listener
        img_cycle_time_accumulate.setOnClickListener { targetTypeChange("cycle_per_accumulate") }
        tv_cycle_time_accumulate.setOnClickListener { targetTypeChange("cycle_per_accumulate") }
        img_cycle_time_shifttotal.setOnClickListener { targetTypeChange("cycle_per_day_total") }
        tv_cycle_time_shifttotal.setOnClickListener { targetTypeChange("cycle_per_day_total") }

        img_server_accumulate.setOnClickListener { targetTypeChange("server_per_accumulate") }
        tv_server_accumulate.setOnClickListener { targetTypeChange("server_per_accumulate") }
        img_server_shifttotal.setOnClickListener { targetTypeChange("server_per_day_total") }
        tv_server_shifttotal.setOnClickListener { targetTypeChange("server_per_day_total") }

        img_device_accumulate.setOnClickListener { targetTypeChange("device_per_accumulate") }
        tv_device_accumulate.setOnClickListener { targetTypeChange("device_per_accumulate") }
        img_device_shifttotal.setOnClickListener { targetTypeChange("device_per_day_total") }
        tv_device_shifttotal.setOnClickListener { targetTypeChange("device_per_day_total") }

//        btn_server_accumulate.setOnClickListener { targetTypeChange("server_per_accumulate") }
//        btn_server_shifttotal.setOnClickListener { targetTypeChange("server_per_day_total") }
//        btn_manual_accumulate.setOnClickListener { targetTypeChange("device_per_accumulate") }
//        btn_manual_shifttotal.setOnClickListener { targetTypeChange("device_per_day_total") }

        // check server button
        btn_setting_check_server.setOnClickListener {
            checkServer()
            var new_ip = et_setting_server_ip.text.toString()
            var old_ip = AppGlobal.instance.get_server_ip()
            if (!new_ip.equals(old_ip)) {
                tv_setting_factory.text = ""
                tv_setting_room.text = ""
                tv_setting_line.text = ""
                tv_setting_mc_model.text = ""
            }
        }

        // Update button click
        btn_version_update.setOnClickListener {
            ToastOut(this, "준비중인 서비스 입니다.", true)
//            startActivity(Intent(this, DownloadApkFile::class.java))
        }

        // Save button click
        btn_setting_confirm.setOnClickListener {
            saveSettingData()
        }

        // Cancel button click
        btn_setting_cancel.setOnClickListener {
//            AppGlobal.instance.set_auto_setting(false)
            finish()
        }

        btn_wifi_state.isSelected = AppGlobal.instance.isOnline(this)
        btn_server_state.isSelected = AppGlobal.instance.get_server_connect()

        // TODO: TEST
        // 10.10.10.90
        // 49.247.205.235
        // 115.68.227.31
        // 183.81.156.206 : inni
        // 36.66.169.221 (8124)
        if (et_setting_server_ip.text.toString() == "") et_setting_server_ip.setText("10.10.10.90")
        if (et_setting_port.text.toString() == "") et_setting_port.setText("80")

//        device_version = packageManager.getPackageInfo(packageName, 0).versionName
//
//        tv_device_version?.text = device_version

        fetchServerTime()
//        fetchLatestVersion()
    }

    private fun checkServer() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/ping.php"
        request(this, url, port, uri, false, false,false, null, { result ->
            var code = result.getString("code")
            ToastOut(this, result.getString("msg"), true)
            if (code == "00") {
                btn_server_state.isSelected = true
            } else {
                btn_server_state.isSelected = false
            }
        }, {
            btn_server_state.isSelected = false
            ToastOut(this, R.string.msg_connection_fail, true)
        })
    }

    private fun fetchServerTime() {

        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        val params = listOf("code" to "current_time")
        request(this, url, port, uri, false, false,false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                val curdatetime = result.getString("curdatetime")?: ""
                if (curdatetime != "") {
                    val server_dt = OEEUtil.parseDateTime(curdatetime).millis
                    val device_dt = DateTime.now().millis
                    _server_time = server_dt - device_dt

                    Log.e("Time","Distance Time ================> " + _server_time)
                }
            }
        }, {
            ToastOut(this, R.string.msg_connection_fail, true)
        })
    }

//    private fun fetchLatestVersion() {
//
//        val url = "http://"+ et_setting_server_ip.text.toString()
//        val port = et_setting_port.text.toString()
//        val uri = "/version.php"
//        val params = listOf("code" to "pattern_hwaseung")
//        request(this, url, port, uri, false, false,false, params, { result ->
//            val code = result.getString("code")
//            if (code == "00") {
//                latest_version = result.getString("version")?: ""
//                latest_url = result.getString("url")?: ""
//                latest_path = result.getString("path")?: ""
//                latest_file = result.getString("file")?: ""
//
//                tv_server_version?.text = latest_version
//
//                val device_code = device_version.replace(".", "").toInt()
//                val server_code = latest_version.replace(".", "").toInt()
//
//                btn_version_update?.visibility =
//                    if (device_code < server_code) View.VISIBLE else View.GONE
//
//            }
//        }, {
//            ToastOut(this, R.string.msg_connection_fail)
//        })
//    }

    private fun saveSettingData() {
        // check value
        if (_selected_factory_idx == "" || _selected_room_idx == "" || _selected_line_idx == "" || tv_setting_mac.text.toString().trim() == "") {
            tabChange(1)
            ToastOut(this, R.string.msg_require_info, true)
            return
        }
        if (tv_shift_1.text.toString().trim()=="") {
            tv_shift_1.setText("0")
        }
        if (tv_shift_2.text.toString().trim()=="") {
            tv_shift_2.setText("0")
        }
        if (tv_shift_3.text.toString().trim()=="") {
            tv_shift_3.setText("0")
        }
//        if (_selected_target_type.substring(0, 6) == "device") {
//            if (tv_shift_1.text.toString().trim()=="" || tv_shift_2.text.toString().trim()=="" || tv_shift_3.text.toString().trim()=="") {
//                tabChange(2)
//                ToastOut(this, R.string.msg_require_target_quantity, true)
//                return
//            }
//        }
        // 이 옵션이 켜져있으면 mc no를 숫자만 입력해야 한다.
//        if (sw_send_stitch_count.isChecked) {
//            val mc_no1 = tv_setting_mc_no1.text.toString()
//            val regex = Regex("""\d+""")
//            if (!regex.matches(mc_no1)) {
//                tabChange(1)
//                ToastOut(this, R.string.msg_enter_only_for_mc_no, true)
//                return
//            }
//        }

        val worksheet_time = if (et_setting_worksheet_display_time.text.toString()=="") 10 else et_setting_worksheet_display_time.text.toString().toInt()

        // setting value
        AppGlobal.instance.set_factory_idx(_selected_factory_idx)
        AppGlobal.instance.set_zone_idx(_selected_room_idx)
        AppGlobal.instance.set_line_idx(_selected_line_idx)
        AppGlobal.instance.set_mc_no_idx(_selected_mc_no_idx)
        AppGlobal.instance.set_mc_model_idx(_selected_mc_model_idx)

        AppGlobal.instance.set_factory(tv_setting_factory.text.toString())
        AppGlobal.instance.set_zone(tv_setting_room.text.toString())
        AppGlobal.instance.set_line(tv_setting_line.text.toString())
        AppGlobal.instance.set_mc_model(tv_setting_mc_model.text.toString())
        AppGlobal.instance.set_mc_no(tv_setting_mc_no1.text.toString())
        AppGlobal.instance.set_mc_serial(et_setting_mc_serial.text.toString())

        AppGlobal.instance.set_server_ip(et_setting_server_ip.text.toString())
        AppGlobal.instance.set_server_port(et_setting_port.text.toString())
        AppGlobal.instance.set_long_touch(sw_long_touch.isChecked)
        AppGlobal.instance.set_message_enable(sw_message_enable.isChecked)
        AppGlobal.instance.set_sound_at_count(sw_sound_at_count.isChecked)
//        AppGlobal.instance.set_without_component(sw_without_component.isChecked)

        if (sw_start_at_target_1.isChecked) AppGlobal.instance.set_start_at_target(1)
        else AppGlobal.instance.set_start_at_target(0)

        AppGlobal.instance.set_worksheet_display_time(worksheet_time)
        AppGlobal.instance.set_sop_name(et_setting_sop_name.text.toString())


        AppGlobal.instance.set_screen_blink(sw_screen_blink_effect.isChecked)
//        AppGlobal.instance.set_remain_number(remain_num)
        AppGlobal.instance.set_blink_color(_selected_blink_color)

//        AppGlobal.instance.set_send_stitch_count(sw_send_stitch_count.isChecked)
        AppGlobal.instance.set_planned_count_process(sw_planned_count_process.isChecked)
        AppGlobal.instance.set_target_stop_when_downtime(sw_target_stop_when_downtime.isChecked)
        AppGlobal.instance.set_ask_when_clicking_defective(sw_ask_when_clicking_defective.isChecked)
        AppGlobal.instance.set_target_by_group(sw_target_by_group.isChecked)
        AppGlobal.instance.set_reverse_downtime_check(sw_reverse_downtime_check.isChecked)

        // count setting
//        AppGlobal.instance.set_trim_qty(tv_trim_qty.text.toString())
//        AppGlobal.instance.set_trim_pairs(tv_trim_pairs.text.toString())

        // target type
        AppGlobal.instance.set_target_type(_selected_target_type)
        AppGlobal.instance.set_target_manual_shift("1", tv_shift_1.text.toString())
        AppGlobal.instance.set_target_manual_shift("2", tv_shift_2.text.toString())
        AppGlobal.instance.set_target_manual_shift("3", tv_shift_3.text.toString())

        // 장비 설정값 저장
        val uri = "/setting1.php"
        var params = listOf(
            "code" to "server",
            "factory_parent_idx" to _selected_factory_idx,
            "factory_idx" to _selected_room_idx,
            "line_idx" to _selected_line_idx,
            "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
            "mac_addr" to tv_setting_mac.text,
            "machine_no" to tv_setting_mc_no1.text.toString(),
            "ip_addr" to tv_setting_ip.text,
            "mc_model" to tv_setting_mc_model.text,
            "mc_serial" to et_setting_mc_serial.text.toString()
        )
        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            if(code == "00") {
                ToastOut(this, result.getString("msg"))
                sendAppStartTime()      // 앱 시작을 알림. 결과에 상관없이 종료
                finish()
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun sendAppStartTime() {
        val now = DateTime()
        val uri = "/setting1.php"
        var params = listOf(
            "code" to "time",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "start_time" to now.toString("yyyy-MM-dd HH:mm:ss"))
        request(this, uri, false, params, { result ->
            val code = result.getString("code")
        })
    }

//    private fun selectTrimPair() {
//        var arr: ArrayList<String> = arrayListOf<String>()
//        arr.add("1/8")
//        arr.add("1/4")
//        arr.add("1/2")
//        arr.add("1")
//
//        val intent = Intent(this, PopupSelectList::class.java)
//        intent.putStringArrayListExtra("list", arr)
//        startActivity(intent, { r, c, m, d ->
//            if (r) {
//                tv_trim_pairs.text = arr[c]
//                _selected_trim_pair = arr[c]
//            }
//        })
//    }

    private fun fetchDataForFactory() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf("code" to "factory_parent")

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            if (code == "00"){
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                        "idx" to item.getString("idx"),
                        "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_factory_idx = lists[c]["idx"] ?: ""
                        tv_setting_factory.text = lists[c]["name"] ?: ""
                        tv_setting_room.text = ""
                        tv_setting_line.text = ""
                    }
                })
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun fetchDataForRoom() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "factory",
            "factory_parent_idx" to _selected_factory_idx)

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map=hashMapOf(
                        "idx" to item.getString("idx"),
                        "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_room_idx = lists[c]["idx"] ?: ""
                        tv_setting_room.text = lists[c]["name"] ?: ""
                        tv_setting_line.text = ""
                    }
                })
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun fetchDataForLine() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "line",
            "factory_parent_idx" to _selected_factory_idx,
            "factory_idx" to _selected_room_idx)

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map=hashMapOf(
                        "idx" to item.getString("idx"),
                        "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_line_idx = lists[c]["idx"] ?: ""
                        tv_setting_line.text = lists[c]["name"] ?: ""
                    }
                })
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun fetchDataForMCModel() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf("code" to "machine_model")

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map=hashMapOf(
                        "idx" to item.getString("idx"),
                        "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_mc_model_idx = lists[c]["idx"] ?: ""
                        tv_setting_mc_model.text = lists[c]["name"] ?: ""
                    }
                })
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun tabChange(v : Int) {
        if (tab_pos == v) return
        tab_pos = v
        when (tab_pos) {
            1 -> {
                btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_system.setBackgroundResource(R.color.colorButtonBlue)
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_target.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_etc.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_etc.setBackgroundResource(R.color.colorButtonDefault)
                layout_setting_system.visibility = View.VISIBLE
                layout_setting_target.visibility = View.GONE
                layout_setting_etc.visibility = View.GONE
            }
            2 -> {
                btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_system.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_target.setBackgroundResource(R.color.colorButtonBlue)
                btn_setting_etc.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_etc.setBackgroundResource(R.color.colorButtonDefault)
                layout_setting_system.visibility = View.GONE
                layout_setting_target.visibility = View.VISIBLE
                layout_setting_etc.visibility = View.GONE
            }
            3 -> {
                btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_system.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_target.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_etc.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_etc.setBackgroundResource(R.color.colorButtonBlue)
                layout_setting_system.visibility = View.GONE
                layout_setting_target.visibility = View.GONE
                layout_setting_etc.visibility = View.VISIBLE
            }
        }
    }

    private fun targetTypeChange(v : String) {
        if (_selected_target_type == v) return
        when (_selected_target_type) {
            "cycle_per_accumulate" -> {
                img_cycle_time_accumulate.isSelected = false
                tv_cycle_time_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            }
            "cycle_per_day_total" -> {
                img_cycle_time_shifttotal.isSelected = false
                tv_cycle_time_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            }
            "server_per_accumulate" -> {
                img_server_accumulate.isSelected = false
                tv_server_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            }
            "server_per_day_total" -> {
                img_server_shifttotal.isSelected = false
                tv_server_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            }
            "device_per_accumulate" -> {
                img_device_accumulate.isSelected = false
                tv_device_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            }
            "device_per_day_total" -> {
                img_device_shifttotal.isSelected = false
                tv_device_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            }
        }
        when (_selected_target_type.substring(0, 6)) {
            "cycle_" -> tv_setting_target_type_cycle_time.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            "server" -> tv_setting_target_type_server.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            "device" -> tv_setting_target_type_manual.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
        }

        _selected_target_type = v

        // Device에 딸려있는 메뉴의 색상 변경
        if (_selected_target_type.substring(0, 6) != "device") {
            tv_target_per_shift.setTextColor(ContextCompat.getColor(this, R.color.colorDarkGray))
            tv_msg_target_per_shift.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            tv_label_shift1.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            tv_label_shift2.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            tv_label_shift3.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            tv_shift_1.setBackgroundResource(R.color.colorEditor1Readonly)
            tv_shift_2.setBackgroundResource(R.color.colorEditor1Readonly)
            tv_shift_3.setBackgroundResource(R.color.colorEditor1Readonly)
        } else {
            tv_target_per_shift.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            tv_msg_target_per_shift.setTextColor(ContextCompat.getColor(this, R.color.colorWhite2))
            tv_label_shift1.setTextColor(ContextCompat.getColor(this, R.color.colorWhite2))
            tv_label_shift2.setTextColor(ContextCompat.getColor(this, R.color.colorWhite2))
            tv_label_shift3.setTextColor(ContextCompat.getColor(this, R.color.colorWhite2))
            tv_shift_1.setBackgroundResource(R.color.colorEditor1)
            tv_shift_2.setBackgroundResource(R.color.colorEditor1)
            tv_shift_3.setBackgroundResource(R.color.colorEditor1)
        }

        when (_selected_target_type) {
            "cycle_per_accumulate" -> {
                img_cycle_time_accumulate.isSelected = true
                tv_cycle_time_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            }
            "cycle_per_day_total" -> {
                img_cycle_time_shifttotal.isSelected = true
                tv_cycle_time_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            }
            "server_per_accumulate" -> {
                img_server_accumulate.isSelected = true
                tv_server_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            }
            "server_per_day_total" -> {
                img_server_shifttotal.isSelected = true
                tv_server_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            }
            "device_per_accumulate" -> {
                img_device_accumulate.isSelected = true
                tv_device_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            }
            "device_per_day_total" -> {
                img_device_shifttotal.isSelected = true
                tv_device_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            }
        }
        when (_selected_target_type.substring(0, 6)) {
            "cycle_" -> tv_setting_target_type_cycle_time.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            "server" -> tv_setting_target_type_server.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            "device" -> tv_setting_target_type_manual.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
        }
    }

    private fun blinkColorChange(v : String) {
        when (_selected_blink_color) {
            "f8ad13" -> blink_color_f8ad13.text = ""
            "ff0000" -> blink_color_ff0000.text = ""
            "0079BA" -> blink_color_0079BA.text = ""
            "888888" -> blink_color_888888.text = ""
        }
        _selected_blink_color = v
        when (_selected_blink_color) {
            "f8ad13" -> blink_color_f8ad13.text = "V"
            "ff0000" -> blink_color_ff0000.text = "V"
            "0079BA" -> blink_color_0079BA.text = "V"
            "888888" -> blink_color_888888.text = "V"
        }
    }
}