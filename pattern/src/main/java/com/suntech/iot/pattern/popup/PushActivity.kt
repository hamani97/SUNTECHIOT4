package com.suntech.iot.pattern.popup

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import kotlinx.android.synthetic.main.activity_push.*

class PushActivity : BaseActivity() {

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    public var _selected_idx =-1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_push)
        initView()
        fetchData()
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun initView() {
        btn_confirm.setOnClickListener {
            if (_selected_idx>=0) sendPush(_selected_idx)
            else finish()
        }
        btn_cancel.setOnClickListener {
            finish()
        }

        list_adapter = ListAdapter(this, _list)
        lv_products.adapter = list_adapter

        lv_products.setOnItemClickListener { adapterView, view, i, l ->
            _selected_idx = i
            list_adapter?.notifyDataSetChanged()
        }
    }

    private fun sendPush(idx:Int) {
        val item = _list[idx]
        var i = 0

        val uri = "/pushcall.php"
        var params = listOf(
            "code" to "push_text_list",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
            "machine_no" to AppGlobal.instance.get_mc_no1(),
            "mc_model" to AppGlobal.instance.get_mc_model(),
            "text_idx" to item["idx"],
            "seq" to i,
            "text" to item["text"])

Log.e("pushcall", "code=push_text_list&"+"&mac_addr="+AppGlobal.instance.getMACAddress()+
        "&factory_parent_idx=" + AppGlobal.instance.get_factory_idx()+
        "&factory_idx=" + AppGlobal.instance.get_room_idx()+
        "&line_idx=" + AppGlobal.instance.get_line_idx()+
        "&shift_idx=" +  AppGlobal.instance.get_current_shift_idx()+
        "&machine_no=" + AppGlobal.instance.get_mc_no1()+
        "&mc_model=" + AppGlobal.instance.get_mc_model()+
        "&text_idx=" + item["idx"]+
        "&seq=" + i+
        "&text=" + item["text"])

        request(this, uri, true, params, { result ->

            var code = result.getString("code")
            if(code == "00"){
                finish(true, 1, "ok", null)
            } else {
                Toast.makeText(this, result.getString("msg"), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchData() {

        val uri = "/getlist1.php"
        var params = listOf("code" to "text")

        request(this, uri, false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){

                var list = result.getJSONArray("item")

                for (i in 0..(list.length() - 1)) {

                    val item = list.getJSONObject(i)

                    var map=hashMapOf(
                        "idx" to item.getString("idx"),
                        "text" to item.getString("text")
                    )
                    _list.add(map)
                }
                list_adapter?.notifyDataSetChanged()

            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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
                view = this._inflator.inflate(R.layout.list_item_push, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_push_idx.text = _list[position]["idx"]
            vh.tv_item_push_contents.text = _list[position]["text"]

            vh.tv_item_push_idx.setTextColor(Color.parseColor("#000000"))
            vh.tv_item_push_contents.setTextColor(Color.parseColor("#000000"))
            vh.tv_item_push_check_box.isSelected = false
            if ((_context as PushActivity)._selected_idx == position) {
                vh.tv_item_push_idx.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_push_contents.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_push_check_box.isSelected = true
            }
            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_push_idx: TextView
            val tv_item_push_class_idx: TextView
            val tv_item_push_contents: TextView
            val tv_item_push_check_box: ImageView

            init {
                this.tv_item_push_idx = row?.findViewById<TextView>(R.id.tv_item_push_idx) as TextView
                this.tv_item_push_class_idx = row?.findViewById<TextView>(R.id.tv_item_push_class_idx) as TextView
                this.tv_item_push_contents = row?.findViewById<TextView>(R.id.tv_item_push_contents) as TextView
                this.tv_item_push_check_box = row?.findViewById<ImageView>(R.id.tv_item_push_check_box) as ImageView
            }
        }
    }

}
