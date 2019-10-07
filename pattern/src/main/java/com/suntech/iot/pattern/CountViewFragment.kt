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
import com.suntech.iot.pattern.base.BaseFragment
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDesign
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.popup.PiecePairCountEditActivity
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.layout_bottom_info_3.*
import kotlinx.android.synthetic.main.layout_side_menu.*
import kotlinx.android.synthetic.main.layout_top_menu.*
import org.joda.time.DateTime
import org.json.JSONObject
import kotlin.math.floor

class CountViewFragment : BaseFragment() {

    private var is_loop: Boolean = false

    private var _color_list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _list_for_db: ArrayList<HashMap<String, String>> = arrayListOf()

    private var _total_target = 0
    private var _total_actual = 0

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            resetDefectiveCount()    // DB에서 기본값을 가져다 화면에 출력
            computeCycleTime()
            viewWorkInfo()
            updateView()
        }
    }

    fun resetDefectiveCount() {
        val db = DBHelperForDesign(activity)
        val count = db.sum_defective_count()
        tv_defective_count.text = if (count<0) "0" else count.toString()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_count_view, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_refresh, IntentFilter("need.refresh"))
        is_loop = true
        computeCycleTime()
        fetchColorData()
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

        // Worker info
        if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
            (activity as MainActivity).ToastOut(activity, R.string.msg_no_operator)
        }
        viewWorkInfo()
        computeCycleTime()
    }

    override fun initViews() {
        super.initViews()

        // Total count view
        tv_count_view_target?.text = "0"
        tv_count_view_actual?.text = "0"
        tv_count_view_ratio?.text = "0%"

        // Server charts
        initOEEGraph()
//        oee_progress.progress = 0
//        availability_progress.progress = 0
//        performance_progress.progress = 0
//        quality_progress.progress = 0
//        tv_oee_rate.text = "0%"
//        tv_availability_rate.text = "0%"
//        tv_performance_rate.text = "0%"
//        tv_quality_rate.text = "0%"

        val version = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
        val verArr = version.split(".")
        tv_app_version2?.text = "Pv" + verArr[verArr.size-2] + "." + verArr[verArr.size-1]

        btn_init_actual.setOnClickListener {
            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                (activity as MainActivity).ToastOut(activity, R.string.msg_no_operator, true)
            } else if (AppGlobal.instance.get_factory() == "" || AppGlobal.instance.get_room() == "" || AppGlobal.instance.get_line() == "") {
                (activity as MainActivity).ToastOut(activity, R.string.msg_no_setting, true)
            } else if (AppGlobal.instance.get_design_info_idx() == "") {
                (activity as MainActivity).ToastOut(activity, R.string.msg_design_not_selected, true)
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
                (activity as MainActivity).ToastOut(activity, R.string.msg_not_start_work, true)
            } else {
                val work_idx = AppGlobal.instance.get_product_idx()
                if (work_idx == "") {
                    (activity as MainActivity).ToastOut(activity, R.string.msg_design_not_selected, true)
                } else {
                    val db = DBHelperForDesign(activity)
                    val row = db.get(work_idx)
                    var seq = row!!["seq"].toString().toInt()
                    if (row == null || seq == null) seq = 1

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
                        (activity as MainActivity).ToastOut(activity, result.getString("msg"), true)
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
            btn_toggle_sop?.visibility = View.GONE
        }

        viewWorkInfo()
//        fetchColorData()
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

    // 무조건 계산해야 할경우 true
//    var force_count = true
//
//    private fun countTarget() {
//        if (_current_cycle_time >= 86400 && force_count == false) return
//
//        val shift_now_time = AppGlobal.instance.get_current_shift_accumulated_time()    // 현재 쉬프트의 누적 시간
//        if (shift_now_time <= 0 && force_count == false) return
//
//        if (shift_now_time % _current_cycle_time == 0 || force_count) {
////            Log.e("countTarget", "Count refresh start ===========> shift_now_time = " + shift_now_time)
////            Log.e("test -----", "shift_now_time % _current_cycle_time = " + shift_now_time % _current_cycle_time)
////            Log.e("test -----", "force_count = " + force_count)
//            force_count = false
//
//            var target = AppGlobal.instance.get_current_shift_target_cnt()
//            if (target == null || target == "") target = "0"
//
//            var total_target = target.toInt()
//
//            val target_type = AppGlobal.instance.get_target_type()
//
//            if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
//                val target = (shift_now_time / _current_cycle_time).toInt() + 1
//                _total_target = if (target > total_target) total_target else target
//
//            } else if (target_type=="device_per_hourly" || target_type=="server_per_hourly") {
//                val shift_total_time = AppGlobal.instance.get_current_shift_total_time()    // 현시프트의 총 시간
//                val target_per_hour = total_target.toFloat() / shift_total_time.toFloat() * 3600    // 시간당 만들어야 할 갯수
//                val target = ((shift_now_time / 3600).toInt() * target_per_hour + target_per_hour).toInt()    // 현 시간에 만들어야 할 갯수
//                _total_target = if (target > total_target) total_target else target
//
//                Log.e("test -----", "target_per_hour = " + target_per_hour + ", _total_target = " + _total_target + ", _current_cycle_time = " + _current_cycle_time)
//
//            } else if (target_type=="device_per_day_total" || target_type=="server_per_day_total") {
//                _total_target = total_target
//            }
//        }
//    }

    // 변화를 체크하기 위함
    var last_total_target = -1
    var last_total_actual = -1

    private fun updateView() {

        // 기본 출력
        tv_current_time?.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
        tv_pieces_qty?.text = "" + (activity as MainActivity).pieces_qty
        tv_pairs_qty?.text = "" + (activity as MainActivity).pairs_qty

//        tv_count_view_actual.text = "" + AppGlobal.instance.get_current_shift_actual_cnt()
//        drawChartView2()

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
            if ((DateTime().millis/1000) % 10 == 0L) {  // 10초마다 출력
                (activity as MainActivity).ToastOut(activity, R.string.msg_design_not_selected)
            }
            refreshScreen(shift_idx, 0, 0, 0)
            return
        }

        var db = DBHelperForDesign(activity)

        // DB에서 디자인 데이터를 가져온다.
        val db_item = db.get(work_idx)
        if (db_item == null || db_item.toString() == "") {
//            refreshScreen(shift_idx, 0, 0, 0)
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
        var start_dt = OEEUtil.parseDateTime(db_item?.get("start_dt").toString())   // 디자인의 시작시간
        val shift_end_dt = OEEUtil.parseDateTime(work_etime)                        // 시프트의 종료 시간

        if (start_dt < shift_stime) start_dt = shift_stime

        // 설정되어 있는 휴식 시간
        val _planned1_stime = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString())
        val _planned1_etime = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString())
        val _planned2_stime = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString())
        val _planned2_etime = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString())


//        Log.e("CountView", "Debug point---")

        val start_at_target = AppGlobal.instance.get_start_at_target()  // 타겟의 시작을 0부터 할지 1부터 할지
        val target_type = AppGlobal.instance.get_target_type()          // setting menu 메뉴에서 선택한 타입
        var current_cycle_time = AppGlobal.instance.get_cycle_time()

        var shift_total_target = 0      // 시프트의 총 타겟
        var total_target = 0            // 현시점까지 타겟
        var total_actual = 0            // 현시점까지 액추얼

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
                    val work_time = ((shift_end_dt.millis - start_dt.millis) / 1000) - d1 - d2 - start_at_target
                    val count = (work_time / current_cycle_time).toInt() + start_at_target // 현 디자인의 시프트 종료까지 만들어야 할 갯수

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
                        val work_time = ((now.millis - start_dt.millis) / 1000) - d1 - d2 - start_at_target
                        val count = (work_time / current_cycle_time).toInt() + start_at_target // 현 시간에 만들어야 할 갯수

                        total_target += count

                        // target값이 변형되었으면 업데이트
                        if (work_idx != null && target2 != count) {
                            db.updateWorkTarget(work_idx, count, count)
                        }
                    }

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

        if (AppGlobal.instance.get_target_stop_when_downtime()) {
            // Downtime
            val down_db = DBHelperForDownTime(activity)
            val down_list = down_db.gets()
            var down_target = 0
            down_list?.forEach { item ->
                down_target += item["target"].toString().toInt()
            }
            total_target -= down_target
        }

        refreshScreen(shift_idx, total_actual, total_target, shift_total_target)
    }

    // 값에 변화가 생겼을 때만 리프레시
    private fun refreshScreen(shift_idx:String, total_actual:Int, total_target:Int, shift_total_target:Int) {

        // 값에 변화가 생겼을 때만 다시 그림
        if (total_target != last_total_target || total_actual != last_total_actual) {

            OEEUtil.LogWrite("shift_idx="+shift_idx + ", total_actual="+total_actual + ", total_target="+total_target + ", shift_total_target="+shift_total_target, "Refresh Start")

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

            for (i in 0..(_color_list.size - 1)) {
                val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                val enumber = _color_list[i]["enumber"]?.toInt() ?: 0
                if (snumber <= ratio && enumber >= ratio) color_code = _color_list[i]["color_code"].toString()
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

        // OEE graph
        refreshOEEGraph(total_actual, total_target)
    }

//    var _availability_rate = 0F
//    var _quality_rate = 0F
//    var _performance_rate = 0F
//    var _oee_rate = 0F

    private fun initOEEGraph() {
        (activity as MainActivity)._availability_rate = 0F
        (activity as MainActivity)._quality_rate = 0F
        (activity as MainActivity)._performance_rate = 0F
        (activity as MainActivity)._oee_rate = 0F

        // Server charts
        oee_progress.progress = 0
        availability_progress.progress = 0
        performance_progress.progress = 0
        quality_progress.progress = 0
        tv_oee_rate.text = "0%"
        tv_availability_rate.text = "0%"
        tv_performance_rate.text = "0%"
        tv_quality_rate.text = "0%"
    }

    private fun refreshOEEGraph(total_actual:Int, total_target:Int) {

        val shift_time = AppGlobal.instance.get_current_shift_time()
        if (shift_time == null) {
            initOEEGraph()
            return
        }

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
        val work_time = ((now_millis - shift_stime_millis) / 1000) - planned1_time - planned2_time


        // Downtime
        var down_time = 0
        var down_target = 0

        var down_db = DBHelperForDownTime(activity)
        val down_list = down_db.gets()
        down_list?.forEach { item ->
            down_time += item["real_millis"].toString().toInt()
            down_target += item["target"].toString().toInt()
        }

//        Log.e("refreshOEEGraph", "downtime min : = " + time_sum.toInt()/60 + "min , sec = " + time_sum.toInt())

//        Log.e("test", "---------- work time : " + work_time +", down time : " + down_time +", down target : " + down_target)

        // Availability Check
        // availity = (현시점까지 작업시간 - 다운타임 시간) / 현시점까지 작업시간(초)
        val availability = (work_time-down_time).toFloat() / work_time
        val availability_rate = floor(availability * 1000) / 10

        if ((activity as MainActivity)._availability_rate != availability_rate) {
            (activity as MainActivity)._availability_rate = availability_rate

            Log.e("refreshOEEGraph", "oee graph redraw : availability = " + (availability * 100) + "%")

            val availability_int = floor(availability_rate).toInt()
            var availability_color_code = "ff0000"

            for (i in 0..(_color_list.size - 1)) {
                val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                if (snumber <= availability_int) availability_color_code = _color_list[i]["color_code"].toString()
            }
            tv_availability_rate.text = "" + availability_rate + "%"
            availability_progress.progress = availability_int
            availability_progress.progressStartColor = Color.parseColor("#" + availability_color_code)
            availability_progress.progressEndColor = Color.parseColor("#" + availability_color_code)
        }


        // Performance Check
        // performance = 현재까지의 Actual / (현시점까지 작업시간 - 다운타임 시간)의 타겟
        (work_time-down_time) / _current_cycle_time


        val performance = if (AppGlobal.instance.get_target_stop_when_downtime()) {
            if (total_target > 0) total_actual.toFloat() / total_target else 0F
        } else {
            if (total_target-down_target > 0) total_actual.toFloat() / (total_target-down_target) else 0F
        }

        val performance_rate = floor(performance * 1000) / 10

        if ((activity as MainActivity)._performance_rate != performance_rate) {

            // 100% 넘어가면 푸시발송
            if (performance_rate >= 100.0f) {
                if ((activity as MainActivity)._performance_rate < 100.0f) {
                    OEEUtil.LogWrite("Best performance Push send...", "refreshOEEGraph")
                    (activity as MainActivity).sendPush("SYS: PERFORMANCE")
                }
            }
            (activity as MainActivity)._performance_rate = performance_rate

            Log.e("refreshOEEGraph", "oee graph redraw : performance = " + (performance * 100) + "%")

            val performance_int = floor(performance_rate).toInt()
            var performance_color_code = "ff0000"

            for (i in 0..(_color_list.size - 1)) {
                val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                if (snumber <= performance_int) performance_color_code = _color_list[i]["color_code"].toString()
            }
            tv_performance_rate.text = "" + performance_rate + "%"
            performance_progress.progress = performance_int
            performance_progress.progressStartColor = Color.parseColor("#" + performance_color_code)
            performance_progress.progressEndColor = Color.parseColor("#" + performance_color_code)
        }


        // Quality Check
        // qulity = (현시점의 actual - defective) / Actual
        val db = DBHelperForDesign(activity)
        var defective_count = db.sum_defective_count()
        if (defective_count==null || defective_count<0) defective_count = 0

        val quality = if(total_actual!=0) (total_actual-defective_count).toFloat() / total_actual else 0F
        val quality_rate = floor(quality * 1000) / 10

        if ((activity as MainActivity)._quality_rate != quality_rate) {
            (activity as MainActivity)._quality_rate = quality_rate

            Log.e("refreshOEEGraph", "oee graph redraw : quality = " + (quality*100) + "%")

            val quality_int = floor(quality_rate).toInt()
            var quality_color_code = "ff0000"

            for (i in 0..(_color_list.size - 1)) {
                val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                if (snumber <= quality_int) quality_color_code = _color_list[i]["color_code"].toString()
            }
//            OEEUtil.LogWrite("Qual value = "+quality_int+", Qual color = "+quality_color_code, "Color")
            tv_quality_rate.text = "" + quality_rate + "%"
            quality_progress.progress = quality_int
            quality_progress.progressStartColor = Color.parseColor("#" + quality_color_code)
            quality_progress.progressEndColor = Color.parseColor("#" + quality_color_code)
        }


        // OEE Check
        var oee = availability_rate * performance_rate * quality_rate / 1000F
        var oee_rate = floor(oee) / 10

        if ((activity as MainActivity)._oee_rate != oee_rate) {
            (activity as MainActivity)._oee_rate = oee_rate

            Log.e("refreshOEEGraph", "oee graph redraw : OEE = " + (oee/10) + "%")

            val oee_int = floor(oee_rate).toInt()
            var oee_color_code = "ff0000"

            for (i in 0..(_color_list.size - 1)) {
                val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                if (snumber <= oee_int) oee_color_code = _color_list[i]["color_code"].toString()
            }
//            OEEUtil.LogWrite("OEE value = "+oee_int+", OEE color = "+oee_color_code, "Color")
            tv_oee_rate.text = "" + oee_rate + "%"
            oee_progress.progress = oee_int
            oee_progress.progressStartColor = Color.parseColor("#" + oee_color_code)
            oee_progress.progressEndColor = Color.parseColor("#" + oee_color_code)
        }
    }

    // 현재 target을 서버에 저장
    // shift idx, 현재까지 만들 target, 시프트의 총 target
    private fun updateCurrentWorkTarget(shift_idx: String, target: Int, shift_target: Int) {
            Log.e("updateCurrentWorkTarget", "total_target=" + target + ", shift_total_target=" + shift_target)
            if (target >= 0) {
                // 신서버용
                val uri = "/Starget.php"
                var params = listOf(
                    "mac_addr" to AppGlobal.instance.getMACAddress(),
                    "didx" to AppGlobal.instance.get_design_info_idx(),
                    "now_target" to target,
                    "target" to shift_target,
                    "shift_idx" to  shift_idx
                )
                getBaseActivity().request(activity, uri, true,false, params, { result ->
                    var code = result.getString("code")
                    Log.e("Starget result", "= " + result.getString("msg").toString())
                    if(code != "00"){
                        (activity as MainActivity).ToastOut(activity, result.getString("msg"), true)
                    }
                })
            }
    }


    // 값에 변화가 생길때만 화면을 리프레쉬 하기 위한 변수
//    var _availability = ""
//    var _performance = ""
//    var _quality = ""
//
//    private fun drawChartView2() {
//        var availability = AppGlobal.instance.get_availability()
//        var performance = AppGlobal.instance.get_performance()
//        var quality = AppGlobal.instance.get_quality()
//
//        if (availability=="") availability = "0"
//        if (performance=="") performance = "0"
//        if (quality=="") quality = "0"
//
//        // 값에 변화가 있을때만 갱신
//        if (_availability != availability || _performance != performance || _quality != quality) {
//            Log.e("drawChartView2", "old : availability="+_availability+", performance="+_performance+", quality="+_quality)
//            Log.e("drawChartView2", "new : availability="+availability+", performance="+performance+", quality="+quality)
//
//            _availability = availability
//            _performance = performance
//            _quality = quality
//
//            Log.e("drawChartView2", "oee graph redraw")
//
//            var oee = availability.toFloat() * performance.toFloat() * quality.toFloat() / 10000.0f
//            var oee2 = String.format("%.1f", oee)
//            oee2 = oee2.replace(",", ".")//??
//
//            tv_oee_rate.text = oee2 + "%"
//            tv_availability_rate.text = availability + "%"
//            tv_performance_rate.text = performance + "%"
//            tv_quality_rate.text = quality + "%"
//
//            val oee_int = oee.toInt()
//            val availability_int = ceil(availability.toFloat()).toInt()
//            val performance_int = ceil(performance.toFloat()).toInt()
//            val quality_int = ceil(quality.toFloat()).toInt()
//
//            oee_progress.progress = oee_int
//            availability_progress.progress = availability_int
//            performance_progress.progress = performance_int
//            quality_progress.progress = quality_int
//
//            var oee_color_code = "ff0000"
//            var availability_color_code = "ff0000"
//            var performance_color_code = "ff0000"
//            var quality_color_code = "ff0000"
//
//            for (i in 0..(_color_list.size - 1)) {
//                val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
////                val enumber = _list[i]["enumber"]?.toInt() ?: 0
////                if (snumber <= oee_int && enumber >= oee_int) oee_color_code = _list[i]["color_code"].toString()
////                if (snumber <= availability_int && enumber >= availability_int) availability_color_code = _list[i]["color_code"].toString()
////                if (snumber <= performance_int && enumber >= performance_int) performance_color_code = _list[i]["color_code"].toString()
////                if (snumber <= quality_int && enumber >= quality_int) quality_color_code = _list[i]["color_code"].toString()
//                if (snumber <= oee_int) oee_color_code = _color_list[i]["color_code"].toString()
//                if (snumber <= availability_int) availability_color_code = _color_list[i]["color_code"].toString()
//                if (snumber <= performance_int) performance_color_code = _color_list[i]["color_code"].toString()
//                if (snumber <= quality_int) quality_color_code = _color_list[i]["color_code"].toString()
//            }
//
//            oee_progress.progressStartColor = Color.parseColor("#" + oee_color_code)
//            oee_progress.progressEndColor = Color.parseColor("#" + oee_color_code)
//
//            availability_progress.progressStartColor = Color.parseColor("#" + availability_color_code)
//            availability_progress.progressEndColor = Color.parseColor("#" + availability_color_code)
//
//            performance_progress.progressStartColor = Color.parseColor("#" + performance_color_code)
//            performance_progress.progressEndColor = Color.parseColor("#" + performance_color_code)
//
//            quality_progress.progressStartColor = Color.parseColor("#" + quality_color_code)
//            quality_progress.progressEndColor = Color.parseColor("#" + quality_color_code)
//        }
//    }


    //    var handle_cnt = 0
    fun startHandler() {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                updateView()
//                if (handle_cnt++ > 5) {
//                    handle_cnt = 0
//                    computeCycleTime()
//                }
                startHandler()
            }
        }, 1000)
    }

    // Get Color code
    private fun fetchColorData() {
        var color_list: ArrayList<HashMap<String, String>> = arrayListOf()
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
            color_list.add(map)
        }
        _color_list = color_list

        Log.e("fetchColorData", "---------------------------------------")
        for (i in 0..(_color_list.size - 1)) {
            Log.e("fetchColorData", "" + _color_list[i].toString())
        }
        Log.e("fetchColorData", "---------------------------------------")
    }
}
