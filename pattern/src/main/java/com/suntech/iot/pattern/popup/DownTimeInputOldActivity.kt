package com.suntech.iot.pattern.popup

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_down_time_input_old.*
import org.joda.time.DateTime
import java.util.*


class DownTimeInputOldActivity : BaseActivity() {

    private var _db = DBHelperForDownTime(this)

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _selected_idx = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.suntech.iot.pattern.R.layout.activity_down_time_input_old)
        initView()
        fetchData()
        updateList()
    }

    private fun initView() {

        list_adapter = ListAdapter(this, _list)
        lv_downtimes.adapter = list_adapter

        lv_downtimes.setOnItemClickListener { adapterView, view, i, l ->
            _selected_idx = i
            _list.forEach { item ->
                if (i==_list.indexOf(item)) item.set("selected", "Y")
                else item.set("selected", "N")
            }
            list_adapter?.notifyDataSetChanged()
        }

        btn_confirm.setOnClickListener {
            sendEndDownTime()
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }

    private fun updateList () {
        _list.removeAll(_list)

        var list = AppGlobal.instance.get_downtime_list()

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var map = hashMapOf(
                "idx" to item.getString("idx"),
                "name" to item.getString("name"),
                "color" to item.getString("color"),
                "selected" to "N"
            )
            _list.add(map)
        }

        Collections.sort(_list, object : Comparator<HashMap<String, String>> {
            override fun compare(p0: HashMap<String, String>, p1: HashMap<String, String>): Int {
                return p0["idx"]!!.compareTo(p1["idx"]!!)
            }
        })

        list_adapter?.notifyDataSetChanged()
    }

    private fun sendEndDownTime() {
        if (AppGlobal.instance.get_server_ip() == "") {
            ToastOut(this, com.suntech.iot.pattern.R.string.msg_has_not_server_info, true)
            return
        }
        if (AppGlobal.instance.get_downtime_idx() == "") {
            ToastOut(this, com.suntech.iot.pattern.R.string.msg_data_not_found, true)
            return
        }
        if (_selected_idx < 0) {
            ToastOut(this, com.suntech.iot.pattern.R.string.msg_has_notselected, true)
            return
        }
        val idx = AppGlobal.instance.get_downtime_idx()

        val now = DateTime()
        var down_time = 0
        var real_down_time = 0
        var target = 0f

        val item = _db.get(idx)
        if (item != null) {
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
            down_time = ((now_millis - down_start_millis) / 1000).toInt()   // 다운시간
            real_down_time = down_time - planned1_time - planned2_time      // 휴식시간을 뺀 실제 다운타임

            val ct = AppGlobal.instance.get_cycle_time()
            if (ct > 0) target = real_down_time.toFloat() / ct
        }

        val downtime = _list[_selected_idx]["idx"]

        val uri = "/downtimedata.php"
        var params = listOf(
            "code" to "end",
            "idx" to AppGlobal.instance.get_downtime_idx(),
            "downtime" to downtime,
            "worker" to AppGlobal.instance.get_worker_no(),
            "edate" to now.toString("yyyy-MM-dd"),
            "etime" to now.toString("HH:mm:ss"))

        btn_confirm.isEnabled = false
        btn_cancel.isEnabled = false

        request(this, uri, true,true, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                val idx = AppGlobal.instance.get_downtime_idx()
                AppGlobal.instance.set_downtime_idx("")

                _db.updateEnd(idx, _list[_selected_idx]["name"] ?: "", now.toString("yyyy-MM-dd HH:mm:ss"), down_time, real_down_time, target)

                ToastOut(this, msg, true)
                finish(true, 0, "ok", null)

            } else if (code == "99") {
                resendStartDownTime()

            } else {
                btn_confirm.isEnabled = true
                btn_cancel.isEnabled = true
                ToastOut(this, msg, true)
            }
        })
    }

    private fun resendStartDownTime() {
        if (AppGlobal.instance.get_server_ip() == "") return

        val work_idx = "" + AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        val idx = intent.getStringExtra("idx")
        val item = _db.get(idx)

        if (item !=null) {
            val start_dt = item["start_dt"].toString()
            val didx = item["design_idx"].toString()
            val shift_idx = item["shift_id"].toString()
            val shift_name = item["shift_name"].toString()
            val dt = OEEUtil.parseDateTime(start_dt)
            _db.delete(idx)

//            var work_db = SimpleDatabaseHelper(this)
//            val row = work_db.get(work_idx)
//            val seq = row!!["seq"].toString().toInt()
            val seq = item["seq"]

            val uri = "/downtimedata.php"
            var params = listOf(
                "code" to "start",
                "mac_addr" to AppGlobal.instance.getMACAddress(),
                "didx" to didx,
                "sdate" to dt.toString("yyyy-MM-dd"),
                "stime" to dt.toString("HH:mm:ss"),
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_zone_idx(),
                "line_idx" to AppGlobal.instance.get_line_idx(),
                "shift_idx" to shift_idx,
                "seq" to seq)

            request(this, uri, true, false, params, { result ->
                var code = result.getString("code")
                if (code == "00") {
                    var idx = result.getString("idx")
                    AppGlobal.instance.set_downtime_idx(idx)

                    _db.add(idx, work_idx, didx, shift_idx, shift_name, start_dt)

                    sendEndDownTime()
                } else {
                    ToastOut(this, result.getString("msg"), true)
                }
            })
        }
    }

    private fun fetchData() {
        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "down_time",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx())

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var list = result.getJSONArray("item")
                AppGlobal.instance.set_downtime_list(list)
                updateList()
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    class ListAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

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
                view = this._inflator.inflate(com.suntech.iot.pattern.R.layout.list_item_downtime_type, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_downtime_name.text = _list[position]["name"]
            vh.tv_item_downtime_name.setTextColor(Color.parseColor("#"+_list[position]["color"]))

            if (_list[position]["selected"]=="Y") vh.tv_item_downtime_check_box.isSelected = true
            else vh.tv_item_downtime_check_box.isSelected = false
            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_downtime_check_box: ImageView
            val tv_item_downtime_name: TextView

            init {
                this.tv_item_downtime_check_box = row?.findViewById<ImageView>(com.suntech.iot.pattern.R.id.tv_item_downtime_check_box) as ImageView
                this.tv_item_downtime_name = row?.findViewById<TextView>(com.suntech.iot.pattern.R.id.tv_item_downtime_name) as TextView
            }
        }
    }
}
