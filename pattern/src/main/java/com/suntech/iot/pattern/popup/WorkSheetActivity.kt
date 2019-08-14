package com.suntech.iot.pattern.popup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
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

        list_adapter = ListAdapter(this, _list)
        lv_work_sheets.adapter = list_adapter

        lv_work_sheets.setOnItemClickListener { adapterView, view, i, l ->
            val file_url = _list[i]["file_url"].toString()

            val intent = Intent(this, WorkSheetDetailActivity::class.java)
            intent.putExtra("file_url", file_url)
            startActivity(intent)
        }

        btn_confirm.setOnClickListener {
            finish(true, 1, "ok", null)
        }
    }


    private fun fetchData() {

        val uri = "/getlist1.php"
        var params = listOf("code" to "worksheet",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "date" to DateTime().toString("yyyy-MM-dd"))

        request(this, uri, false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){

                var list = result.getJSONArray("item")

                for (i in 0..(list.length() - 1)) {

                    val item = list.getJSONObject(i)

                    var map=hashMapOf(
                        "date" to item.getString("date"),
                        "file_url" to item.getString("file_url")
                    )
                    _list.add(map)
                }
                list_adapter?.notifyDataSetChanged()

            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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

            vh.tv_item_idx.text = _list[position]["date"]
            vh.tv_item_file_url.text = _list[position]["file_url"]

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_idx: TextView
            val tv_item_file_url: TextView

            init {
                this.tv_item_idx = row?.findViewById<TextView>(R.id.tv_item_idx) as TextView
                this.tv_item_file_url = row?.findViewById<TextView>(R.id.tv_item_file_url) as TextView
            }
        }
    }
}
