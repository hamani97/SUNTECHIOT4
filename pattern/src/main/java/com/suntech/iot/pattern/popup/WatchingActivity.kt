package com.suntech.iot.pattern.popup

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDesign
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_watching.*
import org.joda.time.DateTime
import java.util.*

class WatchingActivity : BaseActivity() {

    private var _list_design: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _list_down: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watching)
        initView()
        start_timer()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    private fun initView() {
        tv_watching.movementMethod = ScrollingMovementMethod()

        // Downtime DB Clear
        btn_delete_downtime.setOnClickListener {
            val down_db = DBHelperForDownTime(this)
            down_db.delete()
            AppGlobal.instance.set_last_count_received("")
            AppGlobal.instance.set_first_count(false)
            ToastOut(this, "All downtime data has been deleted.", true)
        }

        btn_confirm.setOnClickListener {
            finish(true, 1, "ok", null)
        }
    }

    private fun updateView() {
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        var value = "Server = " + AppGlobal.instance.get_server_ip() + ":" + AppGlobal.instance.get_server_port() + "  (v${version})\n"

        value += "DownTime Type : " + AppGlobal.instance.get_downtime_type() + "\n"
        value += "DownTime Second : "

        if (AppGlobal.instance.get_downtime_type() == "Cycle Time") {
            val pieces_cnt = AppGlobal.instance.get_pieces_value()
            val downtime_time = AppGlobal.instance.get_downtime_sec().toInt()
            val total_downtime = downtime_time * pieces_cnt
            value += "${total_downtime} ( = ${downtime_time} * ${pieces_cnt} )"
        } else {
            value += AppGlobal.instance.get_downtime_sec()
        }
        value += "\n\n"

        value += "Design Data : \n\n"

        val design_db = DBHelperForDesign(this)
        _list_design = design_db.gets() ?: _list_design

        for (i in 0..(_list_design.size - 1)) {
            val item = _list_design[i]
            value = value + item?.toString() + "\n"
        }

        value += "\n"
        value += "Downtime Data : \n\n"

        var down_db = DBHelperForDownTime(this)
        _list_down = down_db.gets() ?: _list_down

        // Downtime
        var down_time = 0
        var down_target = 0f

        _list_down?.forEach { item ->
            value = value + item?.toString() + "\n"

            down_time += item["real_millis"].toString().toInt()
            down_target += item["target"].toString().toFloat()
        }

//        for (i in 0..(_list.size - 1)) {
//            val item = _list[i]
//            value = value + item?.toString() + "\n"
//        }

        val target_type = AppGlobal.instance.get_target_type()          // setting menu 메뉴에서 선택한 타입

        val shift_time = AppGlobal.instance.get_current_shift_time()

        if (shift_time != null) {

//            val work_idx = AppGlobal.instance.get_product_idx()

            var total_target = 0f            // 현시점까지 타겟
            var total_actual = 0f            // 현시점까지 액추얼

            for (i in 0..((_list_design.size ?: 1) - 1)) {

                val item = _list_design.get(i)
//                val work_idx2 = item?.get("work_idx").toString()
//                val actual2 = item?.get("actual").toString().toInt()
                val target2 = item.get("target").toString().toFloat()

//                total_actual += actual2

//                if (work_idx == work_idx2) {        // 현재 진행중인 디자인
//                } else {
                    total_target += target2   // 현재 계산된 카운트를 더한다.
//                }
            }

            value += "\n"
            value += "[ OEE Graph ] \n\n"


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

            val planned1_time = AppGlobal.instance.compute_time_millis(
                shift_stime_millis,
                now_millis,
                planned1_stime_millis,
                planned1_etime_millis
            )
            val planned2_time = AppGlobal.instance.compute_time_millis(
                shift_stime_millis,
                now_millis,
                planned2_stime_millis,
                planned2_etime_millis
            )

            // 현재까지의 작업시간
            val work_time = ((now_millis - shift_stime_millis) / 1000) - planned1_time - planned2_time

            // 현재까지의 Actual
            total_actual = AppGlobal.instance.get_current_shift_actual_cnt()

            // 현재까지의 Target
//            val total_target = 0


            // Availability Check
            val availability = (work_time - down_time).toFloat() / work_time
//            val availability_rate = floor(availability * 1000) / 10

            value += "Availibility = ($work_time - $down_time) / $work_time = $availability\n"


            // Performance Check
//            val performance = if (AppGlobal.instance.get_target_stop_when_downtime()) {
//                if (total_target > 0) total_actual.toFloat() / total_target else 0F
//            } else {
//                if (total_target - down_target > 0) total_actual.toFloat() / (total_target - down_target) else 0F
//            }
//            val performance_rate = floor(performance * 1000) / 10

            val performance = if (total_target - down_target > 0f) total_actual / (total_target - down_target) else 0F

            value += "Performance = $total_actual / ($total_target - $down_target) = $performance\n"


            // Quality Check
            var defective_count = design_db.sum_defective_count()
            if (defective_count==null || defective_count<0) defective_count = 0

            val quality = if (total_actual != 0f) (total_actual - defective_count) / total_actual else 0F
//            val quality_rate = floor(quality * 1000) / 10

            value += "Quality = ($total_actual - $defective_count) / $total_actual = $quality\n"

            value += "\n"
            value += "\n"

            value += "- Availibility = (현시점까지 작업시간 - 다운타임 시간) / 현시점까지 작업시간(초)\n"
            value += "- Performance = 현재까지의 Actual / (현시점까지 타겟 - 다운타임 시간동안 타겟) ===> 원래는 (현시점까지 작업시간-다운타임 시간)의 타겟임\n"
            value += "- Quality = (현시점의 actual - defective) / Actual\n"

        }

        tv_watching.setText(value)
    }

    private val _timer_task1 = Timer()

    private fun start_timer () {
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateView()
                }
            }
        }
        _timer_task1.schedule(task1, 1000, 10000)
    }
    private fun cancel_timer () {
        _timer_task1.cancel()
    }
}
