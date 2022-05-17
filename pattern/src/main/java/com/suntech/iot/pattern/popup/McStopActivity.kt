package com.suntech.iot.pattern.popup

import android.os.Bundle
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import kotlinx.android.synthetic.main.activity_mc_stop.*
import java.util.*

class McStopActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mc_stop)
        initView()
        updateView()
        start_timer()
    }

    override fun onResume() {
        super.onResume()
        is_loop = true
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
        is_loop = false
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    private fun updateView() {
//        AppGlobal.instance.set_downtime_idx("")
        AppGlobal.instance.set_last_count_received()    // 현재시간으로 리셋
    }

    private fun initView() {
        btn_mc_start.setOnClickListener {
//            AppGlobal.instance.set_downtime_idx("")
            AppGlobal.instance.set_last_count_received()    // 현재시간으로 리셋
            finish(true, 1, "ok", null)
        }
    }

    private val _timer_task1 = Timer()

    private var is_loop = true

    private fun start_timer() {
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (is_loop) {
                        updateView()
                    }
                }
            }
        }
        _timer_task1.schedule(task1, 2000, 2000)

    }
    private fun cancel_timer () {
        _timer_task1.cancel()
    }
}
