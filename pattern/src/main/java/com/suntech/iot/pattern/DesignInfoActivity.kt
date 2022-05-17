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
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.TextView
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.common.Constants
import com.suntech.iot.pattern.popup.DesignInfoInputActivity
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_design_info.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import org.joda.time.DateTime
import java.util.*

class DesignInfoActivity : BaseActivity() {

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _filtered_list: ArrayList<HashMap<String, String>> = arrayListOf()

    var _selected_index = -1

    private var design_pieces_value: Int = 0
    private var design_pairs_value: Float = 0f

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
        setContentView(R.layout.activity_design_info)
        initView()
        initDesign()
        start_timer()
    }

    fun parentSpaceClick(view: View) {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(et_search_design_text.windowToken, 0)
        }
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))

        btn_wifi_state.isSelected = AppGlobal.instance.isOnline(this)
        btn_server_state.isSelected = AppGlobal.instance.get_server_connect()
        is_loop = true
    }
    public override fun onPause() {
        super.onPause()
        unregisterReceiver(_broadcastReceiver)
        is_loop = false
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    private fun initView() {

        tv_title?.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))

        val list = AppGlobal.instance.get_current_work_time()
        var find_title = false
        if (list.length() > 0) {
            val now_millis = DateTime().millis
            for (i in 0..(list.length() - 1)) {
                val item = list.getJSONObject(i)
                val shift_stime = OEEUtil.parseDateTime(item["work_stime"].toString()).millis
                val shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString()).millis
                if (shift_stime <= now_millis && now_millis < shift_etime) {
                    // 타이틀 변경
                    tv_title?.setText(item["shift_name"].toString() + "   " +
                            OEEUtil.parseDateTime(item["work_stime"].toString()).toString("HH:mm") + " - " +
                            OEEUtil.parseDateTime(item["work_etime"].toString()).toString("HH:mm"))
                    find_title = true
                    break
                }
            }
        }
        if (find_title == false) {
            tv_title?.setText("No shift")
        }

        list_adapter = ListAdapter(this, _filtered_list)
        lv_design_info.adapter = list_adapter

        tv_design_pieces?.text = AppGlobal.instance.get_pieces_text()
        tv_design_pairs?.text = AppGlobal.instance.get_pairs_text()
        design_pieces_value = AppGlobal.instance.get_pieces_value()
        design_pairs_value = AppGlobal.instance.get_pairs_value()

        lv_design_info.setOnItemClickListener { adapterView, view, i, l ->
            _selected_index = i
            list_adapter?.notifyDataSetChanged()
        }

        et_search_design_text.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s != "") {
                    filterData()
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
        btn_reload.setOnClickListener {
            et_search_design_text.setText("")
            fetchDesignData()
        }
        img_last_design.setOnClickListener {
            lastDesign()
        }
        btn_last_design.setOnClickListener {
            lastDesign()
        }

        btn_setting_confirm.setOnClickListener {
            if (tv_design_pieces.text.toString() == "" || tv_design_pairs.text.toString() == "") {
                ToastOut(this, R.string.msg_require_info, true)
                return@setOnClickListener
            }

            design_pieces_value = tv_design_pieces.text.toString().toInt()
            design_pairs_value = tv_design_pairs.text.toString().toFloat()

            AppGlobal.instance.set_pieces_text(tv_design_pieces.text.toString())
            AppGlobal.instance.set_pieces_value(design_pieces_value)
            AppGlobal.instance.set_pairs_text(tv_design_pairs.text.toString())
            AppGlobal.instance.set_pairs_value(design_pairs_value)

            if (_selected_index < 0) {
                finish(false, 0, "ok", null)
            } else {
                val idx = _filtered_list[_selected_index]["idx"]!!
                val model = _filtered_list[_selected_index]["model"]!!
                val article = _filtered_list[_selected_index]["article"]!!
                val material_way = _filtered_list[_selected_index]["material_way"]!!
                val component = _filtered_list[_selected_index]["component"]!!
                val ct = _filtered_list[_selected_index]["ct"]!!

                AppGlobal.instance.push_last_design(idx, model, article, material_way, component, ct)   // history 저장

                finish(true, 1, "ok", _filtered_list[_selected_index])
            }
        }
        btn_setting_cancel.setOnClickListener {
            finish(false, 0, "ok", null)
        }

        //tv_design_pieces.setOnClickListener { fetchPiecesData() }
        //tv_design_pairs.setOnClickListener { fetchPairsData() }
    }

    fun lastDesign() {
        val intent = Intent(this, DesignInfoInputActivity::class.java)
        startActivity(intent, { r, c, m, d ->
            if (r && d!=null) {
                val idx = d!!["idx"]!!.toString()
                val model = d["model"]!!.toString()
                val article = d["article"]!!.toString()
                val material_way = d["material_way"]!!.toString()
                val component = d["component"]!!.toString()
                val ct = d["ct"]!!.toString()

                for (j in 0..(_list.size-1)) {
                    val item = _list[j]
                    val item_idx = item["idx"] ?: ""
                    val item_model = item["model"] ?: ""
                    val item_article = item["article"] ?: ""
                    val item_material_way = item["material_way"] ?: ""
                    val item_component = item["component"] ?: ""
                    val item_ct = item["ct"] ?: ""
                    if (idx == item_idx && model == item_model && article == item_article &&
                        material_way == item_material_way && component == item_component && ct == item_ct) {
                        et_search_design_text.setText("")
                        _selected_index = j
                        list_adapter?.notifyDataSetChanged()
                        lv_design_info.smoothScrollToPosition(j)
                        break
                    }
                }
//                    OEEUtil.LogWrite(d.toString(), "selected")
            }
        })
    }

    private fun initDesign() {
        var list = AppGlobal.instance.get_design_info()
        _list.removeAll(_list)
        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var map=hashMapOf(
                "idx" to item.getString("idx"),
                "model" to item.getString("model"),
                "article" to item.getString("article"),
                "stitch" to item.getString("stitch"),
                "material_way" to item.getString("material_way"),
                "component" to item.getString("component"),
                "remark" to item.getString("remark"),
                "ct" to item.getString("ct"),
                "pallet" to item.getString("pallet"),
                "pairs" to item.getString("pairs")
            )
            _list.add(map)
        }
        filterData()
    }

    // getlist1.php 에서 hwi/query.php 바뀜 요청에 의해
    // 2021-08-16
    private fun fetchDesignData() {
        //val uri = "/getlist1.php"
        val uri = "/hwi/query.php"
        val params = listOf(
            "code" to "get_designP",
            //"code" to "design",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx())

        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if (code == "00"){
                val list = result.getJSONArray("item")
                AppGlobal.instance.set_design_info(list)
                initDesign()
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun fetchPiecesData() {
        val intent = Intent(this, PopupSelectList::class.java)
        intent.putStringArrayListExtra("list", Constants.arr_pieces)
        startActivity(intent, { r, c, m, d ->
            if (r) {
                tv_design_pieces.text = Constants.arr_pieces[c]
                design_pieces_value = Constants.arr_pieces[c].toInt()
            }
        })
    }
    private fun fetchPairsData() {
        val intent = Intent(this, PopupSelectList::class.java)
        intent.putStringArrayListExtra("list", Constants.arr_pairs)
        startActivity(intent, { r, c, m, d ->
            if (r) {
                tv_design_pairs.text = Constants.arr_pairs[c]
                design_pairs_value = Constants.arr_pairs_value[c]
            }
        })
    }

    private fun filterData() {
        _filtered_list.removeAll(_filtered_list)
        _selected_index = -1

        val cur_design_idx = AppGlobal.instance.get_design_info_idx()
        val filter_text = et_search_design_text.text.toString()

        for (i in 0..(_list.size-1)) {

            val item = _list[i]
            val idx = item["idx"] ?: ""
            val model = item["model"] ?: ""
            val article = item["article"] ?: ""
            val material_way = item["material_way"] ?: ""
            val component = item["component"] ?: ""

            val a = idx.toUpperCase().contains(filter_text.toUpperCase())
            val b = model.toUpperCase().contains(filter_text.toUpperCase())
            val c = article.toUpperCase().contains(filter_text.toUpperCase())
            val d = material_way.toUpperCase().contains(filter_text.toUpperCase())
            val e = component.toUpperCase().contains(filter_text.toUpperCase())
            if (filter_text=="" || a || b || c || d || e) {
                _filtered_list.add(item)
                if (idx == cur_design_idx) _selected_index = i
            }
        }
        list_adapter?.notifyDataSetChanged()
    }

    /////// 쓰레드
    private val _timer_task2 = Timer()
    private var is_loop = true

    private fun start_timer() {
        val task2 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (is_loop) btn_usb_state2.isSelected = AppGlobal.instance.get_usb_connect()
                }
            }
        }
        _timer_task2.schedule(task2, 500, 1000)
    }
    private fun cancel_timer () {
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
            vh.tv_item_pallet.text = _list[position]["pallet"]
            vh.tv_item_pairs.text = _list[position]["pairs"]

            if ((_context as DesignInfoActivity)._selected_index==position) {
                vh.tv_item_idx.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_article.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_material.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_component.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_cycle.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_pallet.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_pairs.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                (_context as DesignInfoActivity).tv_design_pieces?.text = _list[position]["pallet"]
                (_context as DesignInfoActivity).tv_design_pairs?.text = _list[position]["pairs"]
            } else {
                vh.tv_item_idx.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_article.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_material.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_component.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_cycle.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_pallet.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_pairs.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
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
            val tv_item_pallet: TextView
            val tv_item_pairs: TextView

            init {
                this.tv_item_idx = row?.findViewById<TextView>(R.id.tv_item_idx) as TextView
                this.tv_item_model = row?.findViewById<TextView>(R.id.tv_item_model) as TextView
                this.tv_item_article = row?.findViewById<TextView>(R.id.tv_item_article) as TextView
                this.tv_item_material = row?.findViewById<TextView>(R.id.tv_item_material) as TextView
                this.tv_item_component = row?.findViewById<TextView>(R.id.tv_item_component) as TextView
                this.tv_item_cycle = row?.findViewById<TextView>(R.id.tv_item_cycle) as TextView
                this.tv_item_pallet = row?.findViewById(R.id.tv_item_pallet) as TextView
                this.tv_item_pairs = row?.findViewById(R.id.tv_item_pairs) as TextView
            }
        }
    }
}