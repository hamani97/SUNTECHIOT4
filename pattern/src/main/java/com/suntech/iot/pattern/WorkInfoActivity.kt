package com.suntech.iot.pattern

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.TextView
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_workinfo.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import kotlinx.android.synthetic.main.layout_workinfo_manual.*
import kotlinx.android.synthetic.main.layout_workinfo_server.*
import org.joda.time.DateTime
import org.json.JSONObject
import java.util.*

class WorkInfoActivity : BaseActivity() {

    private var tab_pos : Int = 1
    private var usb_state = false

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    var _selected_index = -1

    private var list_for_operator_adapter: ListOperatorAdapter? = null
    private var _list_for_operator: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _filtered_list_for_operator: ArrayList<HashMap<String, String>> = arrayListOf()

    private var list_for_last_worker_adapter: ListOperatorAdapter? = null
    private var _list_for_last_worker: ArrayList<HashMap<String, String>> = arrayListOf()

    private val _broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)){
                    btn_wifi_state.isSelected = true
                } else {
                    btn_wifi_state.isSelected = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workinfo)
        initShiftData()
        fetchOperatorData(false)
        initLastWorkers()
        initView()
        start_timer()
    }

    fun parentSpaceClick(view: View) {
        var view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))

        // USB state
        btn_usb_state2.isSelected = AppGlobal.instance.get_usb_connect()

        btn_server_state.isSelected = AppGlobal.instance.get_server_connect()
        btn_wifi_state.isSelected = AppGlobal.instance.isOnline(this)

        is_loop = true
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(_broadcastReceiver)
        is_loop = false
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    private fun initView() {
        tv_title.setText(R.string.title_operator_detail)

        // Shift info
        list_adapter = ListAdapter(this, _list)
        lv_available_info.adapter = list_adapter

        _selected_index = AppGlobal.instance.get_current_shift_pos()

        // worker info
        list_for_operator_adapter = ListOperatorAdapter(this, _filtered_list_for_operator)
        lv_operator_info.adapter = list_for_operator_adapter

        lv_operator_info.setOnItemClickListener { adapterView, view, i, l ->
            list_for_operator_adapter?.select(i)
            list_for_operator_adapter?.notifyDataSetChanged()
        }

        // last worker info
        list_for_last_worker_adapter = ListOperatorAdapter(this, _list_for_last_worker)
        lv_last_worker.adapter = list_for_last_worker_adapter

        lv_last_worker.setOnItemClickListener { adapterView, view, i, l ->
            et_search_text.setText("")

            list_for_last_worker_adapter?.select(i)
            list_for_last_worker_adapter?.notifyDataSetChanged()

            var list = AppGlobal.instance.get_last_workers()
            val worker = list.getJSONObject(list.length() - 1 - i)

            for (j in 0..(_list_for_operator.size-1)) {
                val item = _list_for_operator[j]
                val number = item["number"] ?: ""
                if (number == worker.getString("number")) {
                    list_for_operator_adapter?.select(j)
                    list_for_operator_adapter?.notifyDataSetChanged()
                    lv_operator_info.smoothScrollToPosition(j)
                    break
                }
            }
        }

        et_search_text.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s != "") {
                    filterOperatorData()
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        // Tab button click
        btn_work_info_server.setOnClickListener { tabChange(1) }
        btn_work_info_manual.setOnClickListener { tabChange(2) }

        // Command button click
        btn_reload.setOnClickListener {
            et_search_text.setText("")
            list_for_operator_adapter?.select(-1)
            list_for_last_worker_adapter?.select(-1)

            fetchShiftData()
            fetchOperatorData(true)
            initLastWorkers()

            _selected_index = AppGlobal.instance.get_current_shift_pos()
        }
        btn_setting_confirm.setOnClickListener {
            val selected_index = list_for_operator_adapter?.getSelected() ?:-1
            if (selected_index < 0) {
                val last_no = AppGlobal.instance.get_worker_no()
                val last_name = AppGlobal.instance.get_worker_name()
                if (last_no == "" && last_name == "") {
                    ToastOut(this, R.string.msg_has_notselected, true)
                    return@setOnClickListener
                }
            }
            if (_filtered_list_for_operator != null && selected_index >= 0) {
                val no = _filtered_list_for_operator[selected_index]["number"]!!
                val name = _filtered_list_for_operator[selected_index]["name"]!!
                AppGlobal.instance.set_worker_no(no)
                AppGlobal.instance.set_worker_name(name)
                AppGlobal.instance.push_last_worker(no, name)
            }
            saveWorkTime()
        }
        btn_setting_cancel.setOnClickListener { finish() }
    }

    private fun saveWorkTime() {

        var shift3 = JSONObject()

        // Work time
        var start_hour = et_setting_s_3_s_h.text.toString().trim()
        var start_min = et_setting_s_3_s_m.text.toString().trim()
        var end_hour = et_setting_s_3_e_h.text.toString().trim()
        var end_min = et_setting_s_3_e_m.text.toString().trim()

        if (start_hour!="" || start_min!="" || end_hour!="" || end_min!="") {
            if (start_hour.length != 2 || start_min.length != 2 || end_hour.length != 2 || end_min.length != 2 ||
                start_hour < "00" || start_hour > "23" || end_hour < "00" || end_hour > "23" ||
                start_min < "00" || start_min > "59" || end_min < "00" || end_min > "59") {
                ToastOut(this, "The time input for shift3 is invalid", true)
                return
            }

            shift3.put("available_stime", start_hour + ":" + start_min)
            shift3.put("available_etime", end_hour + ":" + end_min)

            // Planned time
            start_hour = et_setting_p_3_s_h.text.toString().trim()
            start_min = et_setting_p_3_s_m.text.toString().trim()
            end_hour = et_setting_p_3_e_h.text.toString().trim()
            end_min = et_setting_p_3_e_m.text.toString().trim()

            if (start_hour!="" || start_min!="" || end_hour!="" || end_min!="") {
                if (start_hour.length != 2 || start_min.length != 2 || end_hour.length != 2 || end_min.length != 2 ||
                    start_hour < "00" || start_hour > "23" || end_hour < "00" || end_hour > "23" ||
                    start_min < "00" || start_min > "59" || end_min < "00" || end_min > "59") {
                    ToastOut(this, "Planned Time input for shift3 is invalid", true)
                    return
                }
                shift3.put("planned1_stime", start_hour + ":" + start_min)
                shift3.put("planned1_etime", end_hour + ":" + end_min)
            } else {
                shift3.put("planned1_stime", "")
                shift3.put("planned1_etime", "")
            }
        } else {
            shift3.put("available_stime", "")
            shift3.put("available_etime", "")
            shift3.put("planned1_stime", "")
            shift3.put("planned1_etime", "")
        }
        AppGlobal.instance.set_work_time_manual(shift3)

        finish(true, 1, "ok", null)
    }

    private fun initShiftData() {
        val list = AppGlobal.instance.get_current_work_time()
        if (list.length() == 0) return

        Log.e("WorkInfo", " " + list.toString())

        _list.removeAll(_list)

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var map = hashMapOf(
                "idx" to item.getString("idx"),
                "date" to item.getString("date"),
                "work_stime" to item.getString("work_stime"),
                "work_etime" to item.getString("work_etime"),
                "available_stime" to item.getString("available_stime"),
                "available_etime" to item.getString("available_etime"),
                "planned1_stime" to item.getString("planned1_stime"),
                "planned1_etime" to item.getString("planned1_etime"),
                "planned2_stime" to item.getString("planned2_stime"),
                "planned2_etime" to item.getString("planned2_etime"),
                "planned3_stime" to item.getString("planned3_stime"),
                "planned3_etime" to item.getString("planned3_etime"),
                "over_time" to item.getString("over_time"),
                "line_idx" to item.getString("line_idx"),
                "line_name" to item.getString("line_name"),
                "shift_idx" to item.getString("shift_idx"),
                "shift_name" to item.getString("shift_name")
            )
            _list.add(map)
        }
        list_adapter?.notifyDataSetChanged()

        initViewManual()
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

    private fun fetchShiftData() {

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

                initShiftData()
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun initViewManual() {
        // shift-1, shift-2는 서버에서 내려온 작업시간
        if (_list.size >= 1 && _list[0].get("shift_idx") == "1") {
            val work_stime = OEEUtil.parseDateTime(_list[0]["work_stime"].toString())
            val work_etime = OEEUtil.parseDateTime(_list[0]["work_etime"].toString())

            et_setting_s_1_s_h.text = work_stime.toString("HH")
            et_setting_s_1_s_m.text = work_stime.toString("mm")
            et_setting_s_1_e_h.text = work_etime.toString("HH")
            et_setting_s_1_e_m.text = work_etime.toString("mm")

            if (_list[0]["planned1_stime"]!=null && _list[0]["planned1_stime"]!="") {
                val planned1_stime = OEEUtil.parseTime2(_list[0]["planned1_stime"].toString())
                et_setting_p_1_s_h.text = planned1_stime.toString("HH")
                et_setting_p_1_s_m.text = planned1_stime.toString("mm")
            }
            if (_list[0]["planned1_etime"]!=null && _list[0]["planned1_etime"]!="") {
                val planned1_etime = OEEUtil.parseTime2(_list[0]["planned1_etime"].toString())
                et_setting_p_1_e_h.text = planned1_etime.toString("HH")
                et_setting_p_1_e_m.text = planned1_etime.toString("mm")
            }
        }
        if (_list.size >= 2 && _list[1].get("shift_idx") == "2") {
            val work_stime = OEEUtil.parseDateTime(_list[1]["work_stime"].toString())
            val work_etime = OEEUtil.parseDateTime(_list[1]["work_etime"].toString())

            et_setting_s_2_s_h.text = work_stime.toString("HH")
            et_setting_s_2_s_m.text = work_stime.toString("mm")
            et_setting_s_2_e_h.text = work_etime.toString("HH")
            et_setting_s_2_e_m.text = work_etime.toString("mm")

            if (_list[1]["planned1_stime"]!=null && _list[1]["planned1_stime"]!="") {
                val planned2_stime = OEEUtil.parseTime2(_list[1]["planned1_stime"].toString())
                et_setting_p_2_s_h.text = planned2_stime.toString("HH")
                et_setting_p_2_s_m.text = planned2_stime.toString("mm")
            }
            if (_list[1]["planned1_etime"]!=null && _list[1]["planned1_etime"]!="") {
                val planned2_etime = OEEUtil.parseTime2(_list[1]["planned1_etime"].toString())
                et_setting_p_2_e_h.text = planned2_etime.toString("HH")
                et_setting_p_2_e_m.text = planned2_etime.toString("mm")
            }
        }

        val shift3 = AppGlobal.instance.get_work_time_manual()

        if (shift3 != null && shift3.length()>0) {
            val available_stime = shift3.getString("available_stime")
            val available_etime = shift3.getString("available_etime")
            val planned1_stime = shift3.getString("planned1_stime")
            val planned1_etime = shift3.getString("planned1_etime")

            if (available_stime != null && available_stime != "") {
                val time = OEEUtil.parseTime2(available_stime)
                et_setting_s_3_s_h.setText(time.toString("HH"))
                et_setting_s_3_s_m.setText(time.toString("mm"))
            }
            if (available_etime != null && available_etime != "") {
                val time = OEEUtil.parseTime2(available_etime)
                et_setting_s_3_e_h.setText(time.toString("HH"))
                et_setting_s_3_e_m.setText(time.toString("mm"))
            }
            if (planned1_stime != null && planned1_stime != "") {
                val time = OEEUtil.parseTime2(planned1_stime)
                et_setting_p_3_s_h.setText(time.toString("HH"))
                et_setting_p_3_s_m.setText(time.toString("mm"))
            }
            if (planned1_etime != null && planned1_etime != "") {
                val time = OEEUtil.parseTime2(planned1_etime)
                et_setting_p_3_e_h.setText(time.toString("HH"))
                et_setting_p_3_e_m.setText(time.toString("mm"))
            }
        }
    }

    private fun filterOperatorData() {
        _filtered_list_for_operator.removeAll(_filtered_list_for_operator)
        list_for_operator_adapter?.select(-1)

        val filter_text = et_search_text.text.toString()

        for (i in 0..(_list_for_operator.size-1)) {

            val item = _list_for_operator[i]
            val number = item["number"] ?: ""
            val name = item["name"] ?: ""

            val b = number.toUpperCase().contains(filter_text.toUpperCase())
            val c = name.toUpperCase().contains(filter_text.toUpperCase())
            if (filter_text=="" || b || c) {
                _filtered_list_for_operator.add(item)
            }
        }
        list_for_operator_adapter?.notifyDataSetChanged()
    }

    private fun fetchOperatorData(progress: Boolean = false) {
        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "worker",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx())
        request(this, uri, progress, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                _list_for_operator.removeAll(_list_for_operator)

                var list = result.getJSONArray("item")
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                        "idx" to item.getString("idx"),
                        "number" to item.getString("number"),
                        "name" to item.getString("name")
                    )
                    _list_for_operator.add(map)
                }
                filterOperatorData()
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
        filterOperatorData()
    }

    private fun initLastWorkers() {
        _list_for_last_worker.removeAll(_list_for_last_worker)
        var list = AppGlobal.instance.get_last_workers()
        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(list.length() - 1 - i)
            var worker = hashMapOf("number" to item.getString("number"), "name" to item.getString("name"))
            _list_for_last_worker.add(worker)
        }
        list_for_last_worker_adapter?.notifyDataSetChanged()
    }

    private fun sendPing() {
        if (AppGlobal.instance.get_server_ip() == "") return
        val uri = "/ping.php"
        request(this, uri, false, false, null, { result ->
            var code = result.getString("code")
            if (code == "00") {
                btn_server_state.isSelected = true
                AppGlobal.instance.set_server_connect(true)
            } else {
                ToastOut(this, result.getString("msg"))
            }
        }, {
            btn_server_state.isSelected = false
        })
    }

    /////// 쓰레드
    private val _timer_task1 = Timer()          // 서버 접속 체크 ping test.
    private val _timer_task2 = Timer()
    private var is_loop = true

    private fun start_timer() {
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (is_loop) sendPing()
                }
            }
        }
        _timer_task1.schedule(task1, 5000, 10000)

        val task2 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (is_loop) checkUSB()
                }
            }
        }
        _timer_task2.schedule(task2, 500, 1000)
    }
    private fun cancel_timer () {
        _timer_task1.cancel()
        _timer_task2.cancel()
    }

    private fun checkUSB() {
        if (usb_state != AppGlobal.instance.get_usb_connect()) {
            usb_state = AppGlobal.instance.get_usb_connect()
            btn_usb_state2.isSelected = usb_state
        }
    }

    private class ListAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

        private var _list: ArrayList<HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_available_info, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            var work_stime = OEEUtil.parseDateTime(_list[position]["work_stime"].toString())
            var work_etime = OEEUtil.parseDateTime(_list[position]["work_etime"].toString())

            vh.tv_item_shift.text = _list[position]["shift_name"]
            vh.tv_item_work_time.text = if (work_stime.toString() != "" && work_etime.toString() != "") work_stime.toString("HH:mm") + "~" + work_etime.toString("HH:mm") else ""
            vh.tv_item_planned_time1.text = if (_list[position]["planned1_stime"] != "" && _list[position]["planned1_etime"] != "") _list[position]["planned1_stime"] + "~" + _list[position]["planned1_etime"] else ""
            vh.tv_item_planned_time2.text = if (_list[position]["planned2_stime"] != "" && _list[position]["planned2_etime"] != "") _list[position]["planned2_stime"] + "~" + _list[position]["planned2_etime"] else ""

            if ((_context as WorkInfoActivity)._selected_index==position) {
                vh.tv_item_shift.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_work_time.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_planned_time1.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_planned_time2.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
            } else {
                vh.tv_item_shift.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_work_time.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_planned_time1.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_planned_time2.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_shift: TextView
            val tv_item_work_time: TextView
            val tv_item_planned_time1: TextView
            val tv_item_planned_time2: TextView

            init {
                this.tv_item_shift = row?.findViewById<TextView>(R.id.tv_item_shift) as TextView
                this.tv_item_work_time = row?.findViewById<TextView>(R.id.tv_item_work_time) as TextView
                this.tv_item_planned_time1 = row?.findViewById<TextView>(R.id.tv_item_planned_time1) as TextView
                this.tv_item_planned_time2 = row?.findViewById<TextView>(R.id.tv_item_planned_time2) as TextView
            }
        }
    }

    private class ListOperatorAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

        private var _list: ArrayList<HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null
        private var _selected_op_index = -1

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        fun select(index:Int) { _selected_op_index=index }
        fun getSelected(): Int { return _selected_op_index }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_operator_info, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_employee_number.text = _list[position]["number"]
            vh.tv_item_name.text = _list[position]["name"]

            if (_selected_op_index==position) {
                vh.tv_item_employee_number.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_name.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
            } else {
                vh.tv_item_employee_number.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_name.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_employee_number: TextView
            val tv_item_name: TextView

            init {
                this.tv_item_employee_number = row?.findViewById<TextView>(R.id.tv_item_employee_number) as TextView
                this.tv_item_name = row?.findViewById<TextView>(R.id.tv_item_name) as TextView
            }
        }
    }

    private fun tabChange(v : Int) {
        if (tab_pos == v) return
        tab_pos = v
        when (tab_pos) {
            1 -> {
                btn_work_info_server.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_work_info_server.setBackgroundResource(R.color.colorButtonBlue)
                btn_work_info_manual.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_work_info_manual.setBackgroundResource(R.color.colorButtonDefault)
                layout_work_info_server.visibility = View.VISIBLE
                layout_work_info_manual.visibility = View.GONE
            }
            2 -> {
                btn_work_info_server.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_work_info_server.setBackgroundResource(R.color.colorButtonDefault)
                btn_work_info_manual.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_work_info_manual.setBackgroundResource(R.color.colorButtonBlue)
                layout_work_info_server.visibility = View.GONE
                layout_work_info_manual.visibility = View.VISIBLE
            }
        }
    }
}