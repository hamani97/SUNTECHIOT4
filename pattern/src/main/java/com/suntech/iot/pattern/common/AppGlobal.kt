package com.suntech.iot.pattern.common

import android.content.Context
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import com.suntech.iot.pattern.util.OEEUtil
import com.suntech.iot.pattern.util.UtilLocalStorage
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*

class AppGlobal private constructor() {

    private var _context : Context? = null

    private object Holder { val INSTANCE = AppGlobal() }

    companion object {
        val instance: AppGlobal by lazy { Holder.INSTANCE }
    }
    fun setContext(ctx : Context) { _context = ctx }

    // Default Setting
    fun set_server_ip(idx: String) { UtilLocalStorage.setString(instance._context!!, "server_ip", idx) }
    fun get_server_ip() : String { return UtilLocalStorage.getString(instance._context!!, "server_ip") }
    fun set_server_port(idx: String) { UtilLocalStorage.setString(instance._context!!, "server_port", idx) }
    fun get_server_port() : String { return UtilLocalStorage.getString(instance._context!!, "server_port") }

    fun set_factory_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "factory_idx", idx) }
    fun get_factory_idx() : String { return UtilLocalStorage.getString(instance._context!!, "factory_idx") }
    fun set_factory(idx: String) { UtilLocalStorage.setString(instance._context!!, "factory_name", idx) }
    fun get_factory() : String { return UtilLocalStorage.getString(instance._context!!, "factory_name") }

    fun set_zone_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "zone_idx", idx) }
    fun get_zone_idx() : String { return UtilLocalStorage.getString(instance._context!!, "zone_idx") }
    fun set_zone(idx: String) { UtilLocalStorage.setString(instance._context!!, "zone_name", idx) }
    fun get_zone() : String { return UtilLocalStorage.getString(instance._context!!, "zone_name") }

    fun set_line_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "line_idx", idx) }
    fun get_line_idx() : String { return UtilLocalStorage.getString(instance._context!!, "line_idx") }
    fun set_line(idx: String) { UtilLocalStorage.setString(instance._context!!, "line_name", idx) }
    fun get_line() : String { return UtilLocalStorage.getString(instance._context!!, "line_name") }

    fun set_mc_model_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "mc_model_idx", idx) }
    fun get_mc_model_idx() : String { return UtilLocalStorage.getString(instance._context!!, "mc_model_idx") }
    fun set_mc_model(idx: String) { UtilLocalStorage.setString(instance._context!!, "mc_model_name", idx) }
    fun get_mc_model() : String { return UtilLocalStorage.getString(instance._context!!, "mc_model_name") }

    fun set_mc_no_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "mc_no_idx", idx) }
    fun get_mc_no_idx() : String { return UtilLocalStorage.getString(instance._context!!, "mc_no_idx") }
    fun set_mc_no(idx: String) { UtilLocalStorage.setString(instance._context!!, "mc_no", idx) }
    fun get_mc_no() : String { return UtilLocalStorage.getString(instance._context!!, "mc_no") }

    fun set_mc_serial(idx: String) { UtilLocalStorage.setString(instance._context!!, "mc_serial", idx) }
    fun get_mc_serial() : String { return UtilLocalStorage.getString(instance._context!!, "mc_serial") }

    // 디바이스
    fun getMACAddress(): String? {
        var mac = ""
        try {
            mac = loadFileAsString("/sys/class/net/eth0/address").toUpperCase().substring(0, 17)
        } catch (e: IOException) {
            //e.printStackTrace()
        }
        if (mac == "") {
            mac = getMACAddress2()
            if (mac == "") mac = "NO_MAC_ADDRESS"
        }
        return mac
    }
    @Throws(java.io.IOException::class)
    fun loadFileAsString(filePath: String): String {
        val data = StringBuffer(1024)
        val reader = BufferedReader(FileReader(filePath))
        val buf = CharArray(1024)
        while (true) {
            val numRead = reader.read(buf)
            if (numRead == -1) break
            val readData = String(buf, 0, numRead)
            data.append(readData)
        }
        reader.close()
        return data.toString()
    }
    fun getMACAddress2(): String {
        val interfaceName = "wlan0"
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.getName().equals(interfaceName)) continue

                val mac = intf.getHardwareAddress() ?: return ""
                val buf = StringBuilder()
                for (idx in mac.indices)
                    buf.append(String.format("%02X:", mac[idx]))
                if (buf.length > 0) buf.deleteCharAt(buf.length - 1)
                return buf.toString()
            }
        } catch (ex: Exception) {
            Log.e("Error", ex.toString())
        }
        return ""
    }
    fun isOnline(context: Context) : Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }

    // 앱시작후 false로 세팅후 첫번째 Count가 들어오면 true로 변경됨.
    // 첫 Count 들어오기전에 Downtime 체크를 하지 않기 위함.
    fun set_first_count(state: Boolean) {
        UtilLocalStorage.setBoolean(instance._context!!, "first_count", state)
        if (state==false) {
            set_stitch_type(false)       // Stitch 검사 모드 끔
        }
    }
    fun get_first_count() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "first_count") }

    fun set_server_connect(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "server_state", state) }
    fun get_server_connect() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "server_state") }
    fun set_usb_connect(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "usb_state", state) }
    fun get_usb_connect() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "usb_state") }

    // Option Setting
    fun set_long_touch(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "long_touch", state) }
    fun get_long_touch() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "long_touch") }
    fun set_message_enable(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "msg_enable", state) }
    fun get_message_enable() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "msg_enable") }
    fun set_sound_at_count(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "sound_count", state) }
    fun get_sound_at_count() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "sound_count") }
    fun set_worksheet_display_time(value: Int) { UtilLocalStorage.setInt(instance._context!!, "worksheet_disptime", value) }
    fun get_worksheet_display_time() : Int { return UtilLocalStorage.getInt(instance._context!!, "worksheet_disptime") }

    fun set_sop_name(name: String) { UtilLocalStorage.setString(instance._context!!, "sop_name", name) }
    fun get_sop_name() : String {
        val name = UtilLocalStorage.getString(instance._context!!, "sop_name")
        return if (name != "") name else "WORK SHEET"
    }

    fun set_screen_blink(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "screen_blink", state) }
    fun get_screen_blink() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "screen_blink") }
    fun set_blink_color(value: String) { UtilLocalStorage.setString(instance._context!!, "blink_color", value) }
    fun get_blink_color() : String { return UtilLocalStorage.getString(instance._context!!, "blink_color") }

    fun set_start_at_target(value: Int) { UtilLocalStorage.setInt(instance._context!!, "start_at_target", value) }
    fun get_start_at_target() : Int { return UtilLocalStorage.getInt(instance._context!!, "start_at_target") }
    fun set_planned_count_process(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "planned_count", state) }
    fun get_planned_count_process() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "planned_count") }
    fun set_target_stop_when_downtime(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "target_stop_downtime", state) }
    fun get_target_stop_when_downtime() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "target_stop_downtime") }
    fun set_ask_when_clicking_defective(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "ask_click_defective", state) }
    fun get_ask_when_clicking_defective() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "ask_click_defective") }
//    fun set_piece_pair_count_edit(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "piece_pair_edit", state) }
//    fun get_piece_pair_count_edit() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "piece_pair_edit") }
    fun set_target_by_group(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "target_by_group", state) }
    fun get_target_by_group() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "target_by_group") }

    fun set_reverse_downtime_check(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "reverse_downtime_check", state) }
    fun get_reverse_downtime_check() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "reverse_downtime_check") }





    // millis value for Downtime
    fun set_last_count_received() { set_last_count_received(DateTime().toString("yyyy-MM-dd HH:mm:ss")) }
    fun set_last_count_received(value: String) { UtilLocalStorage.setString(instance._context!!, "last_received", value) }
    fun get_last_count_received() : String { return UtilLocalStorage.getString(instance._context!!, "last_received") }








//    fun set_without_component(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "current_without_component", state) }
//    fun get_without_component() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "current_without_component") }

//    fun set_wos_name(name: String) { UtilLocalStorage.setString(instance._context!!, "current_wos_name", name) }
//    fun get_wos_name() : String {
//        val name = UtilLocalStorage.getString(instance._context!!, "current_wos_name")
//        return if (name != "") name else "WOS"
//    }





//    fun set_remain_number(value: Int) { UtilLocalStorage.setInt(instance._context!!, "current_remain_number", value) }
//    fun get_remain_number() : Int { return UtilLocalStorage.getInt(instance._context!!, "current_remain_number") }






    // 작업자 정보 설정
    fun set_worker_no(name: String) { UtilLocalStorage.setString(instance._context!!, "current_worker_no", name) }
    fun get_worker_no() : String { return UtilLocalStorage.getString(instance._context!!, "current_worker_no") }
    fun set_worker_name(name: String) { UtilLocalStorage.setString(instance._context!!, "current_worker_name", name) }
    fun get_worker_name() : String { return UtilLocalStorage.getString(instance._context!!, "current_worker_name") }

    fun get_last_workers() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "last_workers") }
    fun remove_last_worker(no:String) {
        var list = get_last_workers()
        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            val item_no = item.getString("number")
            if (item_no==no) {
                list.remove(i)
                break
            }
        }
        UtilLocalStorage.setJSONArray(instance._context!!, "last_workers", list)
    }
    fun push_last_worker(no: String, name: String) {
        remove_last_worker(no)
        var list = get_last_workers()
        var json = JSONObject()
        json.put("number", no)
        json.put("name", name)
        list.put(json)
        if (list.length() > 4) list.remove(0)
        UtilLocalStorage.setJSONArray(instance._context!!, "last_workers", list)
    }

    fun get_last_designs() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "last_designs") }
    fun remove_last_design(no:String) {
        var list = get_last_designs()
        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            val item_no = item.getString("idx")
            if (item_no==no) {
                list.remove(i)
                break
            }
        }
        UtilLocalStorage.setJSONArray(instance._context!!, "last_designs", list)
    }
    fun push_last_design(idx: String, model: String, article: String, material_way: String, component: String, ct: String) {
        remove_last_design(idx)
        var list = get_last_designs()
        var json = JSONObject()
        json.put("idx", idx)
        json.put("model", model)
        json.put("article", article)
        json.put("material_way", material_way)
        json.put("component", component)
        json.put("ct", ct)
        list.put(json)
        if (list.length() > 5) list.remove(0)
        UtilLocalStorage.setJSONArray(instance._context!!, "last_designs", list)
    }


    // Design 관련 세팅값
    fun set_design_info(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "design_info", data) }
    fun get_design_info() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "design_info") }

    fun set_design_info_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_design_info_idx", idx) }
    fun get_design_info_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_design_info_idx") }
    fun set_model(data: String) { UtilLocalStorage.setString(instance._context!!, "current_model", data) }
    fun get_model() : String { return UtilLocalStorage.getString(instance._context!!, "current_model") }
    fun set_article(data: String) { UtilLocalStorage.setString(instance._context!!, "current_article", data) }
    fun get_article() : String { return UtilLocalStorage.getString(instance._context!!, "current_article") }
    fun set_material_way(data: String) { UtilLocalStorage.setString(instance._context!!, "current_material_way", data) }
    fun get_material_way() : String { return UtilLocalStorage.getString(instance._context!!, "current_material_way") }
    fun set_component(data: String) { UtilLocalStorage.setString(instance._context!!, "current_component", data) }
    fun get_component() : String { return UtilLocalStorage.getString(instance._context!!, "current_component") }
    fun set_cycle_time(idx: Int) { UtilLocalStorage.setInt(instance._context!!, "current_cycle_time", idx) }
    fun get_cycle_time() : Int { return UtilLocalStorage.getInt(instance._context!!, "current_cycle_time") }

    fun set_stitch(data: String) { UtilLocalStorage.setString(instance._context!!, "current_stitch", data) }
    fun get_stitch() : String { return UtilLocalStorage.getString(instance._context!!, "current_stitch") }

    fun set_pieces_text(txt: String) { UtilLocalStorage.setString(instance._context!!, "current_pieces_info", txt) }
    fun get_pieces_text() : String { return UtilLocalStorage.getString(instance._context!!, "current_pieces_info") }
    fun set_pieces_value(value: Int) { UtilLocalStorage.setInt(instance._context!!, "current_pieces_value", value) }
    fun get_pieces_value() : Int { return UtilLocalStorage.getInt(instance._context!!, "current_pieces_value") }
    fun set_pairs_text(txt: String) { UtilLocalStorage.setString(instance._context!!, "current_pairs_info", txt) }
    fun get_pairs_text() : String { return UtilLocalStorage.getString(instance._context!!, "current_pairs_info") }
    fun set_pairs_value(value: Float) { UtilLocalStorage.setFloat(instance._context!!, "current_pairs_value", value) }
    fun get_pairs_value() : Float { return UtilLocalStorage.getFloat(instance._context!!, "current_pairs_value") }


    // 작업 워크 고유값 설정 (커팅버전. 여기선 안씀)
//    fun set_work_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "work_idx", idx) }
//    fun get_work_idx() : String { return UtilLocalStorage.getString(instance._context!!, "work_idx") }

    // 패턴용 버전 (이앱에서 사용)
    fun set_product_idx() {
        var product_idx = get_product_idx()
        val new_product_idx = if (product_idx == "") 1000 else product_idx.toInt() + 1
        UtilLocalStorage.setString(instance._context!!, "work_idx", new_product_idx.toString())
    }
    fun get_product_idx() : String { return UtilLocalStorage.getString(instance._context!!, "work_idx") }
    fun reset_product_idx() { UtilLocalStorage.setString(instance._context!!, "work_idx", "") }

    // 작업시간 설정
    fun set_today_work_time(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "today_work_time", data) }      // 오늘의 shift 정보
    fun get_today_work_time() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "today_work_time") }
    fun set_prev_work_time(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "prev_work_time", data) }  // 어제의 shift 정보
    fun get_prev_work_time() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "prev_work_time") }

    fun set_work_time_manual(data: JSONObject) { UtilLocalStorage.setJSONObject(instance._context!!, "work_time_manual", data) }
    fun get_work_time_manual() : JSONObject? { return UtilLocalStorage.getJSONObject(instance._context!!, "work_time_manual") }

    fun set_current_work_day(data: String) { UtilLocalStorage.setString(instance._context!!, "work_day", data) }
    fun get_current_work_day() : String { return UtilLocalStorage.getString(instance._context!!, "work_day") }

    // 어제시간과 오늘시간 중에 지나지 않은 날짜의 JSON을 선택해서 반환
    fun get_current_work_time() : JSONArray {
        val today = get_today_work_time()
        val yesterday = get_prev_work_time()
        if (yesterday.length() > 0) {
            val item = yesterday.getJSONObject(yesterday.length()-1)
            var shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString())
            if (shift_etime.millis > DateTime().millis) return yesterday
        }
        return today
    }
    // 현재 작업중인 Shift의 JSON object 반환
    fun get_current_shift_time() : JSONObject? {
        val list = get_current_work_time()
//        if (list.length() == 0) return null
        val now = DateTime().millis
        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            if (OEEUtil.parseDateTime(item["work_stime"].toString()).millis <= now &&
                now < OEEUtil.parseDateTime(item["work_etime"].toString()).millis)
                return item
        }
        return null
    }
    // 현재 작업중인 Shift의 배열 index값 반환. 작업중이 아니면 -1 리턴
    fun get_current_shift_pos() : Int {
        val list = get_current_work_time()
        val now = DateTime().millis
        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            if (OEEUtil.parseDateTime(item["work_stime"].toString()).millis <= now &&
                now < OEEUtil.parseDateTime(item["work_etime"].toString()).millis)
                return i
        }
        return -1
    }
    // 현재 작업중인 Shift Idx 값 반환. 작업중이 아니면 "" 리턴
    fun get_current_shift_idx() : String {
//        val item: JSONObject = get_current_shift_time() ?: return "No-shift"
        val item: JSONObject = get_current_shift_time() ?: return "0"
        return item["shift_idx"].toString()
    }
    fun get_current_shift_name() : String {
        val item: JSONObject = get_current_shift_time() ?: return "No-shift"
        return item["shift_name"].toString()
    }

    // Shift info
//    fun set_current_shift_name(value: String) { UtilLocalStorage.setString(instance._context!!, "current_shift_name", value) }
//    fun get_current_shift_name() : String { return UtilLocalStorage.getString(instance._context!!, "current_shift_name") }

//    fun set_current_shift_idx(value: String) { UtilLocalStorage.setString(instance._context!!, "current_shift_idx", value) }
//    fun get_current_shift_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_shift_idx") }
//    fun get_current_shift_idx() : String {
//        var item: JSONObject = get_current_shift_time() ?: return ""
//        return item["shift_idx"].toString()
//    }



    // 다운 타임
    fun set_downtime_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_downtime_idx", idx) }
    fun get_downtime_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_downtime_idx") }

    // downtime 값이 Cycle Time 이라는 문자로 들어올 수도 있음
    // 그외 숫자가 들어오면 "" 빈값
    fun set_downtime_type(value: String) { UtilLocalStorage.setString(instance._context!!, "downtime_type", value) }
    fun get_downtime_type() : String { return UtilLocalStorage.getString(instance._context!!, "downtime_type") }

    fun set_downtime_sec(value: String) { UtilLocalStorage.setString(instance._context!!, "current_downtime_sec", value) }
    fun get_downtime_sec() : String { return UtilLocalStorage.getString(instance._context!!, "current_downtime_sec") }

    // Stitch 가 들어왔을 때 계산되는 초
    fun set_downtime_sec_for_stitch(value: String) { UtilLocalStorage.setString(instance._context!!, "downtime_sec_for_stitch", value) }
    fun get_downtime_sec_for_stitch() : String { return UtilLocalStorage.getString(instance._context!!, "downtime_sec_for_stitch") }

    // 바로 전에 Stitch 가 들어온 상태인지
    fun set_stitch_type(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "stitch_type", state) }
    fun get_stitch_type() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "stitch_type") }


    fun set_downtime_list(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "downtime_list", data) }
    fun get_downtime_list() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "downtime_list") }


    // 기타
    fun set_color_code(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "color_code", data) }
    fun get_color_code() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "color_code") }
    fun set_comopnent_data(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "comopnent_data", data) }
    fun get_comopnent_data() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "comopnent_data") }

    fun set_push_data(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "push_data", data) }
    fun get_push_data() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "push_data") }
//    fun set_availability(value: String) { UtilLocalStorage.setString(instance._context!!, "current_availability", value) }
//    fun get_availability() : String { return UtilLocalStorage.getString(instance._context!!, "current_availability") }
//    fun set_performance(value: String) { UtilLocalStorage.setString(instance._context!!, "current_performance", value) }
//    fun get_performance() : String { return UtilLocalStorage.getString(instance._context!!, "current_performance") }
//    fun set_quality(value: String) { UtilLocalStorage.setString(instance._context!!, "current_quality", value) }
//    fun get_quality() : String { return UtilLocalStorage.getString(instance._context!!, "current_quality") }

    // Layer정보 = pair 수
//    fun set_layer_pairs(layer_no: String, pair: String) { UtilLocalStorage.setString(instance._context!!, "current_layer_" + layer_no, pair) }
//    fun get_layer_pairs(layer_no: String) : String { return UtilLocalStorage.getString(instance._context!!, "current_layer_" + layer_no) }

//    fun set_trim_qty(value: String) { UtilLocalStorage.setString(instance._context!!, "current_trim_qty", value) }
//    fun get_trim_qty() : String { return UtilLocalStorage.getString(instance._context!!, "current_trim_qty") }
//    fun set_trim_pairs(pair: String) { UtilLocalStorage.setString(instance._context!!, "current_trim_pair", pair) }
//    fun get_trim_pairs() : String { return UtilLocalStorage.getString(instance._context!!, "current_trim_pair") }


    // cycle, server, manual 방식
    fun set_target_type(value: String) { UtilLocalStorage.setString(instance._context!!, "target_type", value) }
    fun get_target_type() : String { return UtilLocalStorage.getString(instance._context!!, "target_type") }

    fun set_last_shift_info(info: String) { UtilLocalStorage.setString(instance._context!!, "last_shift_info", info) }
    fun get_last_shift_info() : String { return UtilLocalStorage.getString(instance._context!!, "last_shift_info") }


    fun set_target_manual_shift(shift_no: String, value: String) { UtilLocalStorage.setString(instance._context!!, "current_target_shift_" + shift_no, value) }
    fun get_target_manual_shift(shift_no: String) : String { return UtilLocalStorage.getString(instance._context!!, "current_target_shift_" + shift_no) }

    fun set_target_server_shift(shift_no: String, value: String) { UtilLocalStorage.setString(instance._context!!, "current_server_target_shift_" + shift_no, value) }
    fun get_target_server_shift(shift_no: String) : String { return UtilLocalStorage.getString(instance._context!!, "current_server_target_shift_" + shift_no) }

    // 현 시프트의 토탈 타겟
    // From Server 와 From Device 사용
    fun get_current_shift_target() : Float {
        val item = get_current_shift_time()
        if (item != null) {
            val shift_idx = item["shift_idx"].toString()
            var target_type = get_target_type().substring(0, 6)
            if (target_type == "server") {
                var target = "0"
                if (get_target_by_group()) {
                    target = get_target_server_shift(shift_idx)
                } else {
                    try {
                        target = item["target"].toString()
                    } catch (e: JSONException) {
                        target = "0"
//                    e.printStackTrace()
                    }
//                var target2 = item!!["target"]?.toString() ?: "0"      // From server
//                var target = item?.getString("target") ?: "0"
                    if (target == "") target = "0"
                }
                return target.trim().toFloat()
            } else if (target_type == "device") {
                return when (shift_idx) {
                    "1" -> get_target_manual_shift("1").trim().toFloat()
                    "2" -> get_target_manual_shift("2").trim().toFloat()
                    "3" -> get_target_manual_shift("3").trim().toFloat()
                    else -> 0f
                }
            }
        }
        return 0f
    }

    // 현 시프트에서 한개 만드는데 걸리는 시간
    // From Server 와 From Device 사용
    fun get_current_maketime_per_piece() : Float {
        val item = get_current_shift_time()
        if (item != null) {
            val target = get_current_shift_target()

            if (target > 0f) {
                // 시프트 기본 정보
                val shift_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
                val shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString())

                // 휴식타임
                val _planned1_stime = OEEUtil.parseDateTime(item["planned1_stime_dt"].toString())
                val _planned1_etime = OEEUtil.parseDateTime(item["planned1_etime_dt"].toString())
                val _planned2_stime = OEEUtil.parseDateTime(item["planned2_stime_dt"].toString())
                val _planned2_etime = OEEUtil.parseDateTime(item["planned2_etime_dt"].toString())

                // 휴식시간 계산
                val d1 = compute_time(shift_stime, shift_etime, _planned1_stime, _planned1_etime)
                val d2 = compute_time(shift_stime, shift_etime, _planned2_stime, _planned2_etime)

                // 시프트 시작부터 종료까지 시간(초)
                val work_time = ((shift_etime.millis - shift_stime.millis) / 1000) - d1 - d2

                return (work_time.toFloat() / target)
            }
        }
        return 0F
    }

    fun get_current_shift_target_cnt() : String {
        var total_target = ""
        val target_type = get_target_type()
        val shift_idx = get_current_shift_idx()
        if (target_type.substring(0, 6) == "server") {
            total_target = "0"
//            when (shift_idx) {
//                "1" -> total_target = get_target_server_shift("1")
//                "2" -> total_target = get_target_server_shift("2")
//                "3" -> total_target = get_target_server_shift("3")
//            }
        } else if (target_type.substring(0, 6) == "device") {
            when (shift_idx) {
                "1" -> total_target = get_target_manual_shift("1")
                "2" -> total_target = get_target_manual_shift("2")
                "3" -> total_target = get_target_manual_shift("3")
            }
        }
        return total_target
    }

    fun set_current_shift_actual_cnt(value: Float) { UtilLocalStorage.setFloat(instance._context!!, "shift_actual_cnt", value) }
    fun get_current_shift_actual_cnt() : Float { return UtilLocalStorage.getFloat(instance._context!!, "shift_actual_cnt") }


    // 작업 정보
    fun set_accumulated_count(cnt: Int) { UtilLocalStorage.setInt(instance._context!!, "accumulated_count", cnt) }
    fun get_accumulated_count() : Int { return UtilLocalStorage.getInt(instance._context!!, "accumulated_count") }





    // 현재 쉬프트의 누적 시간을 구함
    fun get_current_shift_accumulated_time() : Int {

        var item = get_current_shift_time()
        if (item==null) return 0

        var shift_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
        var shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString())

        return compute_work_time(shift_stime, shift_etime, false)
    }

    // 현재 쉬프트의 총 작업 시간을 구함 (쉬프트시작시간,종료시간무시)
    fun get_current_shift_total_time() : Int {

        var item = get_current_shift_time()
        if (item==null) return 0

        var shift_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
        var shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString())

        return compute_work_time(shift_stime, shift_etime, false, false)
    }


    // 두시간(기간)에서 겹치는 시간을 계산해서 초로 리턴
    fun compute_time_millis(src_dt_s:Long, src_dt_e:Long, dst_dt_s:Long, dst_dt_e:Long) : Int {
        var dst_dt_s_cpy = dst_dt_s
        var dst_dt_e_cpy = dst_dt_e
        if (src_dt_s > dst_dt_s_cpy ) dst_dt_s_cpy = src_dt_s
        if (src_dt_e < dst_dt_e_cpy ) dst_dt_e_cpy = src_dt_e

        if (dst_dt_s_cpy >= src_dt_s && dst_dt_s_cpy <= src_dt_e &&
            dst_dt_e_cpy >= src_dt_s && dst_dt_e_cpy <= src_dt_e) {
            return ((dst_dt_e_cpy-dst_dt_s_cpy) / 1000 ).toInt()
        }
        return 0
    }
    // 두시간(기간)에서 겹치는 시간을 계산
    // 위의 compute_time_millis()와 같음.
    fun compute_time(src_dt_s:DateTime, src_dt_e:DateTime, dst_dt_s:DateTime, dst_dt_e:DateTime) : Int {
        var dst_dt_s_cpy = dst_dt_s
        var dst_dt_e_cpy = dst_dt_e
        if (src_dt_s.millis > dst_dt_s_cpy.millis ) dst_dt_s_cpy = src_dt_s
        if (src_dt_e.millis < dst_dt_e_cpy.millis ) dst_dt_e_cpy = src_dt_e

        var src_diff = ( src_dt_e.millis - src_dt_s.millis)
        var dst_diff = ( dst_dt_e_cpy.millis - dst_dt_s_cpy.millis)

        if (dst_dt_s_cpy.millis >= src_dt_s.millis && dst_dt_s_cpy.millis <= src_dt_e.millis &&
            dst_dt_e_cpy.millis >= src_dt_s.millis && dst_dt_e_cpy.millis <= src_dt_e.millis) {
            return (dst_diff / 1000 ).toInt()
        }
        return 0
    }

    // 특정시간(기간)안에 휴식시간과 다운타임시간을 뺀 시간을 계산
    fun compute_work_time(stime:DateTime, etime:DateTime, is_downtime:Boolean = true, is_total:Boolean = true) : Int {
        val item = get_current_shift_time()
        if (item == null) return 0

        var shift_stime = stime
        var shift_etime = etime

        // 작업시간내에서만 계산
        val shift_stime_src = OEEUtil.parseDateTime(item["work_stime"].toString())
        val shift_etime_src = OEEUtil.parseDateTime(item["work_etime"].toString())

        val shift_stime_millis = shift_stime_src.millis
        val shift_etime_millis = shift_etime_src.millis

        if (shift_stime.millis < shift_stime_millis) shift_stime = shift_stime_src
        if (shift_etime.millis < shift_stime_millis || shift_stime.millis > shift_etime_millis) return 0

        if (is_total) {
            val now = DateTime()
            if (now.millis < shift_etime.millis ) shift_etime = now  // 작업종료시간보다 넘은경우, 현재시간은 종료시간으로 고정
            if (now.millis < shift_stime.millis ) shift_stime = now  // 작업시작시간보다 빠른경우, 현재시간은 시작시간으로 고정
        }

        var dif = (shift_etime.millis - shift_stime.millis) / 1000

        //Log.e("test", "shift_stime = "+ shift_stime.toString())
        //Log.e("test", "shift_etime = "+ shift_etime.toString())

        // 휴식 시간 계산

        val planned1_stime = OEEUtil.parseDateTime(item["planned1_stime_dt"].toString())
        val planned1_etime = OEEUtil.parseDateTime(item["planned1_etime_dt"].toString())
        val planned2_stime = OEEUtil.parseDateTime(item["planned2_stime_dt"].toString())
        val planned2_etime = OEEUtil.parseDateTime(item["planned2_etime_dt"].toString())

        val d1 = compute_time(shift_stime, shift_etime, planned1_stime, planned1_etime)
        val d2 = compute_time(shift_stime, shift_etime, planned2_stime, planned2_etime)

        dif = dif - d1 - d2
        //Log.e("test", "dif1 = "+ dif.toString())

        if (is_downtime==false) return dif.toInt()
/*
        // 다운타임 시간 계산
        var db = DBHelperForDownTime(_context!!)
        var list = db.gets() ?: null

        list?.forEach { item ->
            var end_dt = item["end_dt"].toString()
            if (end_dt=="null") end_dt = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")

            val downtime_s = OEEUtil.parseDateTime(item["start_dt"].toString())
            val downtime_e = OEEUtil.parseDateTime(end_dt)
            val df = compute_time(shift_stime, shift_etime, downtime_s, downtime_e)
            dif -= df

            // 휴식시간과 다운타임시간 중복 계산
            if (planned1_stime.millis < shift_stime.millis ) planned1_stime = shift_stime
            if (planned1_etime.millis > shift_etime.millis ) planned1_etime = shift_etime

            if (planned1_etime.millis-planned1_stime.millis >0) {
                val p1_dw = compute_time(planned1_stime, planned1_etime, downtime_s, downtime_e)
                dif += p1_dw
            }

            if (planned2_stime.millis < shift_stime.millis ) planned2_stime = shift_stime
            if (planned2_etime.millis > shift_etime.millis ) planned2_etime = shift_etime
            if (planned2_etime.millis-planned2_stime.millis >0) {
                val p2_dw = compute_time(planned2_stime, planned2_etime, downtime_s, downtime_e)
                dif += p2_dw
            }
        }
        //Log.e("test", "dif2 = "+ dif.toString())
*/
        return dif.toInt()
    }



    fun get_local_ip(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val interf = en.nextElement()
                val ips = interf.inetAddresses
                while (ips.hasMoreElements()) {
                    val inetAddress = ips.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress.toString()
                    }
                }
            }
        } catch (ex: SocketException) {
            Log.e("Error", ex.toString())
        }
        return ""
    }


    // Network & Wifi check
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }
    fun isWifiConnected(context: Context): Boolean {
        if (isNetworkAvailable(context)) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
        }
        return false
    }
    fun isEthernetConnected(context: Context): Boolean {
        if (isNetworkAvailable(context)) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo.type == ConnectivityManager.TYPE_ETHERNET
        }
        return false
    }
    fun getWiFiSSID(context: Context): String {
        if (isWifiConnected(context)) {
            val manager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = manager.connectionInfo
            return wifiInfo.ssid
        }
        else if (isEthernetConnected(context)) {
            return "Ethernet"
        }
        return "unknown or no connected"
    }

    fun playSound(context: Context) {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}