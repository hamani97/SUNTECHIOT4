package com.suntech.iot.pattern.popup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_down_time.*
import kotlinx.android.synthetic.main.list_item_downtime_total.*
import java.util.*

class DownTimeActivity : BaseActivity() {

    private var _db = DBHelperForDownTime(this)
    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    val _start_down_time_activity = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_down_time)

        btn_confirm.setOnClickListener {
            if (_db.count_for_notcompleted() > 0) {
                ToastOut(this, R.string.msg_has_notcompleted, true)
                return@setOnClickListener
            }
            finish(true, 1, "ok", null)
        }

        lv_downtimes.setOnItemClickListener { adapterView, view, i, l ->
            val local_id = _list[i]["_id"].toString()
            val idx = _list[i]["idx"].toString()
            val start_dt = _list[i]["start_dt"].toString()
            val completed = _list[i]["completed"]
            if (completed=="Y") return@setOnItemClickListener

            val intent = Intent(this, DownTimeInputActivity::class.java)
            intent.putExtra("idx", local_id)
            intent.putExtra("start_dt", start_dt)
            startActivity(intent, { r, c, m, d ->
                if (r) {
                    if (_db.count_for_notcompleted() > 0) updateView()
                    else finish()
                }
            })
        }

        updateView()
        start_timer()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(_start_down_time_activity, IntentFilter("start.downtime"))
        is_loop = true
    }

    public override fun onPause() {
        super.onPause()
        is_loop = false
        overridePendingTransition(0, 0)
        unregisterReceiver(_start_down_time_activity)
        ll_downtime_window.setBackgroundResource(R.color.colorWhite)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    override fun onBackPressed() {
        if (_db.count_for_notcompleted() > 0) {
            ToastOut(this, R.string.msg_has_notcompleted, true)
            return
        }
        super.onBackPressed()
    }

    private fun updateView() {
        _list = _db.gets() ?: _list
        list_adapter = ListAdapter(this, _list)
        lv_downtimes.adapter = list_adapter

        // 통계 (하단3줄)
//        var total_downtime = 0
        var total_downtime_sec = 0
        var total_planned_sec = 0

        Log.e("DownTime", "---------------------------------------")

        _list?.forEach { item ->
            item.put("downtime", "")
            if (item["end_dt"] != null) {
                val start_dt = OEEUtil.parseDateTime(item["start_dt"])
                val end_dt = OEEUtil.parseDateTime(item["end_dt"])
                val realtime = item["real_millis"].toString().toInt()

                val dif = end_dt.millis - start_dt.millis   // 총시간(밀리초)
                val total_sec = (dif / 1000).toInt()
                val min = total_sec / 60
                val sec = total_sec % 60

                item.set("downtime", "${min}m ${sec}s")

                total_downtime_sec += total_sec
                total_planned_sec += (total_sec - realtime) // 총 휴식시간(초)
            }
            Log.e("DownTime", "" + item.toString())
        }
        val min2 = total_downtime_sec / 60
        val sec2 = total_downtime_sec % 60

        tv_item_downtime_total?.text = "${min2}m ${sec2}s"

        val p_min = total_planned_sec / 60
        val p_sec = total_planned_sec % 60

        tv_item_downtime_total1?.text = "-${p_min}m ${p_sec}s"

        val final = total_downtime_sec - total_planned_sec
        val f_min = final / 60
        val f_sec = final % 60

        tv_item_downtime_total2?.text = "${f_min}m ${f_sec}s"
    }

    var blink_cnt = 0

    private fun checkBlink() {
        val count = _db.count_for_notcompleted()
        if (AppGlobal.instance.get_screen_blink()) blink_cnt = 1 - blink_cnt

        if (count > 0 && blink_cnt==1) {
            ll_downtime_window.setBackgroundColor(Color.parseColor("#" + AppGlobal.instance.get_blink_color()))
        } else {
            ll_downtime_window.setBackgroundResource(R.color.colorWhite)
        }
    }

    private val _timer_task1 = Timer()
    private val _timer_task2 = Timer()

    private var is_loop = true

    private fun start_timer() {
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (is_loop) {
                        checkBlink()
                        if (_list.size == 0) updateView()
                    }
                }
            }
        }
        _timer_task1.schedule(task1, 500, 1000)

        val task2 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (is_loop) {
                        updateView()
                    }
                }
            }
        }
        _timer_task2.schedule(task2, 1000, 10000)
    }
    private fun cancel_timer () {
        _timer_task1.cancel()
        _timer_task2.cancel()
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
//            vh.tv_item_downtime.text = _list[position]["downtime"] + "\n(" + (_list[position]["real_millis"].toString().toInt() / 60) + " m)"
                    //  + "\n" + _list[position]["real_millis"] + " sec\n" + _list[position]["target"] + " ea"
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