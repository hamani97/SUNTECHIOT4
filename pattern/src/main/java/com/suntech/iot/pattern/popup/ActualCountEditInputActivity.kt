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
import com.suntech.iot.pattern.db.DBHelperForReport
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_actual_count_edit_input.*
import org.joda.time.DateTime

class ActualCountEditInputActivity : BaseActivity() {

    private var _origin_actual = 0f

    private var _step_actual = 0f    // 증가치

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actual_count_edit_input)
        initView()
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
        val actual = intent.getStringExtra("actual")

        val target_type = AppGlobal.instance.get_target_type()

        _origin_actual = actual.toFloat()
        _step_actual =
            if (target_type.substring(0, 6) == "cycle_") {
                AppGlobal.instance.get_pairs_value()
            } else {
                1.0f
            }

        var db = DBHelperForDesign(this)
        val row = db.get(work_idx)

        if (row == null) {
            ToastOut(this, R.string.msg_has_not_server_info, true)
            finish()
        }

        tv_work_actual?.setText(row!!["actual"].toString())
        et_actual_qty?.setText(row!!["actual"].toString())

        tv_unit_actual?.text = _step_actual.toString()

        btn_actual_count_edit_plus.setOnClickListener {
            var value = et_actual_qty.text.toString().toFloat()
            value += _step_actual
            et_actual_qty.setText(value.toString())
        }
        btn_actual_count_edit_minus.setOnClickListener {
            var value = et_actual_qty.text.toString().toFloat()
            value = if (value > _step_actual) value - _step_actual else 0f
            et_actual_qty.setText(value.toString())
        }
        btn_confirm.setOnClickListener {
            sendCountData(work_idx)
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }

    private fun sendCountData(work_idx: String) {

        if (AppGlobal.instance.get_server_ip()=="") {
            ToastOut(this, R.string.msg_has_not_server_info, true); return
        }

        val design_db = DBHelperForDesign(this)
        val row = design_db.get(work_idx)

        if (row == null) {
            ToastOut(this, R.string.msg_data_not_found, true); return
        }

        val now_target: Float = row["target"].toString().toFloat()  // 현디자인의 타겟
        val now_actual: Float = row["actual"].toString().toFloat()  // 현디자인의 액추얼

        val count = et_actual_qty.text.toString()

        val total_actual = AppGlobal.instance.get_current_shift_actual_cnt()
        var count_target = design_db.sum_target_count()            // 총 타겟
        val count_defective = design_db.sum_defective_count()      // 현재 디펙티브 값


        // 작업시작
        val actual = count.toFloat()                // 사용자가 입력한 새 Actual 값
        val inc_count = actual - _origin_actual     // 사용자가 입력한 Actual로 계산된 증분값
        val new_actual = total_actual + inc_count   // 새로 계산된 카운트 최종값
        val seq = row["seq"].toString().toInt()


        // Downtime
        var shift_total_time = 0
        var planned_time = 0
        var param_rumtime = 0
        var work_time = 0

        val shift_time = AppGlobal.instance.get_current_shift_time()
        val shift_idx =
            if (shift_time != null) {
                val now = DateTime()
                val now_millis = now.millis

                // 시프트 시작/끝
                val shift_stime_millis = OEEUtil.parseDateTime(shift_time["work_stime"].toString()).millis
                val shift_etime_millis = OEEUtil.parseDateTime(shift_time["work_etime"].toString()).millis

                // 휴식시간
                val planned1_stime_millis = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString()).millis
                val planned1_etime_millis = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString()).millis
                val planned2_stime_millis = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString()).millis
                val planned2_etime_millis = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString()).millis

                val planned1_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
                val planned2_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned2_stime_millis, planned2_etime_millis)

                shift_total_time = ((shift_etime_millis-shift_stime_millis) / 1000 ).toInt()
                planned_time = (((planned1_etime_millis-planned1_stime_millis) + (planned2_etime_millis-planned2_stime_millis)) / 1000).toInt()
//                planned_time = planned1_time + planned2_time

                // 현재까지의 작업시간
                work_time = ((now_millis - shift_stime_millis) / 1000).toInt() - planned1_time - planned2_time

                // Downtime
                val down_db = DBHelperForDownTime(this)
                val down_time = down_db.sum_real_millis_count()
                val down_target = down_db.sum_target_count()

                if (down_target > 0f) count_target -= down_target

                param_rumtime = work_time - down_time

                shift_time["shift_idx"].toString()
                // ctO 구하기 (현시점까지 작업시간 - 다운타임 시간)의 타겟
            } else {
                "0"
            }
        // 서버에서 새로운 데이터를 요청해서 생겨난 로직 끝.

        val prepare_time = 0L       // 직전의 count 와 현재 count 사이의 차이

        val actual_no = (actual / _step_actual).toInt()

        design_db.updateWorkActual(work_idx, actual, actual_no)       // Design Actual 갱신

        if (shift_time != null) {
            val now = shift_time["date"]
            val date = now.toString()
            val houly = DateTime().toString("HH")

            val report_db = DBHelperForReport(this)
            val rep = report_db.get(date, houly, shift_idx)
            if (rep == null) {
                report_db.add(date, houly, shift_idx, inc_count)
            } else {
                val idx = rep!!["idx"].toString()
                val actual2 = rep!!["actual"].toString().toFloat() + inc_count
                report_db.updateActual(idx, actual2)
            }
        }

        AppGlobal.instance.set_last_count_received()    // DownTime 초기화
        AppGlobal.instance.set_first_count(true)

        val uri = "/hwi/query.php"
        val params = listOf(
            "code" to "send_count",
            "mac" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "count_sum" to new_actual,
            "shift_idx" to  shift_idx,
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "worktime" to work_time,
            "runtime" to param_rumtime.toString(),
            "actualO" to new_actual.toString(),
            "ctO" to count_target.toString(),
            "defective" to count_defective.toString(),
            "worker" to AppGlobal.instance.get_worker_no(),
            "available_time" to (shift_total_time/60).toString(),
            "planned_stop_time" to (planned_time/60).toString(),
            "target" to now_target.toString(),
            "actual" to now_actual.toString())

        // 신서버용
//        val uri = "/Scount.php"
//        var params = listOf(
//            "mac_addr" to AppGlobal.instance.getMACAddress(),
//            "didx" to AppGlobal.instance.get_design_info_idx(),
//            "count" to inc_count.toString(),
//            "total_count" to new_actual,
//            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
//            "factory_idx" to AppGlobal.instance.get_zone_idx(),
//            "line_idx" to AppGlobal.instance.get_line_idx(),
//            "shift_idx" to  shift_idx,
//            "seq" to seq,
//            "runtime" to param_rumtime.toString(),
//            "stitching_count" to AppGlobal.instance.get_stitch(),
//            "curing_sec" to "0",                                // runtime : MainActivity의 runtime_total 값
//            "prepare_time" to prepare_time.toString(),          //
//            "actualO" to new_actual.toString(),
//            "ctO" to count_target.toString(),
//            "defective" to count_defective.toString(),
//            "worker" to AppGlobal.instance.get_worker_no())

        request(this, uri, true,false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                finish(true, 0, "ok", null)
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }
}
