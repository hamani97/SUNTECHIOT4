package com.suntech.iot.pattern

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.pattern.PopupSelectList
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForComponent
import com.suntech.iot.pattern.util.OEEUtil
import com.suntech.iot.pattern.util.UtilString.addPairText
import kotlinx.android.synthetic.main.activity_component_info.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import org.json.JSONObject
import java.util.*

class ComponentInfoActivity : BaseActivity() {

    private var _selected_wos_idx : String = ""
    private var _selected_component_idx : String = ""
    private var _selected_component_code : String = ""
//    private var _selected_size_idx : String = ""

//    private var _selected_layer_no : String = ""
    private var _selected_pair_info : String = ""

    private var _list_for_wos_adapter: ListWosAdapter? = null
    private var _list_for_wos: ArrayList<HashMap<String, String>> = arrayListOf()

    var _selected_wos_index = -1

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
        setContentView(R.layout.activity_component_info)
        initView()
        filterWosData()
        start_timer()
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
        updateView()
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(_broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    private fun updateView() {
        if (AppGlobal.instance._server_state) btn_server_state.isSelected = true
        else btn_server_state.isSelected = false

        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false

        if (AppGlobal.instance.get_compo_sort_key() == "BALANCE") {
            tv_btn_size.setTextColor(ContextCompat.getColor(this, R.color.colorWhite2))
            tv_btn_balance.setTextColor(ContextCompat.getColor(this, R.color.colorButtonOrange))
        } else {
            tv_btn_size.setTextColor(ContextCompat.getColor(this, R.color.colorButtonOrange))
            tv_btn_balance.setTextColor(ContextCompat.getColor(this, R.color.colorWhite2))
        }
    }

    private fun initView() {
        var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
        if (item == null) {
            tv_title.setText("No shift")
        } else {
//            tv_title.setText(item["shift_name"].toString() + "   " + item["available_stime"].toString() + " - " + item["available_etime"].toString())
            tv_title.setText(item["shift_name"].toString() +
                    "   " +
                    OEEUtil.parseDateTime(item["work_stime"].toString()).toString("HH:mm") +
                    " - " +
                    OEEUtil.parseDateTime(item["work_etime"].toString()).toString("HH:mm"))
        }

        _list_for_wos_adapter = ListWosAdapter(this, _list_for_wos)
        lv_wos_info.adapter = _list_for_wos_adapter

        lv_wos_info.setOnItemClickListener { adapterView, view, i, l ->
            _selected_wos_index = i
            _list_for_wos_adapter?.select(i)
            _list_for_wos_adapter?.notifyDataSetChanged()

            tv_compo_size.text = _list_for_wos[i]["size"]!!
            tv_compo_target.text = _list_for_wos[i]["target"]!!
            tv_compo_actual.text = _list_for_wos[i]["actual"]!!
        }

        // WOS name
        tv_wos_name.text = AppGlobal.instance.get_wos_name()
        tv_wos_name2.text = AppGlobal.instance.get_wos_name()

        tv_compo_wos.text = AppGlobal.instance.get_compo_wos()
        tv_compo_model.text = AppGlobal.instance.get_compo_model()
        tv_compo_style.text = AppGlobal.instance.get_compo_style()
        tv_compo_component.text = AppGlobal.instance.get_compo_component()
        tv_compo_size.text = AppGlobal.instance.get_compo_size()
        tv_compo_layer.text = AppGlobal.instance.get_compo_layer()
        tv_compo_target.text = "" + AppGlobal.instance.get_compo_target()

        // set hidden value
        _selected_wos_idx = AppGlobal.instance.get_compo_wos_idx()
        _selected_component_idx = AppGlobal.instance.get_compo_component_idx()
        _selected_pair_info = AppGlobal.instance.get_compo_pairs()
//        _selected_size_idx = AppGlobal.instance.get_compo_size_idx()

        btn_setting_confirm.setOnClickListener {
            if (tv_compo_wos.text.toString() == "" || tv_compo_model.text.toString() == "" ||
                tv_compo_style.text.toString() == "" || tv_compo_component.text.toString() == "" ||
                tv_compo_size.text.toString() == "" || tv_compo_layer.text.toString() == "") {
                Toast.makeText(this, getString(R.string.msg_require_info), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveSettingData()
        }
        btn_setting_cancel.setOnClickListener { finish() }


//        lv_wos_info.setOnItemClickListener { adapterView, view, i, l ->
//            _selected_wos_index = i
//            _list_for_wos_adapter?.select(i)
//            _list_for_wos_adapter?.notifyDataSetChanged()
//        }

        // button click
        tv_compo_wos.setOnClickListener { fetchWosData() }
        tv_compo_component.setOnClickListener { fetchComponentData() }
        tv_compo_size.setOnClickListener { fetchSizeData() }
        tv_compo_layer.setOnClickListener { fetchLayerData() }

        tv_btn_size.setOnClickListener {
            AppGlobal.instance.set_compo_sort_key("SIZE")
            tv_btn_size.setTextColor(ContextCompat.getColor(this, R.color.colorButtonOrange))
            tv_btn_balance.setTextColor(ContextCompat.getColor(this, R.color.colorWhite2))
            outputWosList()
        }
        tv_btn_balance.setOnClickListener {
            AppGlobal.instance.set_compo_sort_key("BALANCE")
            tv_btn_size.setTextColor(ContextCompat.getColor(this, R.color.colorWhite2))
            tv_btn_balance.setTextColor(ContextCompat.getColor(this, R.color.colorButtonOrange))
            outputWosList()
        }
    }

    private fun saveSettingData() {

        AppGlobal.instance.set_compo_wos(tv_compo_wos.text.toString())
        AppGlobal.instance.set_compo_model(tv_compo_model.text.toString())
        AppGlobal.instance.set_compo_style(tv_compo_style.text.toString())
        AppGlobal.instance.set_compo_component(tv_compo_component.text.toString())
        AppGlobal.instance.set_compo_size(tv_compo_size.text.toString())
//        AppGlobal.instance.set_compo_pairs(tv_compo_pairs.text.toString())
        AppGlobal.instance.set_compo_target(tv_compo_target.text.toString().toInt())

        AppGlobal.instance.set_compo_layer(tv_compo_layer.text.toString())
        AppGlobal.instance.set_compo_pairs(_selected_pair_info)

        // set hidden value
        AppGlobal.instance.set_compo_wos_idx(_selected_wos_idx)
        AppGlobal.instance.set_compo_component_idx(_selected_component_idx)
//        AppGlobal.instance.set_compo_size_idx(_selected_size_idx)

        if (_selected_wos_index > -1) {
            // 아이템을 변경한 경우 누적 카운트를 초기화 한다.
            // 같은 아이템을 선택한 경우 카운트를 유지해야 하는지 확인이 필요함
            AppGlobal.instance.set_accumulated_count(0)

            finish(true, 1, "ok", _list_for_wos[_selected_wos_index])
        } else {
            finish()
        }
    }

    private fun outputWosList() {

        // balance로 정렬
        if (AppGlobal.instance.get_compo_sort_key() == "BALANCE") {
            var sortedList = _list_for_wos.sortedWith(compareBy({ it.get("balance").toString().toInt() }))
            _list_for_wos.removeAll(_list_for_wos)
            _list_for_wos.addAll(sortedList)
        } else {
            var sortedList = _list_for_wos.sortedWith(compareBy({ it.get("size").toString().toInt() }))
            _list_for_wos.removeAll(_list_for_wos)
            _list_for_wos.addAll(sortedList)
        }

        val size = tv_compo_size.text.toString().trim()

        if (size == "") {
            _selected_wos_index = -1
//            tv_compo_model.text = ""
//            tv_compo_style.text = ""
            tv_compo_target.text = ""
            tv_compo_actual.text = "-"

        } else {
            // 선택된 항목 찾기
            for (i in 0..(_list_for_wos.size - 1)) {
                val item = _list_for_wos.get(i)
                if (size == item["size"]) {
                    _selected_wos_index = i
                    tv_compo_model.text = item["model"]
                    tv_compo_style.text = item["styleno"]
                    tv_compo_target.text = item["target"]
                    tv_compo_actual.text = item["actual"]
                    break
                }
            }
        }
        _list_for_wos_adapter?.select(_selected_wos_index)
        _list_for_wos_adapter?.notifyDataSetChanged()
    }

    private fun filterWosData() {
        _list_for_wos.removeAll(_list_for_wos)
        _selected_wos_index = -1
        _list_for_wos_adapter?.select(-1)

        val wosno = tv_compo_wos.text.toString()

        if (wosno != "") {
            var db = DBHelperForComponent(this)

            val uri = "/wos.php"
            var params = listOf(
                "code" to "wos",
                "wosno" to wosno)

            request(this, uri, false, params, { result ->
                var code = result.getString("code")
                var msg = result.getString("msg")
                if (code == "00") {
                    var list = result.getJSONArray("item")
                    for (i in 0..(list.length() - 1)) {
                        val item = list.getJSONObject(i)
                        var actual = "0"

                        val row = db.get(wosno, item.getString("size"))
                        if (row != null) actual = row["actual"].toString()

                        val balance = item.getString("target").toInt() - actual.toInt()

                        var map = hashMapOf(
                            "wosno" to item.getString("wosno"),
                            "styleno" to item.getString("styleno"),
                            "model" to item.getString("model"),
                            "size" to item.getString("size"),
                            "target" to item.getString("target"),
                            "actual" to actual,
                            "balance" to balance.toString()
                        )
                        _list_for_wos.add(map)

//                        if (size != "" && size == item.getString("size")) {
//                            _selected_wos_index = i
//                            tv_compo_model.text = item.getString("model")
//                            tv_compo_style.text = item.getString("styleno")
//                            tv_compo_target.text = item.getString("target")
//                            tv_compo_actual.text = actual
//                        }
                    }
                    outputWosList()

                } else {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            })
        }
        _list_for_wos_adapter?.notifyDataSetChanged()
    }

    private fun fetchWosData() {
        val uri = "/wos.php"
        var params = listOf("code" to "wos_list")

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                        "idx" to item.getString("idx"),
                        "wosno" to item.getString("wosno"),
                        "styleno" to item.getString("styleno"),
                        "model" to item.getString("model"),
                        "planday" to item.getString("planday")
                    )
                    lists.add(map)
                    arr.add("[ " + item.getString("planday") + " ]   " + item.getString("wosno") + "  -  " + item.getString("model"))
                }
                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_wos_idx = lists[c]["idx"] ?: ""
                        tv_compo_wos.text = lists[c]["wosno"] ?: ""
                        tv_compo_model.text = lists[c]["model"] ?: ""
                        tv_compo_style.text = lists[c]["styleno"] ?: ""
                        tv_compo_component.text = ""
                        tv_compo_size.text = ""
                        tv_compo_target.text = ""

                        filterWosData()
                    }
                })
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchComponentData() {
        if (tv_compo_wos.text.toString() == "") {
            Toast.makeText(this, getString(R.string.msg_no_setting), Toast.LENGTH_SHORT).show()
            return
        }
        val uri = "/wos.php"
        var params = listOf(
            "code" to "wos_comp",
            "wosno" to tv_compo_wos.text.toString())

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                        "idx" to item.getString("idx"),
                        "c_code" to item.getString("c_code"),
                        "c_name" to item.getString("c_name")
                    )
                    lists.add(map)
                    arr.add(item.getString("c_name"))
                }
                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_component_idx = lists[c]["idx"] ?: ""
                        _selected_component_code = lists[c]["c_code"] ?: ""
                        tv_compo_component.text = lists[c]["c_name"] ?: ""
                    }
                })
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchSizeData() {
        if (tv_compo_wos.text.toString() == "") {
            Toast.makeText(this, getString(R.string.msg_no_setting), Toast.LENGTH_SHORT).show()
            return
        }
        val uri = "/wos.php"
        var params = listOf(
            "code" to "wos_size",
            "wosno" to tv_compo_wos.text.toString())

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                        "idx" to item.getString("idx"),
                        "s_name" to item.getString("s_name"),
                        "s_target" to item.getString("s_target")
                    )
                    lists.add(map)
                    arr.add(item.getString("s_name"))
                }
                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
//                        _selected_size_idx = lists[c]["idx"] ?: ""
                        tv_compo_size.text = lists[c]["s_name"] ?: ""
                        tv_compo_target.text = lists[c]["s_target"] ?: ""
                        selectWosData()
                    }
                })
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun selectWosData() {
        var wos = tv_compo_wos.text.toString()
        var size = tv_compo_size.text.toString()
        var target = tv_compo_target.text.toString()

        if (wos == "" || size == "" || target == "") return

        for (j in 0..(_list_for_wos.size-1)) {
            val item = _list_for_wos[j]
            val wos2 = item["wosno"] ?: ""
            val size2 = item["size"] ?: ""
            val target2 = item["target"] ?: ""
            if (wos == wos2 && size == size2 && target == target2) {
                _selected_wos_index = j
                _list_for_wos_adapter?.select(j)
                _list_for_wos_adapter?.notifyDataSetChanged()
                lv_wos_info.smoothScrollToPosition(j)

                tv_compo_actual.setText(item["actual"] ?: "-")
                break
            }
        }
    }

    private fun fetchLayerData() {
        var arr: ArrayList<String> = arrayListOf<String>()
        arr.add("1/8")
        arr.add("1/4")
        arr.add("1/2")
        arr.add("1")

        val intent = Intent(this, PopupSelectList::class.java)
        intent.putStringArrayListExtra("list", arr)
        startActivity(intent, { r, c, m, d ->
            if (r) {
                tv_compo_layer.text = arr[c]
                _selected_pair_info = arr[c]
            }
        })
    }

    private fun sendPing() {
        if (AppGlobal.instance.get_server_ip() == "") return
        val uri = "/ping.php"
        request(this, uri, false, false, null, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                btn_server_state.isSelected = true
                AppGlobal.instance._server_state = true
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }, {
            btn_server_state.isSelected = false
        })
    }

    /////// 쓰레드
    private val _timer_task1 = Timer()          // 서버 접속 체크 ping test.

    private fun start_timer() {
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    sendPing()
                }
            }
        }
        _timer_task1.schedule(task1, 5000, 10000)
    }
    private fun cancel_timer () {
        _timer_task1.cancel()
    }

    private class ListWosAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

        private var _list: ArrayList<HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null
        private var _selected_index = -1

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        fun select(index:Int) { _selected_index = index }
        fun getSelected(): Int { return _selected_index }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_component_info, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            val balance = Integer.parseInt(_list[position]["target"]) - Integer.parseInt(_list[position]["actual"])

            vh.tv_item_wosno.text = _list[position]["wosno"]
            vh.tv_item_model.text = _list[position]["model"]
            vh.tv_item_size.text = _list[position]["size"]
            vh.tv_item_target.text = _list[position]["target"]
            vh.tv_item_actual.text = _list[position]["actual"]
            vh.tv_item_balance.text = balance.toString()

            if (_selected_index == position) {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
            } else if (balance <= 0) {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
            } else {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
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
}