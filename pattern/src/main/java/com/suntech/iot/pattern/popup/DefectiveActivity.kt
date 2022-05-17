package com.suntech.iot.pattern.popup

import android.content.Intent
import android.os.Bundle
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDesign
import com.suntech.iot.pattern.db.DBHelperForDownTime
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_defective.*
import kotlinx.android.synthetic.main.list_item_defective_total.*
import org.joda.time.DateTime
import kotlin.math.ceil

class DefectiveActivity : BaseActivity() {

    private var list_adapter: ProductListActivity.ListDefectiveAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_defective)
        initView()
        updateView()
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun updateView() {

        tv_item_row0?.text = getString(R.string.list_item_total)
        tv_item_row2?.text = ""

        val db = DBHelperForDesign(this)
        _list = db.gets() ?: _list

        list_adapter = ProductListActivity.ListDefectiveAdapter(this, _list)
        lv_products.adapter = list_adapter

        var total_actual = 0f
        var total_defective = 0

        for (i in 0..(_list.size - 1)) {
            val item = _list[i]

            val actual = item["actual"]?.toFloat() ?: 0f
            val defective = item["defective"]?.toInt() ?: 0
            val tmp_rate = if (actual > 0f) ((actual-defective) / actual) * 100 else 0.0f
            var quality_rate = String.format("%.1f", tmp_rate)
            quality_rate = quality_rate.replace(",", ".") + "%"

            total_actual += actual
            total_defective += defective

            item.put("actual", actual.toString())
            item.put("defective", defective.toString())
            item.put("quality_rate", quality_rate)
        }

        tv_item_row4.text = total_actual.toString()
        tv_item_row6.text = total_defective.toString()
        tv_item_row7.text = "-"
    }

    private fun initView() {
        btn_confirm.setOnClickListener {
            finish(true, 1, "ok", null)
        }

        lv_products.setOnItemClickListener { adapterView, view, i, l ->
            val work_idx = _list[i]["work_idx"]
            val design_idx = _list[i]["design_idx"]
            val defective = _list[i]["defective"]

            val intent = Intent(this, DefectiveInputActivity::class.java)
            intent.putExtra("work_idx", work_idx)
            intent.putExtra("design_idx", design_idx)
            intent.putExtra("defective", defective)
            startActivity(intent, { r, c, m, d ->
                if (r) {
                    sendCount()
                    updateView()
                }
            })
        }
    }

    // 디펙티브에서도 카운트와 같은 값을 보내달라고 요청함.
    // 2020-12-03
    fun sendCount() {

        val db = DBHelperForDesign(this)

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

        request(this, uri, true,false, params, { result ->
            val code = result.getString("code")
            if(code != "00") {
               ToastOut(this, result.getString("msg"), true)
            }
        })
    }
}
