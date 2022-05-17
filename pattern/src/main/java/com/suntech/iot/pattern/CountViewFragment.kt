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
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.suntech.iot.pattern.base.BaseFragment
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDesign
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.popup.DefectiveEditActivity
import com.suntech.iot.pattern.popup.McStopActivity
import com.suntech.iot.pattern.popup.PiecePairCountEditActivity
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.layout_bottom_info_3.*
import kotlinx.android.synthetic.main.layout_side_menu.*
import kotlinx.android.synthetic.main.layout_top_menu.*
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.floor

class CountViewFragment : BaseFragment() {

    private var is_loop: Boolean = false

    private var _color_list: ArrayList<HashMap<String, String>> = arrayListOf()
//    private var _list_for_db: ArrayList<HashMap<String, String>> = arrayListOf()

//    private var _total_target = 0
//    private var _total_actual = 0

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            resetDefectiveCount()    // DB에서 기본값을 가져다 화면에 출력
            viewWorkInfo()
        }
    }

    fun resetDefectiveCount() {
        val design_db = DBHelperForDesign(activity)
        val count = design_db.sum_defective_count()
        tv_defective_count?.text = if (count<0) "0" else count.toString()
    }

    // 디펙티브에서도 카운트와 같은 값을 보내달라고 요청함.
    // 2020-12-03
    fun sendCount() {

        val db = DBHelperForDesign(activity)

        var now_target = 0F
        var now_actual = 0F

        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx != "") {
            val row = db.get(work_idx)
            if (row != null) {
                now_target = row["target"].toString().toFloat()  // 현디자인의 타겟
                now_actual = row["actual"].toString().toFloat()  // 현디자인의 액추얼
            }
        }

        // 현재시간
        val now_millis = DateTime.now().millis

        val sum_count = AppGlobal.instance.get_current_shift_actual_cnt()
        var count_target = db.sum_target_count()            // 총 타겟
        val count_defective = db.sum_defective_count()      // 현재 디펙티브 값

        // Downtime
        var shift_total_time = 0
        var planned_time = 0
        var param_rumtime = 0
        var work_time = 0

        val shift_time = AppGlobal.instance.get_current_shift_time()
        val shift_idx =
            if (shift_time != null) {
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
                val down_db = DBHelperForDownTime(activity)
                val down_time = down_db.sum_real_millis_count()
                val down_target = down_db.sum_target_count()

                if (down_target > 0f) count_target -= down_target

                param_rumtime = work_time - down_time

                shift_time["shift_idx"].toString()
                // ctO 구하기 (현시점까지 작업시간 - 다운타임 시간)의 타겟
            } else {
                "0"
            }

        val uri = "/hwi/query.php"
        val params = listOf(
            "code" to "send_count",
            "mac" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "count_sum" to sum_count,
            "shift_idx" to  shift_idx,
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "worktime" to work_time,
            "runtime" to param_rumtime.toString(),
            "actualO" to sum_count.toString(),
            "ctO" to count_target.toString(),
            "defective" to count_defective.toString(),
            "worker" to AppGlobal.instance.get_worker_no(),
            "available_time" to (shift_total_time/60).toString(),
            "planned_stop_time" to (planned_time/60).toString(),
            "target" to now_target.toString(),
            "actual" to now_actual.toString())

        getBaseActivity().request(activity, uri, true,false, params, { result ->
            val code = result.getString("code")
            if(code != "00") {
                (activity as MainActivity).ToastOut(activity, result.getString("msg"), true)
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_count_view, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_refresh, IntentFilter("need.refresh"))
        is_loop = true
        fetchColorData()
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
    }

    override fun initViews() {
        super.initViews()

        // Total count view
        tv_count_view_target?.text = "0"
        tv_count_view_actual?.text = "0"
        tv_count_view_ratio?.text = "0%"

        // Server charts
        initOEEGraph()

        val version = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
        val verArr = version.split(".")
        tv_app_version2?.text = "V" + verArr[verArr.size-3] + "." + verArr[verArr.size-2] + verArr[verArr.size-1]

        // Downtime 일시 정지
        // 리턴되면 창이 닫힌 순간으로 다운타임 초기화 된다.
        btn_mc_stop.setOnClickListener {
            val intent = Intent(activity, McStopActivity::class.java)
            (activity as MainActivity).startActivity(intent)
        }
/*
        btn_stitch.setOnClickListener {
            val str = "{\"cmd\":\"stitch\", \"value\":\"start\"}"
            (activity as MainActivity).handleData(str)
        }
*/
        // Piece Pair 수정으로 변경됨
        btn_init_actual.setOnClickListener {
            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                (activity as MainActivity).ToastOut(activity, R.string.msg_no_operator, true)
            } else if (AppGlobal.instance.get_factory() == "" || AppGlobal.instance.get_zone() == "" || AppGlobal.instance.get_line() == "") {
                (activity as MainActivity).ToastOut(activity, R.string.msg_no_setting, true)
            } else if (AppGlobal.instance.get_design_info_idx() == "") {
                (activity as MainActivity).ToastOut(activity, R.string.msg_design_not_selected, true)
            } else {
                val intent = Intent(activity, PiecePairCountEditActivity::class.java)
                intent.putExtra("pieces", "" + (activity as MainActivity).pieces_qty)
                intent.putExtra("pairs", "" + (activity as MainActivity).pairs_qty)
                (activity as MainActivity).startActivity(intent, { r, c, m, d ->
                    if (r) {
                        val work_idx = AppGlobal.instance.get_product_idx()
                        if (work_idx != "") recomputeDowntime(work_idx)

//                        val pieces = d?.get("pieces")
//                        val pairs = d?.get("pairs")
//                        if (pieces != null && pieces != "") {
//                            (activity as MainActivity).pieces_qty = pieces.toInt()
//                            tv_pieces_qty.text = pieces.toString()
//                        }
//                        if (pairs != null && pairs != "") {
//                            (activity as MainActivity).pairs_qty = pairs.toFloat()
//                            tv_pairs_qty.text = pairs.toString()
//                        }
                        resetDefectiveCount()    // DB에서 기본값을 가져다 화면에 출력
                        sendCount()
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
                    if (AppGlobal.instance.get_ask_when_clicking_defective()) {
                        val intent = Intent(activity, DefectiveEditActivity::class.java)
                        (activity as MainActivity).startActivity(intent, { r, c, m, d ->
                            if (r) {
                                resetDefectiveCount()
                                sendCount()
                            }
                        })
                    } else {
                        val design_db = DBHelperForDesign(activity)
                        val row = design_db.get(work_idx)
                        var seq = row!!["seq"].toString().toInt()
                        if (row == null || seq == null || seq == 0) seq = 1

                        // 디펙티브에서도 카운트와 같은 값을 보내달라고 요청함.
                        // 2020-12-03
                        // 현재시간
//                        val now_millis = DateTime.now().millis
//
//                        var count_target = design_db.sum_target_count()            // 총 타겟
//                        val count_defective = design_db.sum_defective_count()      // 현재 디펙티브 값
//                        val sum_count = AppGlobal.instance.get_current_shift_actual_cnt()
//
//                        // Downtime
//
//                        // 시프트 시작/끝
//                        val shift_stime_millis = OEEUtil.parseDateTime(cur_shift["work_stime"].toString()).millis
//                        val shift_etime_millis = OEEUtil.parseDateTime(cur_shift["work_etime"].toString()).millis
//
//                        // 휴식시간
//                        val planned1_stime_millis = OEEUtil.parseDateTime(cur_shift["planned1_stime_dt"].toString()).millis
//                        val planned1_etime_millis = OEEUtil.parseDateTime(cur_shift["planned1_etime_dt"].toString()).millis
//                        val planned2_stime_millis = OEEUtil.parseDateTime(cur_shift["planned2_stime_dt"].toString()).millis
//                        val planned2_etime_millis = OEEUtil.parseDateTime(cur_shift["planned2_etime_dt"].toString()).millis
//
//                        val planned1_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
//                        val planned2_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned2_stime_millis, planned2_etime_millis)
//
//                        // 현재까지의 작업시간
//                        val work_time = ((now_millis - shift_stime_millis) / 1000).toInt() - planned1_time - planned2_time
//
//                        // Downtime
//                        val down_db = DBHelperForDownTime(activity)
//                        val down_time = down_db.sum_real_millis_count()
//                        val down_target = down_db.sum_target_count()
//
//                        if (down_target > 0f) count_target -= down_target
//
//                        val param_rumtime = work_time - down_time

                        // ctO 구하기 (현시점까지 작업시간 - 다운타임 시간)의 타겟

                        val uri = "/defectivedata.php"
                        var params = listOf(
                            "mac_addr" to AppGlobal.instance.getMACAddress(),
                            "didx" to AppGlobal.instance.get_design_info_idx(),
                            "defective_idx" to "99",
                            "cnt" to "1",
                            "shift_idx" to cur_shift["shift_idx"].toString(),
                            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                            "factory_idx" to AppGlobal.instance.get_zone_idx(),
                            "line_idx" to AppGlobal.instance.get_line_idx(),
                            "seq" to seq

//                            "count_sum" to sum_count,               // 현 Actual
//                            "worktime" to work_time,                // 워크타임
//                            "runtime" to param_rumtime.toString(),
//                            "actualO" to sum_count.toString(),
//                            "ctO" to count_target.toString(),
//                            "defective" to (count_defective+1).toString(),  // defective 총수
//                            "worker" to AppGlobal.instance.get_worker_no()
                        )
                        getBaseActivity().request(activity, uri, true, false, params, { result ->
                            val code = result.getString("code")
                            (activity as MainActivity).ToastOut(activity, result.getString("msg"), true)
                            if (code == "00") {
                                val item = design_db.get(work_idx)
                                val defective = if (item != null) item["defective"].toString().toInt() else 0
                                design_db.updateDefective(work_idx, defective + 1)
                                resetDefectiveCount()    // DB에서 기본값을 가져다 화면에 출력
                                sendCount()
                            }
                        })
                    }
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
    }

    // Downtime 재계산
    fun recomputeDowntime(work_idx: String) {
        val didx = AppGlobal.instance.get_design_info_idx()
        val cycle_time = AppGlobal.instance.get_cycle_time()
        val target_type = AppGlobal.instance.get_target_type()

        if (didx == "" || cycle_time == 0) return

        // From Server / From Device 에서 활용
        val one_item_sec = AppGlobal.instance.get_current_maketime_per_piece()

        val down_db = DBHelperForDownTime(activity)
        val down_list = down_db.gets(work_idx)

        for (i in 0..((down_list?.size ?: 1) - 1)) {
            val down_item = down_list?.get(i)
            val down_idx = down_item?.get("idx").toString()
            val down_real_millis = down_item?.get("real_millis").toString().toFloat()
            if (down_real_millis > 0) {
                if (target_type.substring(0, 6) == "cycle_") {
                    val pieces = AppGlobal.instance.get_pieces_value()
                    val pairs = AppGlobal.instance.get_pairs_value()
                    val new_target = OEEUtil.computeTarget(down_real_millis, cycle_time, pieces, pairs)

                    down_db.updateDidxTarget(down_idx, didx, new_target)
                } else {
                    if (one_item_sec != 0F) {
                        val new_target = down_real_millis / one_item_sec
                        down_db.updateDidxTarget(down_idx, didx, new_target)
                    } else {
                        down_db.updateDidxTarget(down_idx, didx, 0f)
                    }
                }
            } else {
                down_db.updateDidxTarget(down_idx, didx, 0f)
            }
        }
    }

    fun viewWorkInfo() {
        // WOS INFO 하단 bottom
        tv_pieces?.text = AppGlobal.instance.get_pieces_text()
        tv_pairs?.text = AppGlobal.instance.get_pairs_text()
        tv_idx?.text = AppGlobal.instance.get_design_info_idx()
        tv_cycle_time?.text = AppGlobal.instance.get_cycle_time().toString()
        tv_model?.text = AppGlobal.instance.get_model()
        tv_mc_no?.text = AppGlobal.instance.get_mc_no()
        tv_material?.text = AppGlobal.instance.get_material_way()
        tv_component?.text = AppGlobal.instance.get_component()

        val pallet = AppGlobal.instance.get_pieces_value()      // Int
        val cycle_time = AppGlobal.instance.get_cycle_time()    // Int
        val pairs = AppGlobal.instance.get_pairs_value()        // float

//        val value = pallet * cycle_time
        val value = (((pallet * cycle_time) / pairs) * 10) / 10   // 소숫점 1번째 자리까지만 구함

        tv_cycle_time_per_pairs?.text = value.toString()
    }

    // 변화를 체크하기 위함
    var last_total_target = -1f
    var last_total_actual = -1f

    private fun updateView() {

        // 기본 출력
        val now = DateTime.now()        // 현재
        tv_current_time?.text = now.toString("yyyy-MM-dd HH:mm:ss")

        // 현재 시프트
        val shift_time = AppGlobal.instance.get_current_shift_time()
        if (shift_time == null) {   // 초기화 후 리턴
            refreshScreen("")
            refreshOEEGraph(0f, 0f)
            return
        }

        val shift_idx = shift_time["shift_idx"].toString()

        // 디자인이 선택되었는지 체크
        val work_idx = AppGlobal.instance.get_product_idx()             // 선택된 디자인
        if (work_idx == "") {   // 초기화 후 리턴. 10초마다 출력
            if ((DateTime().millis / 1000) % 10 == 0L) (activity as MainActivity).ToastOut(activity, R.string.msg_design_not_selected)
            refreshScreen(shift_idx)
            refreshOEEGraph(0f, 0f)
            return
        }

        val now_millis = now.millis

        val target_type = AppGlobal.instance.get_target_type()          // setting menu 메뉴에서 선택한 타입
        val target_type_6 = target_type.substring(0, 6)
        val shift_target = AppGlobal.instance.get_current_shift_target()

        // 서버에서 받아온 타겟값이 정상인지 체크
        if (target_type_6 == "server") {
            if (shift_target == 0f) {   // 초기화 후 리턴. 10초마다 출력
                if ((now_millis / 1000) % 10 == 0L) (activity as MainActivity).ToastOut(activity, R.string.msg_no_target_value_from_server)
                refreshScreen(shift_idx)
                refreshOEEGraph(0f, 0f)
                return
            }
        }

        // 현재 시프트의 기본 정보
        val work_stime = shift_time["work_stime"].toString()
        val work_etime = shift_time["work_etime"].toString()
        val shift_stime = OEEUtil.parseDateTime(work_stime)
        val shift_etime = OEEUtil.parseDateTime(work_etime)

        val shift_etime_millis = shift_etime.millis


        // 설정되어 있는 휴식 시간
        val _planned1_stime = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString())
        val _planned1_etime = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString())
        val _planned2_stime = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString())
        val _planned2_etime = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString())


        // 디자인 DB
        val design_db = DBHelperForDesign(activity)
        val design_db_list = design_db.gets()  // 전체 디자인

        val current_cycle_time = AppGlobal.instance.get_cycle_time()    // Cycle time 계산으로 타겟 구할때 활용
        val current_pieces = AppGlobal.instance.get_pieces_value()
        val current_pairs = AppGlobal.instance.get_pairs_value()

        var shift_total_target = 0f      // 시프트의 총 타겟
        var total_target = 0f            // 현시점까지 타겟
        var total_actual = 0f            // 현시점까지 액추얼

        if (target_type_6 == "cycle_") {

            val refresh_time = current_cycle_time * current_pieces          // 이 시간(초)마다 Target 값이 변한다.

            for (i in 0..((design_db_list?.size ?: 1) - 1)) {
                val item = design_db_list?.get(i)
                val work_idx2 = item?.get("work_idx").toString()
                val actual_no = item?.get("actual_no").toString().toInt()
                val actual2 = item?.get("actual").toString().toFloat()
                val target2 = item?.get("target").toString().toFloat()
                var start_dt = OEEUtil.parseDateTime(item?.get("start_dt").toString())
                if (start_dt < shift_stime) start_dt = shift_stime

                val start_dt_millis = start_dt.millis

                if (work_idx != work_idx2) {        // 지나간 디자인
                    total_actual += actual2
                    total_target += target2         // 이미 계산된 카운트를 더한다.
                    shift_total_target += target2   // 이미 계산된 카운트를 시트프 총합에 더한다.

                } else {                            // 현재 진행중인 디자인
                    if (current_cycle_time == 0) continue

                    total_actual += current_pairs * actual_no

                    // 현 디자인의 휴식시간
                    val d1 = AppGlobal.instance.compute_time(start_dt, shift_etime, _planned1_stime, _planned1_etime)
                    val d2 = AppGlobal.instance.compute_time(start_dt, shift_etime, _planned2_stime, _planned2_etime)

                    // 디자인의 시작부터 시프트 종료시간까지 계산(초) - (시프트의 총 타겟수를 구하기 위해 무조건 계산함)
                    val work_time = ((shift_etime_millis - start_dt_millis) / 1000) - d1 - d2
                    val numbers = floor(work_time.toFloat() / refresh_time)       // 경과된 시간이 몇번째 주기인지 계산
                    var count = floor(numbers * current_pairs * 100) / 100        // 해당 주기를 가지고 실제 타겟값 계산

//                    Log.e("Test-1", "work_time = $work_time, numbers = $numbers, count = $count")

                    shift_total_target += count

                    if (target_type == "cycle_per_day_total") {
                        total_target += count

                    } else if (target_type == "cycle_per_accumulate") {
                        val d11 = AppGlobal.instance.compute_time(start_dt, now, _planned1_stime, _planned1_etime)
                        val d22 = AppGlobal.instance.compute_time(start_dt, now, _planned2_stime, _planned2_etime)

                        // 디자인의 시작부터 현재까지 시간(초)
                        val work_time2 = ((now_millis - start_dt_millis) / 1000) - d11 - d22
                        val numbers2 = floor(work_time2.toFloat() / refresh_time)  // 경과된 시간이 몇번째 주기인지 계산
                        count = floor(numbers2 * current_pairs * 100) / 100        // 해당 주기를 가지고 현 시간까지 만들어야 할 갯수

//                        Log.e("Test-2", "work_time = $work_time2, numbers = $numbers2, count = $count")

                        total_target += count

//                        if (target2 != count) Log.e("process", "work_time2 = $work_time2, refresh_time = $refresh_time, current_pairs = $current_pairs, final = $count")
                    }
                    if (target2 != count) design_db.updateWorkTarget(work_idx2, count)   // target값이 변형되었으면 업데이트
                }
            }

        } else if (target_type_6 == "server" || target_type_6 == "device") {

            shift_total_target = shift_target

            val one_item_sec = AppGlobal.instance.get_current_maketime_per_piece()

            for (i in 0..((design_db_list?.size ?: 1) - 1)) {
                val item = design_db_list?.get(i)
                val actual2 = item?.get("actual").toString().toFloat()
                total_actual += actual2

                if (one_item_sec != 0F) {
                    val work_idx2 = item?.get("work_idx").toString()
                    val target2 = item?.get("target").toString().toFloat()
                    var start_dt2 = OEEUtil.parseDateTime(item?.get("start_dt"))
                    if (start_dt2 < shift_stime) start_dt2 = shift_stime
                    val start_dt2_millis = start_dt2.millis

                    if (work_idx == work_idx2) {        // 현재 진행중인 디자인
                        var count = target2

                        if (target_type.indexOf("total") >= 0) {
                            // 끝나는 시간까지 계산 (시프트의 총 타겟수를 구하기 위해 무조건 계산함)
                            val d1 = AppGlobal.instance.compute_time(start_dt2, shift_etime, _planned1_stime, _planned1_etime)
                            val d2 = AppGlobal.instance.compute_time(start_dt2, shift_etime, _planned2_stime, _planned2_etime)
                            // 디자인의 시작부터 시프트 종료시간까지 (초)
                            val work_time = ((shift_etime_millis - start_dt2_millis) / 1000) - d1 - d2
                            count = ((work_time.toFloat() / one_item_sec) * 100) / 100 // 현 디자인의 시프트 종료까지 만들어야 할 갯수
                            total_target += count

                        } else if (target_type.indexOf("accumulate") >= 0) {
                            // 현 시간까지 계산 (시프트의 총 타겟수를 구하기 위해 무조건 계산함)
                            val d1 = AppGlobal.instance.compute_time(start_dt2, now, _planned1_stime, _planned1_etime)
                            val d2 = AppGlobal.instance.compute_time(start_dt2, now, _planned2_stime, _planned2_etime)
                            // 디자인의 시작부터 현시간까지 (초)
                            val work_time = ((now.millis - start_dt2_millis) / 1000) - d1 - d2
                            count = work_time.toFloat() / one_item_sec
                            total_target += count
                        }
                        // 마지막에 총타겟 갯수를 맞추기 위한 작업
                        if (total_target > shift_target) {
                            val value = total_target - shift_target
                            count -= value
                            total_target -= value
                        }
                        if (target2 != count) design_db.updateWorkTarget(work_idx2, count)   // target값이 변형되었으면 업데이트
                    } else {
                        val end_dt2 = OEEUtil.parseDateTime(item?.get("end_dt"))
                        if (end_dt2 != null) {
                            // 휴식 시간을 뺀 작업시간
                            val d1 = AppGlobal.instance.compute_time(start_dt2, end_dt2, _planned1_stime, _planned1_etime)
                            val d2 = AppGlobal.instance.compute_time(start_dt2, end_dt2, _planned2_stime, _planned2_etime)
                            val work_time2 = ((end_dt2.millis - start_dt2_millis) / 1000) - d1 - d2
                            var count = work_time2.toFloat() / one_item_sec
                            total_target += count

                            // 마지막에 총타겟 갯수를 맞추기 위한 작업
                            if (total_target > shift_target) {
                                val value = total_target - shift_target
                                count -= value
                                total_target -= value
                            }
                            if (target2 != count) design_db.updateWorkTarget(work_idx2, count)     // target값이 변형되었으면 업데이트
                        }
                    }
                }
            }
        }

        // Shift Total Target을 Target DB에 저장
        (activity as MainActivity)._target_db.replace(shift_time["date"].toString(), shift_idx, shift_time["shift_name"].toString(), shift_total_target, work_stime, work_etime)

//        val target_row = (activity as MainActivity)._target_db.get(shift_time["date"].toString(), shift_idx)
//
//        if (target_row == null) {       // insert
//            (activity as MainActivity)._target_db.add(
//                shift_time["date"].toString(), shift_idx, shift_time["shift_name"].toString(), shift_total_target, work_stime, work_etime)
//        } else {                        // update
//            if (shift_total_target != target_row["target"]) {
//                (activity as MainActivity)._target_db.update(
//                    target_row["idx"].toString(), shift_time["shift_name"].toString(), shift_total_target, work_stime, work_etime)
//            }
//        }


        // Downtime 동안 Target 계산안하기
        if (AppGlobal.instance.get_target_stop_when_downtime()) {
            val down_db = DBHelperForDownTime(activity)
            val down_target = down_db.sum_target_count()
            total_target -= down_target
        }

        if (target_type_6 == "cycle_") {
            // 토탈 타겟을 현재의 pairs 배율에 맞춘다.
            total_target = floor(total_target / current_pairs) * current_pairs

            if (AppGlobal.instance.get_start_at_target() == 1) {    // 타겟 시작을 1부터 한다면
                total_target += current_pairs
//            shift_total_target += current_pairs
            }
        } else {
            if (AppGlobal.instance.get_start_at_target() == 1) {    // 타겟 시작을 1부터 한다면
                total_target++
            }
        }

        refreshScreen(shift_idx, total_actual, total_target, shift_total_target)

        refreshOEEGraph(total_actual, total_target)     // OEE graph
    }

    // 값에 변화가 생겼을 때만 리프레시
    private fun refreshScreen(shift_idx:String, total_actual:Float = 0f, total_target:Float = 0f, shift_total_target:Float = 0f) {

        // 값에 변화가 생겼을 때만 다시 그림
        if (total_target != last_total_target || total_actual != last_total_actual) {

            var ratio = 0
            var ratio_txt = "N/A"

            // 소숫점 이하가 0이면 정수만 표시하기 위함
            var total_actual_txt =
                if (total_actual > 0f) {
                    val actual_10_ceil = ceil(total_actual * 10)
                    if (actual_10_ceil == ceil(total_actual) * 10)
                        total_actual.toInt().toString()
                    else
                        (actual_10_ceil / 10).toString()
                } else {
                    "0"
                }

            val total_target_txt =
                if (total_target > 0f) {
                    ratio = (total_actual / total_target * 100).toInt()
                    // rate 값이 100을 넘으면 100%로 표시해 달라고 함.
                    // 2020-10-03.
//                    if (ratio > 999) ratio = 999
                    if (ratio > 100) ratio = 100
                    else if (ratio < 0) ratio = 0
                    ratio_txt = "$ratio%"

                    // 소숫점 이하가 0이면 정수만 표시하기 위함
                    val total_10_ceil = ceil(total_target * 10)
                    if (total_10_ceil == ceil(total_target) * 10)
                        total_target.toInt().toString()
                    else
                        (total_10_ceil / 10).toString()
                } else {
                    "0"
                }

            // Target 보다 Actual 이 크면 (+n) 처럼 표시하는 걸로 변경.
            // 2020-10-03.
            // 줄바꿈 되지 않도록 필요시 글자 크기도 축소
            if (total_target < total_actual) {
                val over = total_actual - total_target
                if (over > 0f) {
                    // 소숫점 이하가 0이면 정수만 표시하기 위함
                    val over_ceil = ceil(over * 10)
                    total_actual_txt = if (over_ceil == ceil(over) * 10)
                        total_target_txt + "(+" + over.toInt().toString() + ")"
                    else
                        total_target_txt + "(+" + (ceil(over*10) / 10).toString() + ")"
                }
            }

            tv_count_view_target.text = total_target_txt
            tv_count_view_actual.text = total_actual_txt
            tv_count_view_ratio.text = ratio_txt

            //
            AppGlobal.instance.set_current_shift_actual_cnt(total_actual)
            tv_report_count?.text = "$total_actual_txt"

            var color_code = "ffffff"

            for (i in 0..(_color_list.size - 1)) {
                val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                val enumber = _color_list[i]["enumber"]?.toInt() ?: 0
                if (ratio in snumber..enumber) color_code = _color_list[i]["color_code"].toString()
            }

            tv_count_view_target.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_actual.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_ratio.setTextColor(Color.parseColor("#" + color_code))

            // 타겟 수량이 바뀌면 서버에 통보한다.
            if (total_target != last_total_target && shift_idx != "") {
                updateCurrentWorkTarget(shift_idx, total_target, shift_total_target)
            }

            // 최종값 업데이트
            last_total_target = total_target
            last_total_actual = total_actual
        }
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
        oee_progress?.progress = 0
        availability_progress?.progress = 0
        performance_progress?.progress = 0
        quality_progress?.progress = 0
        tv_oee_rate?.text = "0%"
        tv_availability_rate?.text = "0%"
        tv_performance_rate?.text = "0%"
        tv_quality_rate?.text = "0%"
    }

    private fun refreshOEEGraph(total_actual:Float, total_target:Float) {

        val shift_time = AppGlobal.instance.get_current_shift_time()
        if (shift_time == null) {
            initOEEGraph(); return
        }

        val now = DateTime()
        val now_millis = now.millis

        // 시프트 시작/끝
        val shift_stime_millis = OEEUtil.parseDateTime(shift_time["work_stime"].toString()).millis
//        val shift_etime_millis = OEEUtil.parseDateTime(shift_time["work_etime"].toString()).millis

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
        var down_target = 0f

        var down_db = DBHelperForDownTime(activity)
        val down_list = down_db.gets()

        down_list?.forEach { item ->
            down_time += item["real_millis"].toString().toInt()
            down_target += item["target"].toString().toFloat()
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
            tv_availability_rate.text = "$availability_int%"
            availability_progress.progress = availability_int
            availability_progress.progressStartColor = Color.parseColor("#" + availability_color_code)
            availability_progress.progressEndColor = Color.parseColor("#" + availability_color_code)
        }


        // Performance Check
        // performance = 현재까지의 Actual / (현시점까지 작업시간 - 다운타임 시간)의 타겟
        val performance =
            if (AppGlobal.instance.get_target_stop_when_downtime()) {
                if (total_target > 0f) total_actual / total_target else 0F     // 이미 down_target을 빼고 온 값이므로 또 빼지 않기 위함
            } else {
                if (total_target-down_target > 0f) total_actual / (total_target-down_target) else 0F
            }

        var performance_rate = floor(performance * 1000) / 10

        if ((activity as MainActivity)._performance_rate != performance_rate) {

            // 100% 넘어가면 푸시발송
            if (performance_rate >= 100.0f) {
                if ((activity as MainActivity)._performance_rate < 100.0f) {
                    (activity as MainActivity).sendPush("SYS: PERFORMANCE")
                }

                // performance 그래프의 값이 100%를 넘지 않도록 표시하기 위해 변경
                // 2020-10-04.
                performance_rate = 100f
            }
            (activity as MainActivity)._performance_rate = performance_rate

            Log.e("refreshOEEGraph", "oee graph redraw : performance = " + (performance * 100) + "%")

            val performance_int = floor(performance_rate).toInt()
            var performance_color_code = "ff0000"

            for (i in 0..(_color_list.size - 1)) {
                val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                if (snumber <= performance_int) performance_color_code = _color_list[i]["color_code"].toString()
            }
            tv_performance_rate.text = "$performance_int%"
            performance_progress.progress = performance_int
            performance_progress.progressStartColor = Color.parseColor("#" + performance_color_code)
            performance_progress.progressEndColor = Color.parseColor("#" + performance_color_code)
        }


        // Quality Check
        // qulity = (현시점의 actual - defective) / Actual
        val design_db = DBHelperForDesign(activity)
        var defective_count = design_db.sum_defective_count()
        if (defective_count==null || defective_count<0) defective_count = 0

        val quality = if(total_actual!=0f) (total_actual-defective_count) / total_actual else 0F
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
            tv_quality_rate.text = "$quality_int%"
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
            tv_oee_rate.text = "" + oee_int + "%"
            oee_progress.progress = oee_int
            oee_progress.progressStartColor = Color.parseColor("#" + oee_color_code)
            oee_progress.progressEndColor = Color.parseColor("#" + oee_color_code)
        }
    }

    // 현재 target을 서버에 저장
    // shift idx, 현재까지 만들 target, 시프트의 총 target
    private fun updateCurrentWorkTarget(shift_idx: String, target: Float, shift_target: Float) {

        Log.e("updateCurrentWorkTarget", "total_target=" + target + ", shift_total_target=" + shift_target)

        // 타겟이 바뀔때도 카운트와 같은 값을 보내달라고 요청함.
        // 2020-12-03

        sendCount()
//
//        val db = DBHelperForDesign(activity)
//
//        // 현재시간
//        val now_millis = DateTime.now().millis
//
//        var count_target = db.sum_target_count()            // 총 타겟
//        val count_defective = db.sum_defective_count()      // 현재 디펙티브 값
//        val sum_count = AppGlobal.instance.get_current_shift_actual_cnt()
//
//        // Downtime
//        var param_rumtime = 0
//        var work_time = 0

//        val shift_time = AppGlobal.instance.get_current_shift_time()
//
//        if (shift_time != null) {
//            // 시프트 시작/끝
//            val shift_stime_millis = OEEUtil.parseDateTime(shift_time["work_stime"].toString()).millis
//            val shift_etime_millis = OEEUtil.parseDateTime(shift_time["work_etime"].toString()).millis
//
//            // 휴식시간
//            val planned1_stime_millis = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString()).millis
//            val planned1_etime_millis = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString()).millis
//            val planned2_stime_millis = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString()).millis
//            val planned2_etime_millis = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString()).millis
//
//            val planned1_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
//            val planned2_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned2_stime_millis, planned2_etime_millis)
//
//            // 현재까지의 작업시간
//            work_time = ((now_millis - shift_stime_millis) / 1000).toInt() - planned1_time - planned2_time
//
//            // Downtime
//            val down_db = DBHelperForDownTime(activity)
//            val down_time = down_db.sum_real_millis_count()
//            val down_target = down_db.sum_target_count()
//
//            if (down_target > 0f) count_target -= down_target
//
//            param_rumtime = work_time - down_time
//
//            shift_time["shift_idx"].toString()
//            // ctO 구하기 (현시점까지 작업시간 - 다운타임 시간)의 타겟
//        }

        val uri = "/Starget.php"
        var params = listOf(
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "now_target" to target.toString(),
            "target" to shift_target.toString(),
            "shift_idx" to  shift_idx

//            "count_sum" to sum_count,               // 현 Actual
//            "worktime" to work_time,                // 워크타임
//            "runtime" to param_rumtime.toString(),
//            "actualO" to sum_count.toString(),
//            "ctO" to count_target.toString(),
//            "defective" to count_defective.toString(),  // defective 총수
//            "worker" to AppGlobal.instance.get_worker_no()
        )
        getBaseActivity().request(activity, uri, true,false, params, { result ->
            var code = result.getString("code")
            if(code != "00"){
                (activity as MainActivity).ToastOut(activity, result.getString("msg"), true)
            }
        })
    }

    //    var handle_cnt = 0
    fun startHandler() {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                updateView()
                startHandler()
            }
        }, 1000)
    }

    private fun fetchColorData() {
        val color_list: ArrayList<HashMap<String, String>> = arrayListOf()
        val list = AppGlobal.instance.get_color_code()
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
    }
}
