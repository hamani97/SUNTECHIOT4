package com.suntech.iot.pattern

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.suntech.iot.pattern.base.BaseFragment
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDesign
import com.suntech.iot.pattern.popup.PiecePairCountEditActivity
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.layout_bottom_info_3.*
import kotlinx.android.synthetic.main.layout_side_menu.*
import kotlinx.android.synthetic.main.layout_top_menu.*
import org.joda.time.DateTime
import org.json.JSONObject
import kotlin.math.ceil

class CountViewFragment : BaseFragment() {

    private var is_loop: Boolean = false

    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _list_for_db: ArrayList<HashMap<String, String>> = arrayListOf()

    private var _total_target = 0
    private var _total_actual = 0

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            computeCycleTime()
            resetDefectiveCount()    // DB에서 기본값을 가져다 화면에 출력
            viewWorkInfo()
            updateView()
        }
    }

    fun resetDefectiveCount() {
        val db = DBHelperForDesign(activity)
        val count = db.sum_defective_count()
        if (count==null || count<0) {
            tv_defective_count.text = "0"
        } else {
            tv_defective_count.text = count.toString()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_count_view, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_refresh, IntentFilter("need.refresh"))
        is_loop = true
        computeCycleTime()
        fetchColorData()     // Get Color
        updateView()
        startHandler()
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(_need_to_refresh)
        is_loop = false
    }

    override fun onSelected() {
        activity?.tv_title?.visibility = View.VISIBLE

//        tv_pieces_qty?.text = "" + (activity as MainActivity).pieces_qty
//        tv_pairs_qty?.text = "" + (activity as MainActivity).pairs_qty

        // Worker info
        if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
            if (AppGlobal.instance.get_message_enable()) {
                Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
//            (activity as MainActivity).changeFragment(0)
            }
        }
        viewWorkInfo()
        computeCycleTime()
    }

    override fun initViews() {
        super.initViews()

        // Total count view
        tv_count_view_target.text = "0"
        tv_count_view_actual.text = "0"
        tv_count_view_ratio.text = "0%"

        // Server charts
        oee_progress.progress = 0
        availability_progress.progress = 0
        performance_progress.progress = 0
        quality_progress.progress = 0
        tv_oee_rate.text = "0%"
        tv_availability_rate.text = "0%"
        tv_performance_rate.text = "0%"
        tv_quality_rate.text = "0%"

        val version = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
        val verArr = version.split(".")
        tv_app_version2?.text = "Pv" + verArr[verArr.size-1]

        // End Work button
//        btn_exit.setOnClickListener {
//            Toast.makeText(activity, "Not yet available", Toast.LENGTH_SHORT).show()
//        }
        btn_init_actual.setOnClickListener {
            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
            } else if (AppGlobal.instance.get_factory() == "" || AppGlobal.instance.get_room() == "" || AppGlobal.instance.get_line() == "") {
                Toast.makeText(activity, getString(R.string.msg_no_setting), Toast.LENGTH_SHORT).show()
            } else if (AppGlobal.instance.get_design_info_idx() == "") {
                Toast.makeText(activity, getString(R.string.msg_design_not_selected), Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(activity, PiecePairCountEditActivity::class.java)
                intent.putExtra("pieces", "" + (activity as MainActivity).pieces_qty)
                intent.putExtra("pairs", "" + (activity as MainActivity).pairs_qty)
                (activity as MainActivity).startActivity(intent, { r, c, m, d ->
                    if (r) {
                        val pieces = d?.get("pieces")
                        val pairs = d?.get("pairs")
                        if (pieces != null && pieces != "") {
                            (activity as MainActivity).pieces_qty = pieces.toInt()
                            tv_pieces_qty.text = pieces.toString()
                        }
                        if (pairs != null && pairs != "") {
                            (activity as MainActivity).pairs_qty = pairs.toInt()
                            tv_pairs_qty.text = pairs.toString()
                        }
                    }
                })
            }
        }
        btn_defective_plus.setOnClickListener {
            val cur_shift: JSONObject ?= AppGlobal.instance.get_current_shift_time()

            // 작업 시간인지 확인
            if (cur_shift == null) {
                Toast.makeText(activity, getString(R.string.msg_not_start_work), Toast.LENGTH_SHORT).show()
            } else {
                val work_idx = AppGlobal.instance.get_product_idx()
                if (work_idx == "") {
                    Toast.makeText(activity, getString(R.string.msg_design_not_selected), Toast.LENGTH_SHORT).show()
                } else {
                    val db = DBHelperForDesign(activity)
                    val row = db.get(work_idx)
                    var seq = row!!["seq"].toString().toInt()
                    if (row == null || seq == null) {
                        seq = 1
                    }
                    val uri = "/defectivedata.php"
                    var params = listOf(
                        "mac_addr" to AppGlobal.instance.getMACAddress(),
                        "didx" to AppGlobal.instance.get_design_info_idx(),
                        "defective_idx" to "99",
                        "cnt" to "1",
                        "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
                        "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                        "factory_idx" to AppGlobal.instance.get_room_idx(),
                        "line_idx" to AppGlobal.instance.get_line_idx(),
                        "seq" to seq
                    )
                    getBaseActivity().request(activity, uri, true, false, params, { result ->
                        val code = result.getString("code")

                        Toast.makeText(activity, result.getString("msg"), Toast.LENGTH_SHORT).show()

                        if (code == "00") {
                            val item = db.get(work_idx)
                            val defective = if (item != null) item["defective"].toString().toInt() else 0
                            db.updateDefective(work_idx, defective + 1)
                            resetDefectiveCount()    // DB에서 기본값을 가져다 화면에 출력
                        }
                    })
                }
            }
        }
        btn_toggle_sop.setOnClickListener {
            (activity as MainActivity).workSheetToggle = true
            (activity as MainActivity).workSheetShow = false
            (activity as MainActivity).ll_worksheet_view?.visibility = View.VISIBLE
//            wv_view_main?.visibility = View.VISIBLE
            btn_toggle_sop?.visibility = View.GONE
//            wv_view_main.loadUrl(file_url)
//            (activity as MainActivity).changeFragment(2)
        }

        viewWorkInfo()
        fetchColorData()    // Get Color
    }

    fun viewWorkInfo() {
        // WOS INFO 하단 bottom
        tv_pieces?.text = AppGlobal.instance.get_pieces_info()
        tv_pairs?.text = AppGlobal.instance.get_pairs_info()
        tv_idx?.text = AppGlobal.instance.get_design_info_idx()
        tv_cycle_time?.text = AppGlobal.instance.get_cycle_time().toString()
        tv_model?.text = AppGlobal.instance.get_model()
        tv_mc_no?.text = AppGlobal.instance.get_mc_no1()
        tv_material?.text = AppGlobal.instance.get_material_way()
        tv_component?.text = AppGlobal.instance.get_component()
//        tv_model?.text = AppGlobal.instance.get_compo_model()
//        tv_component?.text = AppGlobal.instance.get_compo_component()
    }

    // 해당 시간에만 카운트 값을 변경하기 위한 변수
    // 타이밍 값을 미리 계산해 놓는다.
    var _current_cycle_time = 300   // 5분

    // Total target을 표시할 사이클 타임을 계산한다.
    private fun computeCycleTime() {
        val target_type = AppGlobal.instance.get_target_type()  // setting menu 메뉴에서 선택한 타입
        if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
            _current_cycle_time = AppGlobal.instance.get_cycle_time()
            if (_current_cycle_time == 0 ) _current_cycle_time = 30
            else if (_current_cycle_time < 10) _current_cycle_time = 10        // 너무 자주 리프레시 되는걸 막기위함 (10초)
        } else {
            _current_cycle_time = 180   // 3분
        }
        Log.e("Count Time", "Current time = " + _current_cycle_time.toString())
    }
//    private fun computeCycleTime() {
//        force_count = true
//        val target = AppGlobal.instance.get_current_shift_target_cnt()
//        if (target == null || target == "") {
//            // 작업 시간이 아니므로 값을 초기화 한다.
//            _current_cycle_time = 15
//            _total_target = 0
//            return
//        }
//
//        val total_target = target.toInt()
//        val target_type = AppGlobal.instance.get_target_type()
//
//        if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
//            val shift_total_time = AppGlobal.instance.get_current_shift_total_time()
//            _current_cycle_time = if (total_target > 0) (shift_total_time / total_target) else 0
//            if (_current_cycle_time < 5) _current_cycle_time = 5        // 너무 자주 리프레시 되는걸 막기위함
//
//        } else if (target_type=="device_per_hourly" || target_type=="server_per_hourly") {
//            _current_cycle_time = 86400
//
//        } else if (target_type=="device_per_day_total" || target_type=="server_per_day_total") {
//            _current_cycle_time = 86400
//        }
//    }

    // 무조건 계산해야 할경우 true
    var force_count = true

    private fun countTarget() {
        if (_current_cycle_time >= 86400 && force_count == false) return

        val shift_now_time = AppGlobal.instance.get_current_shift_accumulated_time()    // 현재 쉬프트의 누적 시간
        if (shift_now_time <= 0 && force_count == false) return

        if (shift_now_time % _current_cycle_time == 0 || force_count) {
//            Log.e("countTarget", "Count refresh start ===========> shift_now_time = " + shift_now_time)
//            Log.e("test -----", "shift_now_time % _current_cycle_time = " + shift_now_time % _current_cycle_time)
//            Log.e("test -----", "force_count = " + force_count)
            force_count = false

            var target = AppGlobal.instance.get_current_shift_target_cnt()
            if (target == null || target == "") target = "0"

            var total_target = target.toInt()

            val target_type = AppGlobal.instance.get_target_type()

            if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
                val target = (shift_now_time / _current_cycle_time).toInt() + 1
                _total_target = if (target > total_target) total_target else target

            } else if (target_type=="device_per_hourly" || target_type=="server_per_hourly") {
                val shift_total_time = AppGlobal.instance.get_current_shift_total_time()    // 현시프트의 총 시간
                val target_per_hour = total_target.toFloat() / shift_total_time.toFloat() * 3600    // 시간당 만들어야 할 갯수
                val target = ((shift_now_time / 3600).toInt() * target_per_hour + target_per_hour).toInt()    // 현 시간에 만들어야 할 갯수
                _total_target = if (target > total_target) total_target else target

                Log.e("test -----", "target_per_hour = " + target_per_hour + ", _total_target = " + _total_target + ", _current_cycle_time = " + _current_cycle_time)

            } else if (target_type=="device_per_day_total" || target_type=="server_per_day_total") {
                _total_target = total_target
            }
        }
    }

    // 변화를 체크하기 위함
    var last_total_target = -1
    var last_total_actual = -1

    private fun updateView() {

        // 기본 출력
        tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
        tv_pieces_qty.text = "" + (activity as MainActivity).pieces_qty
        tv_pairs_qty.text = "" + (activity as MainActivity).pairs_qty

//        tv_count_view_actual.text = "" + AppGlobal.instance.get_current_shift_actual_cnt()

        drawChartView2()


        // 현재 시프트의 휴식시간 미리 계산
        val shift_time = AppGlobal.instance.get_current_shift_time()
        if (shift_time == null) {
            refreshScreen("", 0, 0, 0)
            return
        }

//        val work_stime = shift_time["work_stime"].toString()
        val work_etime = shift_time["work_etime"].toString()
        val shift_idx = shift_time["shift_idx"].toString()

        val shift_stime = OEEUtil.parseDateTime(shift_time["work_stime"].toString())

        // 디자인이 선택되었는지 체크
        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") {
            if (AppGlobal.instance.get_message_enable() && (DateTime().millis/1000) % 10 == 0L) {  // 10초마다 출력
                Toast.makeText(activity, getString(R.string.msg_design_not_selected), Toast.LENGTH_SHORT).show()
            }
//            refreshScreen(shift_idx, 0, 0)
            return
        }

        var db = DBHelperForDesign(activity)

        // DB에서 디자인 데이터를 가져온다.
        val db_item = db.get(work_idx)
        if (db_item == null || db_item.toString() == "") {
//            refreshScreen(shift_idx, 0, 0)
            return
        }


        // 가져온 DB가 현 시프트의 정보가 아니라면 리턴
        // 문제점 있음
//        if (db_item["end_dt"].toString() != null) {
//            if (db_item["end_dt"].toString() < work_stime) {
//                Log.e("CountView", "Not work stime1. work_stime = " + work_stime)
//                Log.e("CountView", "Not work stime1. " + db_item["end_dt"].toString())
//                return
//            }
//        } else {
            // 디자인을 미리 선택할 수도 있기 때문에 이 부분을 제거
//            if (db_item["start_dt"].toString() < work_stime) {
//                Log.e("CountView", "Not work stime2. work_stime = " + work_stime)
//                Log.e("CountView", "Not work stime2. " + db_item["start_dt"].toString())
//                return
//            }
//        }


        val now = DateTime()        // 현재
        var start_dt = OEEUtil.parseDateTime(db_item?.get("start_dt").toString())    // 디자인의 시작시간
        val shift_end_dt = OEEUtil.parseDateTime(work_etime)    // 시프트의 종료 시간

        if (start_dt < shift_stime) start_dt = shift_stime

        // 설정되어 있는 휴식 시간
        val _planned1_stime = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString())
        val _planned1_etime = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString())
        val _planned2_stime = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString())
        val _planned2_etime = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString())


        // 현 디자인의 휴식 시간 계산
//        var d1 = 0
//        var d2 = 0

//        Log.e("CountView", "Debug point---")

        val target_type = AppGlobal.instance.get_target_type()  // setting menu 메뉴에서 선택한 타입
        var current_cycle_time = AppGlobal.instance.get_cycle_time()

        var shift_total_target = 0
        var total_target = 0
        var total_actual = 0

        if (target_type.substring(0, 6) == "server") {

            // 전체 디자인을 가져온다.
            var db_list = db.gets()

            for (i in 0..((db_list?.size ?: 1) - 1)) {

                val item = db_list?.get(i)
                val work_idx2 = item?.get("work_idx").toString()
                val actual2 = item?.get("actual").toString().toInt()
                val target2 = item?.get("target").toString().toInt()

                total_actual += actual2


                if (work_idx == work_idx2) {        // 현재 진행중인 디자인

                    if (current_cycle_time == 0) continue

                    // 끝나는 시간까지 계산 (시프트의 총 타겟수를 구하기 위해 무조건 계산함)
                    val d1 = AppGlobal.instance.compute_time(start_dt, shift_end_dt, _planned1_stime, _planned1_etime)
                    val d2 = AppGlobal.instance.compute_time(start_dt, shift_end_dt, _planned2_stime, _planned2_etime)

                    // 디자인의 시작부터 시프트 종료시간까지 (초)
                    val start_at_target = AppGlobal.instance.get_start_at_target()

                    val work_time = ((shift_end_dt.millis - start_dt.millis) / 1000) - d1 - d2 - start_at_target
                    val count = (work_time / current_cycle_time).toInt() + start_at_target // 현 시간에 만들어야 할 갯수

                    shift_total_target += count

                    if (target_type == "server_per_day_total") {
                        total_target += count
                        // target값이 변형되었으면 업데이트
                        if (work_idx != null && target2 != count) {
                            db.updateWorkTarget(work_idx, count, count)
                        }
                    } else if (target_type == "server_per_accumulate") {
                        val d1 = AppGlobal.instance.compute_time(start_dt, now, _planned1_stime, _planned1_etime)
                        val d2 = AppGlobal.instance.compute_time(start_dt, now, _planned2_stime, _planned2_etime)

                        // 디자인의 시작부터 현재까지 시간(초)
                        val start_at_target = AppGlobal.instance.get_start_at_target()

                        val work_time = ((now.millis - start_dt.millis) / 1000) - d1 - d2 - start_at_target
                        val count = (work_time / current_cycle_time).toInt() + start_at_target // 현 시간에 만들어야 할 갯수

                        total_target += count

                        // target값이 변형되었으면 업데이트
                        if (work_idx != null && target2 != count) {
                            db.updateWorkTarget(work_idx, count, count)
                        }
                    }

//                    if (target_type == "server_per_accumulate") {
//                        val d1 = AppGlobal.instance.compute_time(start_dt, now, _planned1_stime, _planned1_etime)
//                        val d2 = AppGlobal.instance.compute_time(start_dt, now, _planned2_stime, _planned2_etime)
//
//                        // 디자인의 시작부터 현재까지 시간(초)
//                        val work_time = ((now.millis - start_dt.millis) / 1000) - d1 - d2 -1
//
//                        val count = (work_time / current_cycle_time).toInt() + 1 // 현 시간에 만들어야 할 갯수
//                        total_target += count
//
//                        // target값이 변형되었으면 업데이트
//                        if (work_idx != null && target2 != count) {
//                            db.updateWorkTarget(work_idx, count, count)
//                        }
//
//                    } else if (target_type == "server_per_day_total") {
//                        val d1 = AppGlobal.instance.compute_time(start_dt, shift_end_dt, _planned1_stime, _planned1_etime)
//                        val d2 = AppGlobal.instance.compute_time(start_dt, shift_end_dt, _planned2_stime, _planned2_etime)
//
//                        // 디자인의 시작부터 시프트 종료시간까지 (초)
//                        val work_time = ((shift_end_dt.millis - start_dt.millis) / 1000) - d1 - d2 -1
//
//                        val count = (work_time / current_cycle_time).toInt() + 1 // 현 시간에 만들어야 할 갯수
//                        total_target += count
//
//                        // target값이 변형되었으면 업데이트
//                        if (work_idx != null && target2 != count) {
//                            db.updateWorkTarget(work_idx, count, count)
//                        }
//                    }

                } else {        // 지난 디자인 작업

                    val end_dt2 = OEEUtil.parseDateTime(item?.get("end_dt"))
                    if (end_dt2 != null) {
                        var start_dt2 = OEEUtil.parseDateTime(item?.get("start_dt"))
                        val cycle_time2 = item?.get("cycle_time").toString().toInt()

                        if (start_dt2 != null && cycle_time2 > 0) {
                            if (start_dt2 < shift_stime) start_dt2 = shift_stime

                            // 휴식 시간을 뺀 시간 계산
                            val d1 = AppGlobal.instance.compute_time(start_dt2, end_dt2, _planned1_stime, _planned1_etime)
                            val d2 = AppGlobal.instance.compute_time(start_dt2, end_dt2, _planned2_stime, _planned2_etime)

                            val start_at_target = AppGlobal.instance.get_start_at_target()

                            val work_time2 = ((end_dt2.millis - start_dt2.millis) / 1000) - d1 - d2 - start_at_target
                            val count = (work_time2 / cycle_time2).toInt() + start_at_target // 시작할때 1부터 시작이므로 1을 더함

                            total_target += count   // 현재 계산된 카운트를 더한다.
                            shift_total_target += count   // 현재 계산된 카운트를 시트프 총합에 더한다.

                            // target값이 변형되었으면 업데이트
                            if (work_idx2 != null && target2 != count) {
//                                Log.e("DB", i.toString() + " = " + item.toString())
//                                Log.e("DB", i.toString() + " = db target = " + target2 + ", new target = " + count)
                                db.updateWorkTarget(work_idx2, count, count)
                            }
                        }
                    }
                }
            }
        } else if (target_type.substring(0, 6) == "device") {
            when (shift_idx) {
                "1" -> total_target = AppGlobal.instance.get_target_manual_shift("1").toInt()
                "2" -> total_target = AppGlobal.instance.get_target_manual_shift("2").toInt()
                "3" -> total_target = AppGlobal.instance.get_target_manual_shift("3").toInt()
            }
        }

        // 값에 변화가 생겼을 때만 리프레시
        refreshScreen(shift_idx, total_actual, total_target, shift_total_target)



//        if (target_type.substring(0, 6) == "server") {
//            if (current_cycle_time == 0) return
//
//            if (target_type=="server_per_accumulate") {
//                d1 = AppGlobal.instance.compute_time(start_dt, now, _planned1_stime, _planned1_etime)
//                d2 = AppGlobal.instance.compute_time(start_dt, now, _planned2_stime, _planned2_etime)
//
//                // 디자인의 시작부터 현재까지 시간(시작 시간부터 현재 시간까지 휴식 시간을 뺀 초를 구한다)
//                var work_time = (now.millis - start_dt.millis) / 1000         // 디자인 작업 시작 시간부터 지금까지 시간(초)
//                work_time = work_time - d1 - d2
//
//                total_target = (work_time / current_cycle_time).toInt() + 1    // 현 시간에 만들어야 할 갯수
//                total_actual = db_item["actual"].toString().toInt()
//
//            } else if (target_type=="server_per_day_total") {
//                d1 = AppGlobal.instance.compute_time(start_dt, shift_end_dt, _planned1_stime, _planned1_etime)
//                d2 = AppGlobal.instance.compute_time(start_dt, shift_end_dt, _planned2_stime, _planned2_etime)
//
//                // 디자인의 시작부터 시프트 종료시간까지 (시작 시간부터 초를 구한다)
//                var work_time = (shift_end_dt.millis - start_dt.millis) / 1000         // 디자인 작업 시작 시간부터 지금까지 시간(초)
//                work_time = work_time - d1 - d2
//
//                total_target = (work_time / current_cycle_time).toInt() + 1    // 현 시간에 만들어야 할 갯수
//                total_actual = db_item["actual"].toString().toInt()
//            }
//        }
//
////        // 디자인의 시작부터 현재까지 시간
////        // 시작 시간부터 현재 시간까지 휴식 시간을 뺀 초를 구한다.
////        var work_time = (now.millis - start_dt.millis) / 1000         // 디자인 작업 시작 시간부터 지금까지 시간(초)
////        work_time = work_time - d1 - d2
////
//////        Log.e("Second", "value = " + work_time)
////
////        var total_target = (work_time / current_cycle_time).toInt() + 1    // 현 시간에 만들어야 할 갯수
////        var total_actual = db_item["actual"].toString().toInt()
//
//
//        // 값에 변화가 생겼을 때만 리프레시
//        if (total_target != last_total_target || total_actual != last_total_actual) {
//
////        // 사이클 타임이 되었을 때만 화면 리프레시
////        if (force_refresh || work_time % _current_cycle_time == 0L) {
//
//            if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
//                if (current_cycle_time == 0) return
//            }
//
//            last_total_target = total_target
//            last_total_actual = total_actual
//
//            // 현재 디자인 작업의 타겟을 업데이트한다.
//            // 아직 작업이 끝나지 않았어도 일단 저장
//            db.updateWorkTarget(work_idx, total_target, total_target)
//
//
//            // 전체 디자인을 가져온다.
//            var db_list = db.gets()
//
//            // 지난 디자인의 토탈을 구한다.
//            for (i in 0..((db_list?.size ?: 1) - 1)) {
//                val item = db_list?.get(i)
//                val end_dt2 = OEEUtil.parseDateTime(item?.get("end_dt"))
//                val target2 = item?.get("target").toString().toInt()
//                val actual2 = item?.get("actual").toString().toInt()
//                val work_idx2 = item?.get("work_idx").toString()
//
//                Log.e("DB", i.toString() + " = " + item.toString())
//
//                // 현재 진행중인 디자인이거나 종료된 디자인이 아니면 패스
//                if (work_idx != work_idx2 && end_dt2 != null) {
//
//                    if (target2 == 0) {     // 계산이 안되어 있으므로 재계산
//                        if (target_type.substring(0, 6) == "server") {
//                            val start_dt2 = OEEUtil.parseDateTime(item?.get("start_dt"))
//                            val cycle_time2 = item?.get("cycle_time").toString().toInt()
//
//                            if (start_dt2 != null && cycle_time2 > 0) {
//                                var work_time2 = (end_dt2.millis - start_dt2.millis) / 1000
//                                // 중간에 휴식 시간을 뺀 시간 계산
//                                val d1 = AppGlobal.instance.compute_time(start_dt2, end_dt2, _planned1_stime, _planned1_etime)
//                                val d2 = AppGlobal.instance.compute_time(start_dt2, end_dt2, _planned2_stime, _planned2_etime)
//                                work_time2 = work_time2 - d1 - d2
//
//                                val count = (work_time2 / cycle_time2).toInt() + 1 // 시작할때 1부터 시작이므로 1을 더함
//
//                                // target값 없데이트 다음부터 구하지 않기 위해
//                                if (work_idx2 != null) {
//                                    db.updateWorkTarget(work_idx2, count, count)
//                                }
//                                total_target += count   // 현재 계산된 카운트를 더한다.
//                            }
//                        }
//                    } else {
//                        total_target += target2  // DB에서 가져온 미리 계산된 타겟값을 더한다
//                    }
//                    total_actual += actual2
//                }
//            }
//
//            var ratio = 0
//            var ratio_txt = "N/A"
//
//            if (total_target > 0) {
//                ratio = (total_actual.toFloat() / total_target.toFloat() * 100).toInt()
//                if (ratio > 999) ratio = 999
//                ratio_txt = "" + ratio + "%"
//            }
//
//            tv_count_view_target.text = "" + total_target
//            tv_count_view_actual.text = "" + total_actual
//            tv_count_view_ratio.text = ratio_txt
//
//            var color_code = "ffffff"
//
//            for (i in 0..(_list.size - 1)) {
//                val snumber = _list[i]["snumber"]?.toInt() ?: 0
//                val enumber = _list[i]["enumber"]?.toInt() ?: 0
//                if (snumber <= ratio && enumber >= ratio) color_code = _list[i]["color_code"].toString()
//            }
//            tv_count_view_target.setTextColor(Color.parseColor("#" + color_code))
//            tv_count_view_actual.setTextColor(Color.parseColor("#" + color_code))
//            tv_count_view_ratio.setTextColor(Color.parseColor("#" + color_code))
//        }
//        countTarget()
    }

    private fun refreshScreen(shift_idx:String, total_actual:Int, total_target:Int, shift_total_target:Int) {
        // 값에 변화가 생겼을 때만 리프레시
        if (total_target != last_total_target || total_actual != last_total_actual) {
            Log.e("refreshScreen", "refresh start... shift_idx="+shift_idx + ", total_actual="+total_actual + ", total_target="+total_target + ", shift_total_target="+shift_total_target)
            var ratio = 0
            var ratio_txt = "N/A"

            if (total_target > 0) {
                ratio = (total_actual.toFloat() / total_target.toFloat() * 100).toInt()
                if (ratio > 999) ratio = 999
                ratio_txt = "" + ratio + "%"
            }

            tv_count_view_target.text = "" + total_target
            tv_count_view_actual.text = "" + total_actual
            tv_count_view_ratio.text = ratio_txt

            var color_code = "ffffff"

            for (i in 0..(_list.size - 1)) {
                val snumber = _list[i]["snumber"]?.toInt() ?: 0
                val enumber = _list[i]["enumber"]?.toInt() ?: 0
                if (snumber <= ratio && enumber >= ratio) color_code = _list[i]["color_code"].toString()
            }
            tv_count_view_target.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_actual.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_ratio.setTextColor(Color.parseColor("#" + color_code))

            //
            AppGlobal.instance.set_current_shift_actual_cnt(total_actual)
            tv_report_count?.text = "" + total_actual

            // 타겟 수량이 바뀌면 서버에 통보한다.
            if (total_target != last_total_target) {
                if (shift_idx != "") {
                    updateCurrentWorkTarget(shift_idx, total_target, shift_total_target)
                }
            }

            // 최종값 업데이트
            last_total_target = total_target
            last_total_actual = total_actual
        }
    }

    // 현재 target을 서버에 저장
    private fun updateCurrentWorkTarget(shift_idx: String, target: Int, shift_target: Int) {
            Log.e("updateCurrentWorkTarget", "total_target=" + target + ", shift_total_target=" + shift_target)
            if (target >= 0) {
                // 신서버용
                val uri = "/Starget.php"
                var params = listOf(
                    "mac_addr" to AppGlobal.instance.getMACAddress(),
                    "didx" to AppGlobal.instance.get_design_info_idx(),
                    "target" to shift_target,
                    "shift_idx" to  shift_idx
                )

                getBaseActivity().request(activity, uri, true,false, params, { result ->
                    var code = result.getString("code")
                    var msg = result.getString("msg")
                    Log.e("Starget result", "= " + msg.toString())
                    if(code != "00"){
                        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                    }
                })
            }
    }


    // 값에 변화가 생길때만 화면을 리프레쉬 하기 위한 변수
    var _availability = ""
    var _performance = ""
    var _quality = ""

    private fun drawChartView2() {
        var availability = AppGlobal.instance.get_availability()
        var performance = AppGlobal.instance.get_performance()
        var quality = AppGlobal.instance.get_quality()

        if (availability=="") availability = "0"
        if (performance=="") performance = "0"
        if (quality=="") quality = "0"


        // 값에 변화가 있을때만 갱신
        if (_availability != availability || _performance != performance || _quality != quality) {
            Log.e("drawChartView2", "old : availability="+_availability+", performance="+_performance+", quality="+_quality)
            Log.e("drawChartView2", "new : availability="+availability+", performance="+performance+", quality="+quality)

            _availability = availability
            _performance = performance
            _quality = quality

            Log.e("drawChartView2", "oee graph redraw")

            var oee = availability.toFloat() * performance.toFloat() * quality.toFloat() / 10000.0f
            var oee2 = String.format("%.1f", oee)
            oee2 = oee2.replace(",", ".")//??

            tv_oee_rate.text = oee2 + "%"
            tv_availability_rate.text = availability + "%"
            tv_performance_rate.text = performance + "%"
            tv_quality_rate.text = quality + "%"

            val oee_int = oee.toInt()
            val availability_int = ceil(availability.toFloat()).toInt()
            val performance_int = ceil(performance.toFloat()).toInt()
            val quality_int = ceil(quality.toFloat()).toInt()

            oee_progress.progress = oee_int
            availability_progress.progress = availability_int
            performance_progress.progress = performance_int
            quality_progress.progress = quality_int

            var oee_color_code = "ff0000"
            var availability_color_code = "ff0000"
            var performance_color_code = "ff0000"
            var quality_color_code = "ff0000"

            for (i in 0..(_list.size - 1)) {
                val snumber = _list[i]["snumber"]?.toInt() ?: 0
//                val enumber = _list[i]["enumber"]?.toInt() ?: 0
//                if (snumber <= oee_int && enumber >= oee_int) oee_color_code = _list[i]["color_code"].toString()
//                if (snumber <= availability_int && enumber >= availability_int) availability_color_code = _list[i]["color_code"].toString()
//                if (snumber <= performance_int && enumber >= performance_int) performance_color_code = _list[i]["color_code"].toString()
//                if (snumber <= quality_int && enumber >= quality_int) quality_color_code = _list[i]["color_code"].toString()
                if (snumber <= oee_int) oee_color_code = _list[i]["color_code"].toString()
                if (snumber <= availability_int) availability_color_code = _list[i]["color_code"].toString()
                if (snumber <= performance_int) performance_color_code = _list[i]["color_code"].toString()
                if (snumber <= quality_int) quality_color_code = _list[i]["color_code"].toString()
            }

            oee_progress.progressStartColor = Color.parseColor("#" + oee_color_code)
            oee_progress.progressEndColor = Color.parseColor("#" + oee_color_code)

            availability_progress.progressStartColor = Color.parseColor("#" + availability_color_code)
            availability_progress.progressEndColor = Color.parseColor("#" + availability_color_code)

            performance_progress.progressStartColor = Color.parseColor("#" + performance_color_code)
            performance_progress.progressEndColor = Color.parseColor("#" + performance_color_code)

            quality_progress.progressStartColor = Color.parseColor("#" + quality_color_code)
            quality_progress.progressEndColor = Color.parseColor("#" + quality_color_code)
        }
    }

//    private fun fetchServerTarget() {
////        val work_idx = AppGlobal.instance.get_work_idx()
////        var db = SimpleDatabaseHelper(activity)
////        val row = db.get(work_idx)
//
//        val uri = "/getlist1.php"
//        var params = listOf(
//            "code" to "target",
//            "line_idx" to AppGlobal.instance.get_line_idx(),
//            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
//            "date" to DateTime().toString("yyyy-MM-dd"),
//            "mac_addr" to AppGlobal.instance.getMACAddress()
//        )
//
//        getBaseActivity().request(activity, uri, false, params, { result ->
//            var code = result.getString("code")
//            var msg = result.getString("msg")
//            if(code == "00"){
//                var target_type = AppGlobal.instance.get_target_type()
//
//                if (target_type=="server_per_hourly") _total_target = result.getString("target").toInt()
//                else if (target_type=="server_per_accumulate") _total_target = result.getString("targetsum").toInt()
//                else if (target_type=="server_per_day_total") _total_target = result.getString("daytargetsum").toInt()
//                else _total_target = result.getString("targetsum").toInt()
//
//                updateView()
//            }else{
//                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
//            }
//        })
//    }


    //    var handle_cnt = 0
    fun startHandler() {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                updateView()
                checkBlink()
//                if (handle_cnt++ > 5) {
//                    handle_cnt = 0
//                    computeCycleTime()
//                }
                startHandler()
            }
        }, 1000)
    }

    var blink_cnt = 0
    private fun checkBlink() {
//        var is_toggle = false
//        if (AppGlobal.instance.get_screen_blink()) {
//            if (_current_compo_target_count != -1 || _current_compo_actual_count != -1) {
//                if (_current_compo_target_count - _current_compo_actual_count <= AppGlobal.instance.get_remain_number()) {
//                    blink_cnt = 1 - blink_cnt
//                    is_toggle = true
//                }
//            }
//        }
//        if (is_toggle && blink_cnt==1) {
//            ll_total_count.setBackgroundColor(Color.parseColor("#" + AppGlobal.instance.get_blink_color()))
//        } else {
//            ll_total_count.setBackgroundResource(R.color.colorBlack2)
//        }
    }

    // Get Color code
    private fun fetchColorData() {
        var list = AppGlobal.instance.get_color_code()

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var map=hashMapOf(
                "idx" to item.getString("idx"),
                "snumber" to item.getString("snumber"),
                "enumber" to item.getString("enumber"),
                "color_name" to item.getString("color_name"),
                "color_code" to item.getString("color_code")
            )
            _list.add(map)
        }
    }

}