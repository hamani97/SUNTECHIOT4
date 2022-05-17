package com.suntech.iot.pattern.popup

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDesign
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_defective_edit.*
import org.joda.time.DateTime

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
            "defective_idx" to "99",
            "cnt" to _defective.toString(),
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
//            "defective" to (count_defective + _defective).toString(),  // defective 총수
//            "worker" to AppGlobal.instance.get_worker_no()
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