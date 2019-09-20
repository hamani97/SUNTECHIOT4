package com.suntech.iot.pattern.popup

import android.os.Bundle
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDesign
import kotlinx.android.synthetic.main.activity_defective_input.*

class DefectiveInputActivity : BaseActivity() {

    private var list_adapter: DownTimeInputActivity.ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _selected_idx =-1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_defective_input)
        initView()
        fetchData()
    }

    private fun initView() {
        val design_idx = intent.getStringExtra("design_idx")
        val work_idx = intent.getStringExtra("work_idx")

        tv_design_idx.text = "" + design_idx
        et_defective_qty.setText("")

        list_adapter = DownTimeInputActivity.ListAdapter(this, _list)
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

        val uri = "/defectivedata.php"
        var params = listOf("mac_addr" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "defective_idx" to idx,
            "cnt" to count,
            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "seq" to seq)

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
}
