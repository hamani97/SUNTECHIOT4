package com.suntech.iot.pattern.popup

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDesign
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_defective_input.*
import org.joda.time.DateTime

class DefectiveInputActivity : BaseActivity() {

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _selected_idx =-1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_defective_input)
        initView()
        fetchData()
    }

    fun parentSpaceClick(view: View) {
        var view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun initView() {
        val design_idx = intent.getStringExtra("design_idx")
        val work_idx = intent.getStringExtra("work_idx")

        tv_design_idx?.text = "" + design_idx
        et_defective_qty?.setText("")

        list_adapter = ListAdapter(this, _list)
        lv_types.adapter = list_adapter

        lv_types.setOnItemClickListener { adapterView, view, i, l ->
            _selected_idx = i
            _list.forEach { item ->
                if (i==_list.indexOf(item)) item.set("selected", "Y")
                else item.set("selected", "N")
            }
            list_adapter?.notifyDataSetChanged()
        }

        btn_confirm.setOnClickListener {
            val value = et_defective_qty.text.toString()
            sendData(value, work_idx)
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }

    private fun fetchData() {

        val uri = "/getlist1.php"
        var params = listOf("code" to "defective")

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var list = result.getJSONArray("item")

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)

                    var map=hashMapOf(
                        "idx" to item.getString("idx"),
                        "name" to item.getString("name"),
                        "color" to item.getString("color"),
                        "selected" to "N"
                    )
                    _list.add(map)
                }
                list_adapter?.notifyDataSetChanged()

            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun sendData(count:String, work_idx:String) {

        if (AppGlobal.instance.get_server_ip()=="") {
            ToastOut(this, R.string.msg_has_not_server_info, true)
            return
        }
        if (_selected_idx < 0) {
            ToastOut(this, R.string.msg_has_notselected, true)
            return
        }

        val db = DBHelperForDesign(this)
        val row = db.get(work_idx)
        val seq = row!!["seq"].toString().toInt()

        val idx = _list[_selected_idx]["idx"]

        // 디펙티브에서도 카운트와 같은 값을 보내달라고 요청함.
        // 2020-12-03
        // 현재시간
//        val now_millis = DateTime.now().millis
//
//        var count_target = db.sum_target_count()            // 총 타겟
//        val count_defective = db.sum_defective_count()      // 현재 디펙티브 값
//        val sum_count = AppGlobal.instance.get_current_shift_actual_cnt()
//
//        // Downtime
//        var param_rumtime = 0
//        var work_time = 0
//
//        val shift_time = AppGlobal.instance.get_current_shift_time()
//        val shift_idx =
//            if (shift_time != null) {
//                // 시프트 시작/끝
//                val shift_stime_millis = OEEUtil.parseDateTime(shift_time["work_stime"].toString()).millis
//                val shift_etime_millis = OEEUtil.parseDateTime(shift_time["work_etime"].toString()).millis
//
//                // 휴식시간
//                val planned1_stime_millis = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString()).millis
//                val planned1_etime_millis = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString()).millis
//                val planned2_stime_millis = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString()).millis
//                val planned2_etime_millis = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString()).millis
//
//                val planned1_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
//                val planned2_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned2_stime_millis, planned2_etime_millis)
//
//                // 현재까지의 작업시간
//                work_time = ((now_millis - shift_stime_millis) / 1000).toInt() - planned1_time - planned2_time
//
//                // Downtime
//                val down_db = DBHelperForDownTime(this)
//                val down_time = down_db.sum_real_millis_count()
//                val down_target = down_db.sum_target_count()
//
//                if (down_target > 0f) count_target -= down_target
//
//                param_rumtime = work_time - down_time
//
//                shift_time["shift_idx"].toString()
//                // ctO 구하기 (현시점까지 작업시간 - 다운타임 시간)의 타겟
//            } else {
//                "0"
//            }

        val uri = "/defectivedata.php"
        var params = listOf(
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "defective_idx" to idx,
            "cnt" to count,
            "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "seq" to seq

//            "count_sum" to sum_count,               // 현 Actual
//            "worktime" to work_time,                // 워크타임
//            "runtime" to param_rumtime.toString(),
//            "actualO" to sum_count.toString(),
//            "ctO" to count_target.toString(),
//            "defective" to (count_defective + count.toInt()).toString(),  // defective 총수
//            "worker" to AppGlobal.instance.get_worker_no()
        )

        request(this, uri, true,false, params, { result ->
            var code = result.getString("code")

            ToastOut(this, result.getString("msg"), true)

            if (code == "00") {
                val item = db.get(work_idx)
                val defective = if (item != null) item["defective"].toString().toInt() else 0

                db.updateDefective(work_idx, defective + count.toInt())

                finish(true, 0, "ok", null)
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
                view = this._inflator.inflate(com.suntech.iot.pattern.R.layout.list_item_downtime_type, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_downtime_name.text = _list[position]["name"]
            vh.tv_item_downtime_name.setTextColor(Color.parseColor("#"+_list[position]["color"]))

            if (_list[position]["selected"]=="Y") vh.tv_item_downtime_check_box.isSelected = true
            else vh.tv_item_downtime_check_box.isSelected = false
            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_downtime_check_box: ImageView
            val tv_item_downtime_name: TextView

            init {
                this.tv_item_downtime_check_box = row?.findViewById<ImageView>(com.suntech.iot.pattern.R.id.tv_item_downtime_check_box) as ImageView
                this.tv_item_downtime_name = row?.findViewById<TextView>(com.suntech.iot.pattern.R.id.tv_item_downtime_name) as TextView
            }
        }
    }
}
