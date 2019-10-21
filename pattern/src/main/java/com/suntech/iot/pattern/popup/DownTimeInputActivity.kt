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


class DownTimeInputActivity : BaseActivity() {

    private var _db = DBHelperForDownTime(this)

    var _idx = ""
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

        _idx = intent.getStringExtra("idx")
        _start_dt = intent.getStringExtra("start_dt")
        _start_dt_millis = OEEUtil.parseDateTime(_start_dt).millis

        fetchData()
    }

    private fun initView() {

        val params = ViewGroup.MarginLayoutParams(ll_base_box.layoutParams)
        val par_width = (params.width - 60) / 3 - 20 // 200
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
            btn.textSize = 29f
            btn.setPadding(20, 20, 20, 20)
            if (i < 3) {
                ll_btn_list.addView(btn)
            } else {
                ll_btn_list2.addView(btn)
            }

            var p = btn?.getLayoutParams() as LinearLayout.LayoutParams
            if (p != null) {
                p.width = par_width
                p.height = height
                p.gravity = Gravity.LEFT
                p.setMargins(10, 10, 10, 10)
                btn.setLayoutParams(p)
            }

            btn.setOnClickListener {
//                Log.e("DownTime", "Value = " + down_idx + ", " + down_name)
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

//        btn_cancel.setOnClickListener {
//            finish(false, 1, "ok", null)
//        }
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
        val count = _db.counts_for_notcompleted()
        if (AppGlobal.instance.get_screen_blink()) blink_cnt = 1 - blink_cnt

        if (count > 0 && blink_cnt==1) {
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

    private fun sendEndDownTime(downtime: String = "", down_name: String = "") {
        if (AppGlobal.instance.get_server_ip() == "") {
            ToastOut(this, com.suntech.iot.pattern.R.string.msg_has_not_server_info, true)
            return
        }
        if (AppGlobal.instance.get_downtime_idx() == "") {
            ToastOut(this, com.suntech.iot.pattern.R.string.msg_data_not_found, true)
            finish(true, 0, "ok", null)
            return
        }
//        if (_selected_idx < 0) {
        if (downtime == "") {
            ToastOut(this, com.suntech.iot.pattern.R.string.msg_has_notselected, true)
            return
        }

        val idx = AppGlobal.instance.get_downtime_idx()
        val target_type = AppGlobal.instance.get_target_type()          // setting menu 메뉴에서 선택한 타입

        val now = DateTime()
        var down_time = 0
        var real_down_time = 0
        var target = 0

        val item = _db.get(idx)
        if (item != null) {
            val now_millis = now.millis
            val down_start_millis = OEEUtil.parseDateTime(item["start_dt"].toString()).millis

            var planned1_time = 0
            var planned2_time = 0

            val shift_time = AppGlobal.instance.get_current_shift_time()

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


            val target_type = AppGlobal.instance.get_target_type()          // setting menu 메뉴에서 선택한 타입

            var ct = if (target_type.substring(0, 6) == "cycle_") AppGlobal.instance.get_cycle_time() else AppGlobal.instance.get_current_shift_target()

            if (ct > 0) target = real_down_time / ct
        }

//        val downtime = _list[_selected_idx]["idx"]

        val uri = "/downtimedata.php"
        var params = listOf(
            "code" to "end",
            "idx" to AppGlobal.instance.get_downtime_idx(),
            "downtime" to downtime,
            "edate" to now.toString("yyyy-MM-dd"),
            "etime" to now.toString("HH:mm:ss"))

//        btn_confirm.isEnabled = false
//        btn_cancel.isEnabled = false

        request(this, uri, true,true, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                val idx = AppGlobal.instance.get_downtime_idx()
                AppGlobal.instance.set_downtime_idx("")

                _db.updateEnd(idx, down_name, now.toString("yyyy-MM-dd HH:mm:ss"), down_time, real_down_time, target)

                ToastOut(this, msg, true)
                finish(true, 0, "ok", null)

            } else if (code == "99") {
                resendStartDownTime()

            } else {
//                btn_confirm.isEnabled = true
//                btn_cancel.isEnabled = true
                ToastOut(this, msg, true)
            }
        })
    }

    private fun resendStartDownTime() {
        if (AppGlobal.instance.get_server_ip() == "") return

        val work_idx = "" + AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        val idx = intent.getStringExtra("idx")
        val item = _db.get(idx)

        if (item !=null) {
            val start_dt = item["start_dt"].toString()
            val didx = item["design_idx"].toString()
            val shift_idx = item["shift_id"].toString()
            val shift_name = item["shift_name"].toString()
            val dt = OEEUtil.parseDateTime(start_dt)
            _db.delete(idx)

//            var work_db = SimpleDatabaseHelper(this)
//            val row = work_db.get(work_idx)
//            val seq = row!!["seq"].toString().toInt()
            val seq = item["seq"]

            val uri = "/downtimedata.php"
            var params = listOf(
                "code" to "start",
                "mac_addr" to AppGlobal.instance.getMACAddress(),
                "didx" to didx,
                "sdate" to dt.toString("yyyy-MM-dd"),
                "stime" to dt.toString("HH:mm:ss"),
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx(),
                "line_idx" to AppGlobal.instance.get_line_idx(),
                "shift_idx" to shift_idx,
                "seq" to seq)

            request(this, uri, true, false, params, { result ->
                var code = result.getString("code")
                if (code == "00") {
                    var idx = result.getString("idx")
                    AppGlobal.instance.set_downtime_idx(idx)

                    _db.add(idx, work_idx, didx, shift_idx, shift_name, start_dt)

                    sendEndDownTime()
                } else {
                    ToastOut(this, result.getString("msg"), true)
                }
            })
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
}
