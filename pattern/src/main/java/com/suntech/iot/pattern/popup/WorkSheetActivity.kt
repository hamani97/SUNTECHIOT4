package com.suntech.iot.pattern.popup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import kotlinx.android.synthetic.main.activity_work_sheet.*
import org.joda.time.DateTime

class WorkSheetActivity : BaseActivity() {

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_sheet)
        initView()
        fetchData()
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun initView() {

        tv_title?.text = AppGlobal.instance.get_sop_name()

        list_adapter = ListAdapter(this, _list)
        lv_work_sheets.adapter = list_adapter

        lv_work_sheets.setOnItemClickListener { adapterView, view, i, l ->
            val file_url = _list[i]["file_url"].toString()

            val intent = Intent(this, WorkSheetDetailActivity::class.java)
            intent.putExtra("file_url", file_url)
            startActivity(intent, { r, c, m, d ->
                if (r) {
                    finish(true, 1, "ok", hashMapOf("file_url" to ""+file_url))
                }
            })
        }

        btn_confirm.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }


    private fun fetchData() {

        val uri = "/getlist1.php"
        var params = listOf("code" to "worksheet",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "date" to DateTime().toString("yyyy-MM-dd"))

        request(this, uri, false, params, { result ->

            var code = result.getString("code")
            if (code == "00") {
                var list = result.getJSONArray("item")

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map=hashMapOf(
                        "date" to item.getString("date"),
                        "factory" to item.getString("factory"),
                        "zone" to item.getString("zone"),
                        "line" to item.getString("line"),
                        "machine_no" to item.getString("machine_no"),
                        "file_url" to item.getString("file_url"),
                        "file_name" to item.getString("file_name")
                    )
                    _list.add(map)
                }
                list_adapter?.notifyDataSetChanged()

            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
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
                view = this._inflator.inflate(R.layout.list_work_sheet, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_factory.text = _list[position]["factory"]
//            vh.tv_item_zone.text = _list[position]["zone"]
            vh.tv_item_line.text = _list[position]["line"]
//            vh.tv_item_machine_no.text = _list[position]["machine_no"]
            vh.tv_item_idx.text = _list[position]["date"]
            vh.tv_item_file_url.text = _list[position]["file_name"]

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_factory: TextView
//            val tv_item_zone: TextView
            val tv_item_line: TextView
//            val tv_item_machine_no: TextView
            val tv_item_idx: TextView
            val tv_item_file_url: TextView

            init {
                this.tv_item_factory = row?.findViewById<TextView>(R.id.tv_item_factory) as TextView
//                this.tv_item_zone = row?.findViewById<TextView>(R.id.tv_item_zone) as TextView
                this.tv_item_line = row?.findViewById<TextView>(R.id.tv_item_line) as TextView
//                this.tv_item_machine_no = row?.findViewById<TextView>(R.id.tv_item_machine_no) as TextView
                this.tv_item_idx = row?.findViewById<TextView>(R.id.tv_item_idx) as TextView
                this.tv_item_file_url = row?.findViewById<TextView>(R.id.tv_item_file_url) as TextView
            }
        }
    }
}
