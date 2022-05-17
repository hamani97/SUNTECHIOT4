package com.suntech.iot.pattern.popup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.suntech.iot.pattern.PopupSelectList
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.common.Constants
import com.suntech.iot.pattern.db.DBHelperForDesign
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_piece_pair_count_edit.*
import org.joda.time.DateTime

class PiecePairCountEditActivity : BaseActivity() {

    private var _pieces = 0
    private var _pairs = 0f
    private var _defective = 0

    private var design_pieces_value: Int = 0
    private var design_pairs_value: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_piece_pair_count_edit)

        tv_design_pieces?.text = AppGlobal.instance.get_pieces_text()
        tv_design_pairs?.text = AppGlobal.instance.get_pairs_text()
        design_pieces_value = AppGlobal.instance.get_pieces_value()
        design_pairs_value = AppGlobal.instance.get_pairs_value()

        //tv_design_pieces.setOnClickListener { fetchPiecesData() }
        //tv_design_pairs.setOnClickListener { fetchPairsData() }

        // defective
        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") {
            ToastOut(this, R.string.msg_design_not_selected, true)
            _defective = 0
        } else {
            val db = DBHelperForDesign(this)
            val row = db.get(work_idx)
            if (row != null) {
                _defective = row!!["defective"].toString().toInt()
            }
        }
        tv_defective_count?.setText(_defective.toString())
        et_defective_count?.setText(_defective.toString())

        btn_defective_plus.setOnClickListener {
            _defective++
            et_defective_count?.setText(_defective.toString())
        }
        btn_defective_minus.setOnClickListener {
            if (_defective > 0) {
                _defective--
                et_defective_count?.setText(_defective.toString())
            }
        }

        btn_confirm.setOnClickListener {
            if (tv_design_pieces.text.toString() == "" || tv_design_pairs.text.toString() == "") {
                ToastOut(this, R.string.msg_require_info, true)
                return@setOnClickListener
            }
            AppGlobal.instance.set_pieces_text(tv_design_pieces.text.toString())
            AppGlobal.instance.set_pieces_value(design_pieces_value)
            AppGlobal.instance.set_pairs_text(tv_design_pairs.text.toString())
            AppGlobal.instance.set_pairs_value(design_pairs_value)

            // defective
            val work_idx = AppGlobal.instance.get_product_idx()
            if (work_idx == "") {
                ToastOut(this, R.string.msg_design_not_selected, true)
            } else {
                val db = DBHelperForDesign(this)
                val row = db.get(work_idx)
                if (row != null) {
                    val defective = row!!["defective"].toString().toInt()
                    var seq = row!!["seq"].toString().toInt()
                    if (seq == null) seq = 1
                    val cnt = (_defective - defective)

                    if (cnt != 0) {
                        // 디펙티브에서도 카운트와 같은 값을 보내달라고 요청함.
                        // 2020-12-03

//                        // 현재시간
//                        val now_millis = DateTime.now().millis
//
//                        var count_target = db.sum_target_count()            // 총 타겟
//                        val count_defective = db.sum_defective_count()      // 현재 디펙티브 값
//                        val sum_count = AppGlobal.instance.get_current_shift_actual_cnt()
//
//                        // Downtime
//                        var param_rumtime = 0
//                        var work_time = 0
//
//                        val shift_time = AppGlobal.instance.get_current_shift_time()
//                        val shift_idx =
//                            if (shift_time != null) {
//                                // 시프트 시작/끝
//                                val shift_stime_millis = OEEUtil.parseDateTime(shift_time["work_stime"].toString()).millis
//                                val shift_etime_millis = OEEUtil.parseDateTime(shift_time["work_etime"].toString()).millis
//
//                                // 휴식시간
//                                val planned1_stime_millis = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString()).millis
//                                val planned1_etime_millis = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString()).millis
//                                val planned2_stime_millis = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString()).millis
//                                val planned2_etime_millis = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString()).millis
//
//                                val planned1_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
//                                val planned2_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned2_stime_millis, planned2_etime_millis)
//
//                                // 현재까지의 작업시간
//                                work_time = ((now_millis - shift_stime_millis) / 1000).toInt() - planned1_time - planned2_time
//
//                                // Downtime
//                                val down_db = DBHelperForDownTime(this)
//                                val down_time = down_db.sum_real_millis_count()
//                                val down_target = down_db.sum_target_count()
//
//                                if (down_target > 0f) count_target -= down_target
//
//                                param_rumtime = work_time - down_time
//
//                                shift_time["shift_idx"].toString()
//                                // ctO 구하기 (현시점까지 작업시간 - 다운타임 시간)의 타겟
//                            } else {
//                                "0"
//                            }

                        val uri = "/defectivedata.php"
                        var params = listOf(
                            "mac_addr" to AppGlobal.instance.getMACAddress(),
                            "didx" to AppGlobal.instance.get_design_info_idx(),
                            "defective_idx" to "99",
                            "cnt" to cnt.toString(),
                            "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
                            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                            "factory_idx" to AppGlobal.instance.get_zone_idx(),
                            "line_idx" to AppGlobal.instance.get_line_idx(),
                            "seq" to seq

//                            "count_sum" to sum_count,               // 현 Actual
//                            "worktime" to work_time,                // 워크타임
//                            "runtime" to param_rumtime.toString(),
//                            "actualO" to sum_count.toString(),
//                            "ctO" to count_target.toString(),
//                            "defective" to (count_defective + cnt).toString(),  // defective 총수
//                            "worker" to AppGlobal.instance.get_worker_no()
                        )
                        request(this, uri, true, false, params, { result ->
                            val code = result.getString("code")
                            if (code == "00") {
                                db.updateDefective(work_idx, _defective)
                            } else {
                                ToastOut(this, result.getString("msg"), true)
                            }
                        })
                    }
                }
            }
            finish(true, 0, "ok", null)
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }

    fun parentSpaceClick(view: View) {
        var view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
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
}