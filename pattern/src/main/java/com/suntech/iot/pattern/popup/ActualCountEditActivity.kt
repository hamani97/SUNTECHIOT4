package com.suntech.iot.pattern.popup

import android.content.Intent
import android.os.Bundle
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForDesign
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_actual_count_edit.*
import kotlinx.android.synthetic.main.list_item_product_title.*
import kotlinx.android.synthetic.main.list_item_product_total.*
import org.joda.time.DateTime

class ActualCountEditActivity : BaseActivity() {

    private var list_adapter: ProductListActivity.ListDefectiveAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actual_count_edit)
        initView()
        updateView()
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun initView() {
        btn_confirm.setOnClickListener {
            finish(true, 1, "ok", null)
        }
        lv_products.setOnItemClickListener { adapterView, view, i, l ->
            val work_idx = _list[i]["work_idx"]
            val design_idx = _list[i]["design_idx"]
            val actual = _list[i]["actual"]

            val intent = Intent(this, ActualCountEditInputActivity::class.java)
            intent.putExtra("work_idx", work_idx)
            intent.putExtra("design_idx", design_idx)
            intent.putExtra("actual", actual)
            startActivity(intent, { r, c, m, d ->
                if (r) {
                    updateView()
                }
            })
        }
    }

    private fun updateView() {

        tv_item_row0.text = "TOTAL"
        tv_item_row1.text = ""
        tv_item_row2.text = ""

        var db = DBHelperForDesign(this)
        _list = db.gets() ?: _list

        list_adapter = ProductListActivity.ListDefectiveAdapter(this, _list)
        lv_products.adapter = list_adapter

        var total_target = 0
        var total_actual = 0
        var total_defective = 0
//        var total_product_rate = 0
//        var total_quality_rate = 0
        var total_work_time = 0

        for (i in 0..(_list.size - 1)) {
            val item = _list[i]

            val start_dt_txt = item["start_dt"]
            val end_dt_txt = item["end_dt"]
            var start_dt = OEEUtil.parseDateTime(start_dt_txt)
            var end_dt = if (end_dt_txt==null) DateTime() else OEEUtil.parseDateTime(end_dt_txt)

            var dif = end_dt.millis - start_dt.millis

            val target = item["target"]?.toInt() ?: 0
            val actual = item["actual"]?.toInt() ?: 0
            val defective = item["defective"]?.toInt() ?: 0
            var product_rate = ((actual.toFloat()/target.toFloat()) *100).toInt().toString()+ "%"
            var quality_rate = (((actual.toFloat()-defective)/actual.toFloat()) *100).toInt().toString()+ "%"
            val work_time = (dif / 1000 / 60 ).toInt()
            if (target==0) product_rate = "N/A"
            if (target==0) quality_rate = "N/A"

            total_target += target
            total_actual += actual
            total_defective += defective
            total_work_time += work_time

            item.put("target", target.toString())
            item.put("actual", actual.toString())
            item.put("defective", defective.toString())
            item.put("product_rate", product_rate)
            item.put("quality_rate", quality_rate)
            item.put("work_time", "" +  work_time + " min")
        }

        tv_item_row1.text = "" +  total_work_time + " min"
        tv_item_row3.text = total_target.toString()
        tv_item_row4.text = total_actual.toString()
        tv_item_row5.text = "-"
        tv_item_row6.text = total_defective.toString()
        tv_item_row7.text = "-"
    }
}
