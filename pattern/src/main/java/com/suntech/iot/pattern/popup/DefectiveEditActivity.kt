package com.suntech.iot.pattern.popup

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDesign
import kotlinx.android.synthetic.main.activity_defective_edit.*

class DefectiveEditActivity : BaseActivity() {

    private var _defective = 1
    private var _work_idx = AppGlobal.instance.get_product_idx()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_defective_edit)

        if (_work_idx == "") {
            ToastOut(this, R.string.msg_design_not_selected, true)
            finish(false, 1, "fail", null)
        }

        et_defective_count?.setText(_defective.toString())

        btn_defective_plus.setOnClickListener {
            _defective++
            et_defective_count.setText(_defective.toString())
        }
        btn_defective_minus.setOnClickListener {
            if (_defective > 1) {
                _defective--
                et_defective_count.setText(_defective.toString())
            }
        }
        btn_confirm.setOnClickListener {
            sendDefective()
//            finish(true, 0, "ok", hashMapOf("defective" to ""+_defective))
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "cancel", null)
        }
    }

    private fun sendDefective() {

        val db = DBHelperForDesign(this)
        val row = db.get(_work_idx)
        var seq = row!!["seq"].toString().toInt()
        if (row == null || seq == null) seq = 1

        val uri = "/defectivedata.php"
        var params = listOf(
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "defective_idx" to "99",
            "cnt" to _defective.toString(),
            "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "seq" to seq
        )
        request(this, uri, true, false, params, { result ->
            val code = result.getString("code")
            ToastOut(this, result.getString("msg"), true)
            if (code == "00") {
                val item = db.get(_work_idx)
                val defective = if (item != null) item["defective"].toString().toInt() else 0
                db.updateDefective(_work_idx, defective + _defective)
                finish(true, 0, "ok", null)
            }
        })
    }

    fun parentSpaceClick(view: View) {
        var view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}