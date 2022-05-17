package com.suntech.iot.pattern.popup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_down_time_input.*
import org.joda.time.DateTime
import kotlin.math.floor


class DownTimeInputActivity : BaseActivity() {

    private var _db = DBHelperForDownTime(this)

    var _idx = ""               // 로칼 DB에 저장된 idx 번호
    var _start_dt = ""
    var _start_dt_millis = 0L

    private var is_loop = true

    val _start_down_time_activity = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish(true, 0, "ok", null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_down_time_input)

        _idx = intent.getStringExtra("idx")             // 로칼 DB에 저장된 idx 번호
        _start_dt = intent.getStringExtra("start_dt")   // 다운타임 시작 시간
        _start_dt_millis = OEEUtil.parseDateTime(_start_dt).millis

        // 창이 닫히면 버튼을 누르거나, 삭제한 경우이므로
        // Downtime 계산을 중단한다.
        AppGlobal.instance.set_first_count(false)

        val list = AppGlobal.instance.get_downtime_list()
        if (list.length() > 0) {
            initView()
        } else {
            fetchData()
        }
    }

    private fun fetchData() {
        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "down_time",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx())

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var list = result.getJSONArray("item")
                AppGlobal.instance.set_downtime_list(list)
                initView()
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun initView() {

        val params = ViewGroup.MarginLayoutParams(ll_base_box.layoutParams)
        val par_width = (params.width - 64) / 4 - 12 // 200
        val height = par_width / 3 * 2

        var list = AppGlobal.instance.get_downtime_list()

        for (i in 0..(list.length() - 1)) {

            val item = list.getJSONObject(i)
            val down_idx = item?.getString("idx").toString()
            val down_name = item?.getString("name").toString()

            val btn = Button(this)
            btn.setText(down_name)
            btn.setTextColor(Color.WHITE)
            btn.setBackgroundColor(Color.parseColor("#"+item.getString("color")))
            btn.textSize = 28f
            btn.setPadding(24, 24, 24, 24)
            if (i < 4) {
                ll_btn_list.addView(btn)
            } else {
                ll_btn_list2.addView(btn)
            }

            val p = btn?.getLayoutParams() as LinearLayout.LayoutParams
            if (p != null) {
                p.width = par_width
                p.height = height
                p.gravity = Gravity.LEFT
                p.setMargins(8, 8, 8, 8)
                btn.setLayoutParams(p)
            }

            btn.setOnClickListener {
                sendEndDownTime(down_idx, down_name)
            }
        }

        // Exit 버튼
//        val btn_exit = Button(this)
//        btn_exit.setText(com.suntech.iot.pattern.R.string.exit)
//        btn_exit.setTextColor(Color.parseColor("#535353"))
//        btn_exit.setBackgroundColor(Color.parseColor("#888888"))
//        btn_exit.textSize = 29f
//        btn_exit.setPadding(20, 20, 20, 20)
//
//        ll_btn_list2.addView(btn_exit)
//
//        var p = btn_exit?.getLayoutParams() as LinearLayout.LayoutParams
//        if (p != null) {
//            p.width = par_width
//            p.height = 160
//            p.gravity = Gravity.LEFT
//            p.setMargins(10, 10, 10, 10)
//            btn_exit.setLayoutParams(p)
//        }
//        btn_exit.setOnClickListener {
//            finish(false, 1, "ok", null)
//        }

        btn_cancel.setOnClickListener {
            deleteDownTime()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(_start_down_time_activity, IntentFilter("start.downtime"))
        is_loop = true
        startHandler()
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
        unregisterReceiver(_start_down_time_activity)
        ll_base_box.setBackgroundResource(R.color.colorWhite)
        is_loop = false
    }

    override fun onBackPressed() {
        if (_db.count_for_notcompleted() > 0) {
            ToastOut(this, R.string.msg_has_notcompleted, true)
            return
        }
        super.onBackPressed()
    }

    fun startHandler() {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                updateView()
                checkBlink()
                startHandler()
            }
        }, 1000)
    }

    var blink_cnt = 0

    private fun checkBlink() {
        if (AppGlobal.instance.get_screen_blink()) blink_cnt = 1 - blink_cnt

        if (_db.count_for_notcompleted() > 0 && blink_cnt==1) {
            ll_base_box.setBackgroundColor(Color.parseColor("#" + AppGlobal.instance.get_blink_color()))
        } else {
            ll_base_box.setBackgroundResource(R.color.colorWhite)
        }
    }

    private fun updateView() {

        var planned1_time = 0
        var planned2_time = 0

        val now_millis = DateTime().millis

        val shift_time = AppGlobal.instance.get_current_shift_time()
        if (shift_time != null) {
            val planned1_stime_millis = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString()).millis
            val planned1_etime_millis = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString()).millis
            val planned2_stime_millis = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString()).millis
            val planned2_etime_millis = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString()).millis

            planned1_time = AppGlobal.instance.compute_time_millis(_start_dt_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
            planned2_time = AppGlobal.instance.compute_time_millis(_start_dt_millis, now_millis, planned2_stime_millis, planned2_etime_millis)
        }

        val down_time = ((now_millis - _start_dt_millis) / 1000) - planned1_time - planned2_time   // 휴식시간을 뺀 실제 다운타임

        val h = down_time / 3600
        val m = down_time / 60 % 60
        val s = down_time % 60

        down_remain_time.text = String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun deleteDownTime() {
//        if (AppGlobal.instance.get_downtime_idx() == "") {
//            ToastOut(this, R.string.msg_data_not_found)
//        } else {
//
//            val idx = AppGlobal.instance.get_downtime_idx()
//            val item = _db.getLocalIdx(idx)
//
//            if (item == null) {
//                ToastOut(this, R.string.msg_data_not_found)
//            } else {
//                _db.delete(idx)
//            }
//        }
        val item = _db.getLocalIdx(_idx)

        if (item == null) {
            ToastOut(this, R.string.msg_data_not_found)
        } else {
            _db.deleteLocalIdx(_idx)

            if (_idx == AppGlobal.instance.get_downtime_idx()) {    // 현재 선택된 idx 이면 초기화
                AppGlobal.instance.set_downtime_idx("")
            }
            AppGlobal.instance.set_last_count_received()    // 현재시간으로 리셋
        }
        finish(true, 0, "ok", null)
    }

    // downtime : 선택된 버튼의 서버에 저장된 idx 값
    // down_name : 텍스트
    private fun sendEndDownTime(downtime: String = "", down_name: String = "") {
        if (AppGlobal.instance.get_server_ip() == "") {
            ToastOut(this, R.string.msg_has_not_server_info, true)
            return
        }
//        if (AppGlobal.instance.get_downtime_idx() == "") {
//            ToastOut(this, R.string.msg_data_not_found, true)
//            finish(true, 0, "ok", null)
//            return
//        }
//        if (_selected_idx < 0) {

        if (downtime == "") {
            ToastOut(this, R.string.msg_has_notselected, true)
            return
        }

//        val idx = AppGlobal.instance.get_downtime_idx()
        val target_type = AppGlobal.instance.get_target_type()          // setting menu 메뉴에서 선택한 타입

        val now = DateTime()
        var down_time = 0           // 타운타임시간(초)
        var real_down_time = 0      // 휴식시간을 뺀 실다운타임(초)
        var target = 0f

        val shift_time = AppGlobal.instance.get_current_shift_time()
        val shift_idx = shift_time?.getString("shift_idx") ?: ""

        val item = _db.getLocalIdx(_idx)
        if (item == null) {
            ToastOut(this, R.string.msg_data_not_found, true)
            finish(true, 0, "ok", null)
            return
        }

        val start_dt = item["start_dt"]?.toString() ?: ""

        val now_millis = now.millis
        val start_dt2 = OEEUtil.parseDateTime(start_dt)
        val down_start_millis = start_dt2.millis

        var planned1_time = 0
        var planned2_time = 0

        if (shift_time != null) {
            val planned1_stime_millis = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString()).millis
            val planned1_etime_millis = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString()).millis
            val planned2_stime_millis = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString()).millis
            val planned2_etime_millis = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString()).millis

            planned1_time = AppGlobal.instance.compute_time_millis(down_start_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
            planned2_time = AppGlobal.instance.compute_time_millis(down_start_millis, now_millis, planned2_stime_millis, planned2_etime_millis)
        }
        down_time = ((now_millis - down_start_millis) / 1000).toInt()   // 다운시간
        real_down_time = down_time - planned1_time - planned2_time      // 휴식시간을 뺀 실제 다운타임

        // from cycle 메뉴이면서 Cycle Time 옵션이 들어온 경우
        if (target_type.substring(0, 6) == "cycle_") {
            val ct = AppGlobal.instance.get_cycle_time()
            val pieces = AppGlobal.instance.get_pieces_value()
            val pairs = AppGlobal.instance.get_pairs_value()

            target = OEEUtil.computeTarget(real_down_time.toFloat(), ct, pieces, pairs)

        } else {    // from server, from target 인경우

            val ct = AppGlobal.instance.get_current_shift_target()
            if (ct != 0f) target = floor((real_down_time.toFloat() / ct) * 100) / 100       // 계산식에 이상이 있는지 검사해야함.
        }

        _db.updateEndLocalIdx(_idx, down_name, now.toString("yyyy-MM-dd HH:mm:ss"), down_time, real_down_time, target)

        if (_idx == AppGlobal.instance.get_downtime_idx()) {    // 현재 선택된 idx 이면 초기화
            AppGlobal.instance.set_downtime_idx("")
        }

        AppGlobal.instance.set_last_count_received()    // 현재시간으로 리셋

//        val uri = "/downtimedata.php"
//        var params = listOf(
//            "code" to "end",
//            "idx" to idx,
//            "downtime" to downtime,
//            "worker" to AppGlobal.instance.get_worker_no(),
//            "edate" to now.toString("yyyy-MM-dd"),
//            "etime" to now.toString("HH:mm:ss"))

        val cnt_all = _db.count_all()

        val uri = "/hwi/query.php"
        val params = listOf(
            "code" to "send_downtime",
            "mac" to AppGlobal.instance.getMACAddress(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_zone_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "seq" to cnt_all,
            "downtime_idx" to downtime,
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "sdate" to start_dt2.toString("yyyy-MM-dd"),
            "stime" to start_dt2.toString("HH:mm:ss"),
            "edate" to now.toString("yyyy-MM-dd"),
            "etime" to now.toString("HH:mm:ss"),
            "worker" to AppGlobal.instance.get_worker_no(),
            "shift_idx" to shift_idx,
            "downtime_second" to real_down_time)

        request(this, uri, true,true, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                finish(true, 0, "ok", null)

//            } else if (code == "99") {        // 다운타임 start 전송 기능은 제거됨 2020-11-22
//                resendStartDownTime()

            } else {
                ToastOut(this, "Net: "+result.getString("msg"), true)
                finish(true, 0, "ok", null)
            }
        })
    }

//    private fun resendStartDownTime() {
//        if (AppGlobal.instance.get_server_ip() == "") return
//
//        val work_idx = "" + AppGlobal.instance.get_product_idx()
//        if (work_idx=="") return
//
//        val idx = intent.getStringExtra("idx")
//        val item = _db.get(idx)
//
//        if (item !=null) {
//            val start_dt = item["start_dt"].toString()
//            val didx = item["design_idx"].toString()
//            val shift_idx = item["shift_id"].toString()
//            val shift_name = item["shift_name"].toString()
//            val dt = OEEUtil.parseDateTime(start_dt)
//            _db.delete(idx)
//
//            val local_db_idx = _db.add(idx, work_idx, didx, shift_idx, shift_name, start_dt)
//
//            val seq = item["seq"]
//
//            val uri = "/downtimedata.php"
//            var params = listOf(
//                "code" to "start",
//                "mac_addr" to AppGlobal.instance.getMACAddress(),
//                "didx" to didx,
//                "sdate" to dt.toString("yyyy-MM-dd"),
//                "stime" to dt.toString("HH:mm:ss"),
//                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
//                "factory_idx" to AppGlobal.instance.get_zone_idx(),
//                "line_idx" to AppGlobal.instance.get_line_idx(),
//                "shift_idx" to shift_idx,
//                "worker" to AppGlobal.instance.get_worker_no(),
//                "seq" to seq)
//
//            request(this, uri, true, false, params, { result ->
//                var code = result.getString("code")
//                if (code == "00") {
//                    var idx = result.getString("idx")
//                    AppGlobal.instance.set_downtime_idx(idx)
//
//                    _db.updateLastId(local_db_idx.toString(), idx)
//
//                    sendEndDownTime()
//                } else {
//                    ToastOut(this, result.getString("msg"), true)
//                }
//            })
//        }
//    }

}
