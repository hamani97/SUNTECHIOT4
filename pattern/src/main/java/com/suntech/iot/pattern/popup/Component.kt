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
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import kotlinx.android.synthetic.main.activity_component.*
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ComponentActivity : BaseActivity() {

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    val _start_activity = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_component)
        initView()
        updateView()
        fetchData()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(_start_activity, IntentFilter("start.component"))
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
        unregisterReceiver(_start_activity)
    }

    private fun initView() {
        list_adapter = ListAdapter(this, _list)
        lv_components.adapter = list_adapter

        btn_confirm.setOnClickListener {
            finish(true, 1, "ok", null)
        }
    }

    private fun updateView() {
    }

    private fun fetchData() {

        var list = AppGlobal.instance.get_comopnent_data()

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var map = hashMapOf(
                "idx" to item.getString("idx"),
                "name" to item.getString("name"),
                "total_cycle_time" to item.getString("total_cycle_time"),
                "now_cycle_time" to item.getString("now_cycle_time")
            )
            _list.add(map)
        }

        Collections.sort(_list, object : Comparator<Map<String, String>> {
            override fun compare(m1: Map<String, String>, m2: Map<String, String>): Int {
                val tct1 = m1.get("total_cycle_time")?.toInt() ?: 0
                val tct2 = m2.get("total_cycle_time")?.toInt() ?: 0
                val nct1 = m1.get("now_cycle_time")?.toInt() ?: 0
                val nct2 = m2.get("now_cycle_time")?.toInt() ?: 0
                return if ((tct1 - nct1) > (tct2 - nct2)) 1 else -1
            }
        })
        list_adapter?.notifyDataSetChanged()
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
                view = this._inflator.inflate(R.layout.list_item_component, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }
            val tct = _list[position]["total_cycle_time"].toString().toInt()
            val nct = _list[position]["now_cycle_time"].toString().toInt()

            val rt = (tct - nct)
            val rr = ((tct - nct).toFloat() / tct * 100)

            vh.tv_item_idx.text = ""+(position+1)
            vh.tv_item_name.text = _list[position]["name"]
            vh.tv_item_total_cycle_time.text =  "" + tct + "H"
            vh.tv_item_now_cycle_time.text = "" + nct + "H"

            vh.tv_item_remain_time.text = "" + rt + "H"
            vh.tv_item_remain_rate.text = "" + String.format("%.0f", rr) + "%"

            if (rt>10) {
                vh.tv_item_idx.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_name.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_total_cycle_time.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_now_cycle_time.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_remain_time.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_remain_rate.setTextColor(Color.parseColor("#000000"))
            }
            else {
                vh.tv_item_idx.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_name.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_total_cycle_time.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_now_cycle_time.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_remain_time.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_remain_rate.setTextColor(Color.parseColor("#ff0000"))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_idx: TextView
            val tv_item_name: TextView
            val tv_item_total_cycle_time: TextView
            val tv_item_now_cycle_time: TextView
            val tv_item_remain_time: TextView
            val tv_item_remain_rate: TextView

            init {
                this.tv_item_idx = row?.findViewById<TextView>(R.id.tv_item_idx) as TextView
                this.tv_item_name = row?.findViewById<TextView>(R.id.tv_item_name) as TextView
                this.tv_item_total_cycle_time = row?.findViewById<TextView>(R.id.tv_item_total_cycle_time) as TextView
                this.tv_item_now_cycle_time = row?.findViewById<TextView>(R.id.tv_item_now_cycle_time) as TextView
                this.tv_item_remain_time = row?.findViewById<TextView>(R.id.tv_item_remain_time) as TextView
                this.tv_item_remain_rate = row?.findViewById<TextView>(R.id.tv_item_remain_rate) as TextView
            }
        }
    }
}