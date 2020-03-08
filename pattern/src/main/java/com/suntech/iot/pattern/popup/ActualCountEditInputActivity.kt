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
import org.json.JSONObject

class ActualCountEditInputActivity : BaseActivity() {

    private var _origin_actual = 0

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

        _origin_actual = actual.toInt()

        var db = DBHelperForDesign(this)
        val row = db.get(work_idx)

        if (row == null) {
            ToastOut(this, R.string.msg_has_not_server_info, true)
            finish()
        }

        tv_work_actual?.setText(row!!["actual"].toString())
        et_defective_qty?.setText(row!!["actual"].toString())

        btn_actual_count_edit_plus.setOnClickListener {
            var value = et_defective_qty.text.toString().toInt()
            value++
            et_defective_qty.setText(value.toString())
        }
        btn_actual_count_edit_minus.setOnClickListener {
            var value = et_defective_qty.text.toString().toInt()
            if (value > 0) {
                value--
                et_defective_qty.setText(value.toString())
            }
        }
        btn_confirm.setOnClickListener {
            val value = et_defective_qty.text.toString()
            sendCountData(value, work_idx)
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }

    private fun sendCountData(count: String, work_idx: String) {

        if (AppGlobal.instance.get_server_ip()=="") {
            ToastOut(this, R.string.msg_has_not_server_info, true)
            return
        }

        var db = DBHelperForDesign(this)
        val row = db.get(work_idx)

        if (row == null) {
            ToastOut(this, R.string.msg_data_not_found, true)
            return

        } else {

            var total_actual = 0
            var count_target = 0                                // 총 타겟

//            // 토탈 카운트도 재계산
//            val total_actual = AppGlobal.instance.get_current_shift_actual_cnt()

            // 전체 디자인을 가져온다.
            var db_list = db.gets()
            for (i in 0..((db_list?.size ?: 1) - 1)) {
                val item = db_list?.get(i)
                val actual2 = item?.get("actual").toString().toInt()
                val target2 = item?.get("target").toString().toInt()
                total_actual += actual2
                count_target += target2
            }

            // 작업시작
            val actual = count.toInt()                  // 사용자가 입력한 새 Actual 값
            val inc_count = actual - _origin_actual     // 사용자가 입력한 Actual로 계산된 증분값
            val new_actual = total_actual + inc_count   // 새로 계산된 카운트 최종값
            val seq = row!!["seq"].toString().toInt()

            val count_defective = db.sum_defective_count()      // 현재 디펙티브 값

            // Downtime
            var down_time = 0
            var down_target = 0

            var work_time = 0

            var shift_idx = AppGlobal.instance.get_current_shift_idx()

            if (shift_idx == "") {
                shift_idx = "0"
            } else {
                val shift_time = AppGlobal.instance.get_current_shift_time()

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

                    // 현재까지의 작업시간
                    work_time = ((now_millis - shift_stime_millis) / 1000).toInt() - planned1_time - planned2_time

                    // Downtime
                    val down_db = DBHelperForDownTime(this)
                    val down_list = down_db.gets()
                    down_list?.forEach { item ->
                        down_time += item["real_millis"].toString().toInt()
                        down_target += item["target"].toString().toInt()
                    }

                    // ctO 구하기 (현시점까지 작업시간 - 다운타임 시간)의 타겟
                }
            }
            // 서버에서 새로운 데이터를 요청해서 생겨난 로직 끝.

            val prepare_time = 0L       // 직전의 count 와 현재 count 사이의 차이

            // 신서버용
            val uri = "/Scount.php"
            var params = listOf(
                "mac_addr" to AppGlobal.instance.getMACAddress(),
                "didx" to AppGlobal.instance.get_design_info_idx(),
                "count" to inc_count.toString(),
                "total_count" to new_actual,
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx(),
                "line_idx" to AppGlobal.instance.get_line_idx(),
                "shift_idx" to  shift_idx,
                "seq" to seq,
                "runtime" to (work_time-down_time).toString(),
                "stitching_count" to AppGlobal.instance.get_stitch(),
                "curing_sec" to "0",                                // runtime : MainActivity의 runtime_total 값
                "prepare_time" to prepare_time.toString(),          //
                "actualO" to new_actual.toString(),
                "ctO" to (count_target-down_target).toString(),
                "defective" to count_defective.toString(),
                "worker" to AppGlobal.instance.get_worker_no())

            request(this, uri, true,false, params, { result ->
                var code = result.getString("code")
                if (code == "00") {
                    // DB의 Actual 값 갱신
                    db.updateWorkActual(work_idx, actual)

                    // DownTime 초기화
                    AppGlobal.instance.set_last_received(DateTime().toString("yyyy-MM-dd HH:mm:ss"))

//                    // Total count 의 Actual 값 갱신
//                    AppGlobal.instance.set_current_shift_actual_cnt(if (new_actual>0) new_actual else 0)

                    // Report DB 값 갱신
                    // 작업 시간인지 확인용
                    val cur_shift: JSONObject?= AppGlobal.instance.get_current_shift_time()
                    if (cur_shift != null) {
                        val shift_idx = cur_shift["shift_idx"]
                        val now = cur_shift["date"]
                        val date = now.toString()
                        val houly = DateTime().toString("HH")

                        val report_db = DBHelperForReport(this)
                        val rep = report_db.get(date, houly, shift_idx.toString())

                        if (rep == null) {
                            report_db.add(date, houly, shift_idx.toString(), inc_count)
                        } else {
                            val idx = rep!!["idx"].toString()
                            val actual = rep!!["actual"].toString().toInt() + inc_count
                            report_db.updateActual(idx, actual)
                        }
                    }

                    ToastOut(this, result.getString("msg"))
                    finish(true, 0, "ok", null)

                } else {
                    ToastOut(this, result.getString("msg"), true)
                }
            })
        }
    }
}
