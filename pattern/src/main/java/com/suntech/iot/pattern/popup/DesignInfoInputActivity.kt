package com.suntech.iot.pattern.popup

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import kotlinx.android.synthetic.main.activity_design_info_input.*

class DesignInfoInputActivity : BaseActivity() {

    private var list_adapter: ListDesignAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_design_info_input)
        initLastDesign()
        initView()

//        updateView()
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun initView() {

        // last worker info
        list_adapter = ListDesignAdapter(this, _list)
        lv_design.adapter = list_adapter

        lv_design.setOnItemClickListener { adapterView, view, i, l ->

            list_adapter?.select(i)
            list_adapter?.notifyDataSetChanged()

            finish(true, 1, "ok", _list[i])

//            val design = _list[i]
//
//            val idx = _list[i]["idx"]
//            val model = _list[i]["model"]
//            val article = _list[i]["article"]
//            val material_way = _list[i]["material_way"]
//            val component = _list[i]["component"]
//            val ct = _list[i]["ct"]
//
//            OEEUtil.LogWrite(_list[i].toString(), "selected")

        }

        btn_confirm.setOnClickListener {
            finish(false, 0, "ok", null)
        }
    }

    private fun initLastDesign() {
        _list.removeAll(_list)
        var list = AppGlobal.instance.get_last_designs()
        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(list.length() - 1 - i)
            var design = hashMapOf(
                "idx" to item.getString("idx"),
                "model" to item.getString("model"),
                "article" to item.getString("article"),
                "material_way" to item.getString("material_way"),
                "component" to item.getString("component"),
                "ct" to item.getString("ct")
            )
            _list.add(design)
        }
        list_adapter?.notifyDataSetChanged()
    }

//    private fun updateView() {
//
//        tv_item_row0?.text = "TOTAL"
//        tv_item_row1?.text = ""
//        tv_item_row2?.text = ""
//
//        var db = DBHelperForDesign(this)
//        _list = db.gets() ?: _list
//
//        list_adapter = ProductListActivity.ListActualAdapter(this, _list)
//        lv_products.adapter = list_adapter
//
//        var total_target = 0
//        var total_actual = 0
//        var total_defective = 0
////        var total_product_rate = 0
////        var total_quality_rate = 0
//        var total_work_time = 0
//
//        Log.e("Actual Qty Edit", "---------------------------------------")
//
//        for (i in 0..(_list.size - 1)) {
//            val item = _list[i]
//            val start_dt_txt = item["start_dt"]
//            val end_dt_txt = item["end_dt"]
//            var start_dt = OEEUtil.parseDateTime(start_dt_txt)
//            var end_dt = if (end_dt_txt==null) DateTime() else OEEUtil.parseDateTime(end_dt_txt)
//
//            Log.e("Actual Qty Edit", item.toString())
//
//            var dif = end_dt.millis - start_dt.millis
//
//            val target = item["target"]?.toInt() ?: 0
//            val actual = item["actual"]?.toInt() ?: 0
//            val defective = item["defective"]?.toInt() ?: 0
//            var product_rate = ((actual.toFloat()/target.toFloat()) *100).toInt().toString()+ "%"
////            var quality_rate = (((actual.toFloat()-defective)/actual.toFloat()) *100).toInt().toString()+ "%"
//            val tmp_rate = if (actual > 0) ((actual-defective).toFloat() / actual.toFloat()) * 100 else 0.0f
//            var quality_rate = String.format("%.1f", tmp_rate)
//            quality_rate = quality_rate.replace(",", ".") + "%"//??
//            val work_time = (dif / 1000 / 60 ).toInt()
//            if (target==0) product_rate = "N/A"
//            if (target==0) quality_rate = "N/A"
//
//            total_target += target
//            total_actual += actual
//            total_defective += defective
//            total_work_time += work_time
//
//            item.put("shift_name", item["shift_name"].toString())
//            item.put("target", target.toString())
//            item.put("actual", actual.toString())
//            item.put("defective", defective.toString())
//            item.put("product_rate", product_rate)
//            item.put("quality_rate", quality_rate)
//            item.put("work_time", "" +  work_time + " min")
//        }
//
//        tv_item_row1.text = "" +  total_work_time + " min"
//        tv_item_row3.text = total_target.toString()
//        tv_item_row4.text = total_actual.toString()
//        tv_item_row5.text = "-"
//        tv_item_row6.text = total_defective.toString()
//        tv_item_row7.text = "-"
//    }

    private class ListDesignAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

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
                view = this._inflator.inflate(R.layout.list_design_info, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_idx.text = _list[position]["idx"]
            vh.tv_item_model.text = _list[position]["model"]
            vh.tv_item_article.text = _list[position]["article"]
            vh.tv_item_material.text = _list[position]["material_way"]
            vh.tv_item_component.text = _list[position]["component"]
            vh.tv_item_cycle.text = _list[position]["ct"]

            if (_selected_index==position) {
                vh.tv_item_idx.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_article.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_material.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_component.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_cycle.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
            } else {
                vh.tv_item_idx.setTextColor(ContextCompat.getColor(_context, R.color.list_item_bg_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_bg_color))
                vh.tv_item_article.setTextColor(ContextCompat.getColor(_context, R.color.list_item_bg_color))
                vh.tv_item_material.setTextColor(ContextCompat.getColor(_context, R.color.list_item_bg_color))
                vh.tv_item_component.setTextColor(ContextCompat.getColor(_context, R.color.list_item_bg_color))
                vh.tv_item_cycle.setTextColor(ContextCompat.getColor(_context, R.color.list_item_bg_color))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_idx: TextView
            val tv_item_model: TextView
            val tv_item_article: TextView
            val tv_item_material: TextView
            val tv_item_component: TextView
            val tv_item_cycle: TextView

            init {
                this.tv_item_idx = row?.findViewById<TextView>(R.id.tv_item_idx) as TextView
                this.tv_item_model = row?.findViewById<TextView>(R.id.tv_item_model) as TextView
                this.tv_item_article = row?.findViewById<TextView>(R.id.tv_item_article) as TextView
                this.tv_item_material = row?.findViewById<TextView>(R.id.tv_item_material) as TextView
                this.tv_item_component = row?.findViewById<TextView>(R.id.tv_item_component) as TextView
                this.tv_item_cycle = row?.findViewById<TextView>(R.id.tv_item_cycle) as TextView
            }
        }
    }
}
