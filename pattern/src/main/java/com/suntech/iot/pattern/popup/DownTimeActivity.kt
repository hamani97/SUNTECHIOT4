package com.suntech.iot.pattern.popup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_down_time.*
import kotlinx.android.synthetic.main.list_item_downtime_total.*
import java.util.*

class DownTimeActivity : BaseActivity() {

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    private val _timer_task1 = Timer()

    val _start_down_time_activity = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_down_time)
        initView()
        updateView()
        start_timer()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(_start_down_time_activity, IntentFilter("start.downtime"))
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
        unregisterReceiver(_start_down_time_activity)
    }

    private fun initView() {
        btn_confirm.setOnClickListener {
            var db = DBHelperForDownTime(this)
            val count = db.counts_for_notcompleted()
            if (count>0) {
                Toast.makeText(this, getString(R.string.msg_has_notcompleted), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            finish(true, 1, "ok", null)
        }

        lv_downtimes.setOnItemClickListener { adapterView, view, i, l ->
            val idx = _list[i]["idx"]
            val completed = _list[i]["completed"]
            if (completed=="Y") return@setOnItemClickListener

            val intent = Intent(this, DownTimeInputActivity::class.java)
            intent.putExtra("idx", idx)
            startActivity(intent, { r, c, m, d ->
                if (r) {
                    updateView()
                }
            })
        }
    }

    private fun updateView() {
        var db = DBHelperForDownTime(this)
        _list = db.gets() ?: _list

        list_adapter = ListAdapter(this, _list)
        lv_downtimes.adapter = list_adapter

        var total_downtime = 0

        _list?.forEach { item ->
            item.put("downtime", "")
            val start_dt = OEEUtil.parseDateTime(item["start_dt"])
            if (item["end_dt"]!=null) {
                val end_dt = OEEUtil.parseDateTime(item["end_dt"])

                var dif = end_dt.millis - start_dt.millis
                val downtime = (dif / 1000 / 60 ).toInt()
                total_downtime += downtime
                item.set("downtime", downtime.toString()+ " min")
            }
        }
        tv_item_downtime_total.text = ""+total_downtime + " min"
    }

    private fun start_timer () {
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateView()
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
                view = this._inflator.inflate(R.layout.list_item_downtime, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_shift_name.text = _list[position]["shift_name"]
//            vh.tv_item_design_idx.text = _list[position]["design_idx"]
            vh.tv_item_start_time.text = _list[position]["start_dt"]
            vh.tv_item_end_time.text = _list[position]["end_dt"]
            vh.tv_item_downtime.text = _list[position]["downtime"]
            vh.tv_item_completed.text = _list[position]["completed"]
            vh.tv_item_list.text = _list[position]["list"]

            if (_list[position]["completed"]=="N") {
                vh.tv_item_shift_name.setTextColor(Color.parseColor("#ff0000"))
//                vh.tv_item_design_idx.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_start_time.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_completed.setTextColor(Color.parseColor("#ff0000"))
            }
            else {
                vh.tv_item_shift_name.setTextColor(Color.parseColor("#000000"))
//                vh.tv_item_design_idx.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_start_time.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_completed.setTextColor(Color.parseColor("#000000"))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_shift_name: TextView
//            val tv_item_design_idx: TextView
            val tv_item_start_time: TextView
            val tv_item_end_time: TextView
            val tv_item_downtime: TextView
            val tv_item_completed: TextView
            val tv_item_list: TextView

            init {
                this.tv_item_shift_name = row?.findViewById<TextView>(R.id.tv_item_shift_name) as TextView
//                this.tv_item_design_idx = row?.findViewById<TextView>(R.id.tv_item_design_idx) as TextView
                this.tv_item_start_time = row?.findViewById<TextView>(R.id.tv_item_start_time) as TextView
                this.tv_item_end_time = row?.findViewById<TextView>(R.id.tv_item_end_time) as TextView
                this.tv_item_downtime = row?.findViewById<TextView>(R.id.tv_item_downtime) as TextView
                this.tv_item_completed = row?.findViewById<TextView>(R.id.tv_item_completed) as TextView
                this.tv_item_list = row?.findViewById<TextView>(R.id.tv_item_list) as TextView
            }
        }
    }
}