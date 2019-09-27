package com.suntech.iot.pattern.popup

import android.content.Intent
import android.os.Bundle
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.db.DBHelperForDesign
import com.suntech.iot.pattern.db.DBHelperForDownTime
import kotlinx.android.synthetic.main.activity_watching.*
import java.util.*

class WatchingActivity : BaseActivity() {

    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

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
        btn_confirm.setOnClickListener {
            finish(true, 1, "ok", null)
        }
    }

    private fun updateView() {
        var value = "Design Data : \n\n"

        var db = DBHelperForDesign(this)
        _list = db.gets() ?: _list

        for (i in 0..(_list.size - 1)) {
            val item = _list[i]
            value = value + item?.toString() + "\n"
        }

        value += "\n"
        value += "Downtime Data : \n\n"

        var _db = DBHelperForDownTime(this)
        _list = _db.gets() ?: _list

        for (i in 0..(_list.size - 1)) {
            val item = _list[i]
            value = value + item?.toString() + "\n"
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
