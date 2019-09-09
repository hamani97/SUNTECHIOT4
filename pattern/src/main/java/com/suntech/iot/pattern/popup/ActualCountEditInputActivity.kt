package com.suntech.iot.pattern.popup

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDesign
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

    private fun initView() {
        val design_idx = intent.getStringExtra("design_idx")
        val work_idx = intent.getStringExtra("work_idx")
        val actual = intent.getStringExtra("actual")

        _origin_actual = actual.toInt()

        var db = DBHelperForDesign(this)
        val row = db.get(work_idx)

        if (row == null) {
            Toast.makeText(this, getString(R.string.msg_has_not_server_info), Toast.LENGTH_SHORT).show()
            finish()
        }

        tv_work_actual.setText(row!!["actual"].toString())
        et_defective_qty.setText(row!!["actual"].toString())

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
            Toast.makeText(this, getString(R.string.msg_has_not_server_info), Toast.LENGTH_SHORT).show()
            return
        }

        var db = DBHelperForDesign(this)
        val row = db.get(work_idx)

        if (row == null) {
            Toast.makeText(this, getString(R.string.msg_data_not_found), Toast.LENGTH_SHORT).show()
            return

        } else {

            var total_actual = 0

//            // 토탈 카운트도 재계산
//            val total_actual = AppGlobal.instance.get_current_shift_actual_cnt()

            // 전체 디자인을 가져온다.
            var db_list = db.gets()
            for (i in 0..((db_list?.size ?: 1) - 1)) {
                val item = db_list?.get(i)
                val actual2 = item?.get("actual").toString().toInt()
                total_actual += actual2
            }

            // 작업시작
            val actual = count.toInt()                  // 사용자가 입력한 새 Actual 값
            val inc_count = actual - _origin_actual     // 사용자가 입력한 Actual로 계산된 증분값
            val new_actual = total_actual + inc_count   // 새로 계산된 카운트 최종값
            val seq = row!!["seq"].toString().toInt()

            var shift_idx = AppGlobal.instance.get_current_shift_idx()
            if (shift_idx == "") shift_idx = "0"

            // 구서버용
//            val uri = "/senddata1.php"
//            var params = listOf(
//                "mac_addr" to AppGlobal.instance.getMACAddress(),
//                "didx" to AppGlobal.instance.get_design_info_idx(),
//                "count" to inc_count.toString(),
//                "total_count" to new_actual,
//                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
//                "factory_idx" to AppGlobal.instance.get_room_idx(),
//                "line_idx" to AppGlobal.instance.get_line_idx(),
//                "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
//                "seq" to seq,
//                "max_rpm" to "",
//                "avr_rpm" to "")

            // 신서버용
            val uri = "/Scount.php"
            var params = listOf(
                "mac_addr" to AppGlobal.instance.getMACAddress(),
                "didx" to AppGlobal.instance.get_design_info_idx(),
                "count" to inc_count.toString(),
                "total_count" to new_actual,
                "shift_idx" to  shift_idx,
                "seq" to seq)

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
                    Toast.makeText(this, result.getString("msg"), Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
