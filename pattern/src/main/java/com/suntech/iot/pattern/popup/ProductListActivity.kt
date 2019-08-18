package com.suntech.iot.pattern.popup

import android.content.Context
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

class ProductListActivity : BaseActivity() {

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)
        initView()
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun initView() {
//        tv_item_row0.text = "TOTAL"
//        tv_item_row1.text = ""
//        tv_item_row2.text = ""
//
//        var db = SimpleDatabaseHelper(this)
//        _list = db.gets() ?: _list
//
//        list_adapter = ListAdapter(this, _list)
//        lv_products.adapter = list_adapter
//
//        btn_confirm.setOnClickListener {
//            finish(true, 1, "ok", null)
//        }
//
//        var total_target = 0
//        var total_actual = 0
//        var total_balance = 0
//
//        for (i in 0..(_list.size - 1)) {
//
//            val item = _list[i]
//
//            val target = item["target"]?.toInt() ?: 0
//            val actual = item["actual"]?.toInt() ?: 0
//            val balance = target - actual
//
//            total_target += target
//            total_actual += actual
//            total_balance += balance
//
//            item.put("target", target.toString())
//            item.put("actual", actual.toString())
//            item.put("balance", balance.toString())
//        }
//
//        tv_item_row3.text = total_target.toString()
//        tv_item_row4.text = total_actual.toString()
//        tv_item_row5.text = total_balance.toString()
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
                view = this._inflator.inflate(R.layout.list_item_product, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_wosno.text = _list[position]["wosno"]
            vh.tv_item_model.text = _list[position]["model"]
            vh.tv_item_size.text = _list[position]["size"]
            vh.tv_item_target.text = _list[position]["target"]
            vh.tv_item_actual.text = _list[position]["actual"]
            vh.tv_item_balance.text = _list[position]["balance"]

            if(_list[position]["work_idx"].toString() == AppGlobal.instance.get_product_idx()) {
                vh.tv_item_wosno.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_model.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_size.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_target.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_actual.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_balance.setTextColor(Color.parseColor("#ff0000"))
            } else {
                vh.tv_item_wosno.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_model.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_size.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_target.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_actual.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_balance.setTextColor(Color.parseColor("#000000"))
            }
            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_wosno: TextView
            val tv_item_model: TextView
            val tv_item_size: TextView
            val tv_item_target: TextView
            val tv_item_actual: TextView
            val tv_item_balance: TextView

            init {
                this.tv_item_wosno = row?.findViewById<TextView>(R.id.tv_item_wosno) as TextView
                this.tv_item_model = row?.findViewById<TextView>(R.id.tv_item_model) as TextView
                this.tv_item_size = row?.findViewById<TextView>(R.id.tv_item_size) as TextView
                this.tv_item_target = row?.findViewById<TextView>(R.id.tv_item_target) as TextView
                this.tv_item_actual = row?.findViewById<TextView>(R.id.tv_item_actual) as TextView
                this.tv_item_balance = row?.findViewById<TextView>(R.id.tv_item_balance) as TextView
            }
        }
    }

    class ListActualAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

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
                view = this._inflator.inflate(R.layout.list_item_product, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_shift_name.text = _list[position]["shift_name"]
            vh.tv_item_work_time.text = _list[position]["work_time"]
            vh.tv_item_design_idx.text = _list[position]["design_idx"]
            vh.tv_item_target.text = _list[position]["target"]
            vh.tv_item_actual.text = _list[position]["actual"]
            vh.tv_item_product_rate.text = _list[position]["product_rate"]
            vh.tv_item_defective.text = _list[position]["defective"]
            vh.tv_item_quality_rate.text = _list[position]["quality_rate"]

            if(_list[position]["work_idx"].toString() == AppGlobal.instance.get_product_idx()) {
                vh.tv_item_shift_name.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_work_time.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_design_idx.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_target.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_actual.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_product_rate.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_defective.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_quality_rate.setTextColor(Color.parseColor("#ff0000"))
            } else {
                vh.tv_item_shift_name.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_work_time.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_design_idx.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_target.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_actual.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_product_rate.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_defective.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_quality_rate.setTextColor(Color.parseColor("#000000"))
            }
            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_shift_name: TextView
            val tv_item_work_time: TextView
            val tv_item_design_idx: TextView
            val tv_item_target: TextView
            val tv_item_actual: TextView
            val tv_item_product_rate: TextView
            val tv_item_defective: TextView
            val tv_item_quality_rate: TextView

            init {
                this.tv_item_shift_name = row?.findViewById<TextView>(R.id.tv_item_shift_name) as TextView
                this.tv_item_work_time = row?.findViewById<TextView>(R.id.tv_item_work_time) as TextView
                this.tv_item_design_idx = row?.findViewById<TextView>(R.id.tv_item_design_idx) as TextView
                this.tv_item_target = row?.findViewById<TextView>(R.id.tv_item_target) as TextView
                this.tv_item_actual = row?.findViewById<TextView>(R.id.tv_item_actual) as TextView
                this.tv_item_product_rate = row?.findViewById<TextView>(R.id.tv_item_product_rate) as TextView
                this.tv_item_defective = row?.findViewById<TextView>(R.id.tv_item_defective) as TextView
                this.tv_item_quality_rate = row?.findViewById<TextView>(R.id.tv_item_quality_rate) as TextView
            }
        }
    }

    class ListDefectiveAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

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
                view = this._inflator.inflate(R.layout.list_item_defective, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_shift_name.text = _list[position]["shift_name"]
//            vh.tv_item_work_time.text = _list[position]["work_time"]
            vh.tv_item_design_idx.text = _list[position]["design_idx"]
//            vh.tv_item_target.text = _list[position]["target"]
            vh.tv_item_actual.text = _list[position]["actual"]
//            vh.tv_item_product_rate.text = _list[position]["product_rate"]
            vh.tv_item_defective.text = _list[position]["defective"]
//            vh.tv_item_quality_rate.text = _list[position]["quality_rate"]

            if(_list[position]["work_idx"].toString() == AppGlobal.instance.get_product_idx()) {
                vh.tv_item_shift_name.setTextColor(Color.parseColor("#ff0000"))
//                vh.tv_item_work_time.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_design_idx.setTextColor(Color.parseColor("#ff0000"))
//                vh.tv_item_target.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_actual.setTextColor(Color.parseColor("#ff0000"))
//                vh.tv_item_product_rate.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_defective.setTextColor(Color.parseColor("#ff0000"))
//                vh.tv_item_quality_rate.setTextColor(Color.parseColor("#ff0000"))
            } else {
                vh.tv_item_shift_name.setTextColor(Color.parseColor("#000000"))
//                vh.tv_item_work_time.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_design_idx.setTextColor(Color.parseColor("#000000"))
//                vh.tv_item_target.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_actual.setTextColor(Color.parseColor("#000000"))
//                vh.tv_item_product_rate.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_defective.setTextColor(Color.parseColor("#000000"))
//                vh.tv_item_quality_rate.setTextColor(Color.parseColor("#000000"))
            }
            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_shift_name: TextView
//            val tv_item_work_time: TextView
            val tv_item_design_idx: TextView
//            val tv_item_target: TextView
            val tv_item_actual: TextView
//            val tv_item_product_rate: TextView
            val tv_item_defective: TextView
//            val tv_item_quality_rate: TextView

            init {
                this.tv_item_shift_name = row?.findViewById<TextView>(R.id.tv_item_shift_name) as TextView
//                this.tv_item_work_time = row?.findViewById<TextView>(R.id.tv_item_work_time) as TextView
                this.tv_item_design_idx = row?.findViewById<TextView>(R.id.tv_item_design_idx) as TextView
//                this.tv_item_target = row?.findViewById<TextView>(R.id.tv_item_target) as TextView
                this.tv_item_actual = row?.findViewById<TextView>(R.id.tv_item_actual) as TextView
//                this.tv_item_product_rate = row?.findViewById<TextView>(R.id.tv_item_product_rate) as TextView
                this.tv_item_defective = row?.findViewById<TextView>(R.id.tv_item_defective) as TextView
//                this.tv_item_quality_rate = row?.findViewById<TextView>(R.id.tv_item_quality_rate) as TextView
            }
        }
    }
}
