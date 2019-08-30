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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_work_info.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import org.json.JSONObject
import java.util.*

class WorkInfoActivity : BaseActivity() {

    private var tab_pos : Int = 1

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    var _selected_index = -1

//    private var _list_json: JSONArray? = null

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
        setContentView(R.layout.activity_work_info)
        fetchShiftData()
        fetchOperatorData()
        initLastWorkers()
        initView()
        start_timer()
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
        updateView()
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(_broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    private fun updateView() {
        if (AppGlobal.instance._server_state) btn_server_state.isSelected = true
        else btn_server_state.isSelected = false

        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false
    }

    private fun initView() {

        tv_title.text = "OPERATOR DETAIL"

        // Shift info
        list_adapter = ListAdapter(this, _list)
        lv_available_info.adapter = list_adapter

        val idx = AppGlobal.instance.get_current_shift_idx()
        _selected_index = if (idx == "") -1 else (idx.toInt()-1)

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
        btn_setting_confirm.setOnClickListener {
            val selected_index = list_for_operator_adapter?.getSelected() ?:-1
            if (selected_index < 0) {
                val last_no = AppGlobal.instance.get_worker_no()
                val last_name = AppGlobal.instance.get_worker_name()
                if (last_no == "" && last_name == "") {
                    Toast.makeText(this, getString(R.string.msg_has_notselected), Toast.LENGTH_SHORT).show()
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
        btn_setting_cancel.setOnClickListener {
            AppGlobal.instance.set_auto_setting(false)
            finish()
        }
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
                Toast.makeText(this, "The time input for shift3 is invalid", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Planned Time input for shift3 is invalid", Toast.LENGTH_SHORT).show()
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

    private fun fetchShiftData() {
        val list = AppGlobal.instance.get_current_work_time()
//        _list_json = list

        if (list == null) return

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

    private fun fetchOperatorData() {
        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "worker",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx())

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
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
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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
                AppGlobal.instance._server_state = true
            } else {
                ToastOut(this, result.getString("msg"))
            }
        }, {
            btn_server_state.isSelected = false
        })
    }

    /////// 쓰레드
    private val _timer_task1 = Timer()          // 서버 접속 체크 ping test.

    private fun start_timer() {
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    sendPing()
                }
            }
        }
        _timer_task1.schedule(task1, 5000, 10000)
    }
    private fun cancel_timer () {
        _timer_task1.cancel()
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
        private var _selected_index = -1

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        fun select(index:Int) {_selected_index=index}
        fun getSelected(): Int { return _selected_index }

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

            if (_selected_index==position) {
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