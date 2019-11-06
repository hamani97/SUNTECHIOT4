package com.suntech.iot.pattern

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import org.joda.time.DateTime

class IntroActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        AppGlobal.instance.setContext(this)

        Log.e("settings", "Server IP = " + AppGlobal.instance.get_server_ip())
        Log.e("settings", "Mac addr = " + AppGlobal.instance.getMACAddress())
        Log.e("settings", "IP addr " + AppGlobal.instance.get_local_ip())
        Log.e("settings", "factory = " + AppGlobal.instance.get_factory())
        Log.e("settings", "room = " + AppGlobal.instance.get_room())
        Log.e("settings", "line = " + AppGlobal.instance.get_line())

        Handler().postDelayed({
            if (AppGlobal.instance.get_server_ip()=="") {
                moveToNext()
            } else {
                getDeviceInfo()
            }
        }, 600)
    }

    private fun moveToNext() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getDeviceInfo() {
        val uri = "/device.php"
        var params = listOf(
            "code" to "device_info",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "serial_no" to AppGlobal.instance.get_mc_serial(),
            "machine_no" to AppGlobal.instance.get_mc_no1(),
            "model_idx" to AppGlobal.instance.get_mc_model_idx())

        request(this, uri, true, params, { result ->
            var code = result.getString("code")
            Log.e("Device Info", "" + result.getString("msg"))
            if(code == "99") {
                val item = result.getJSONObject("item")

                val line_idx = item.getString("line_idx") ?: ""
                val line_name = item.getString("line_name") ?: ""
                val serial_no = item.getString("serial_no") ?: ""
                val machine_no = item.getString("machine_no") ?: ""
                val model_idx = item.getString("model_idx") ?: ""
                val model_name = item.getString("model_name") ?: ""

                if (line_idx != "" && line_name != "") {
                    AppGlobal.instance.set_line_idx(line_idx)
                    AppGlobal.instance.set_line(line_name)
                }
                if (serial_no != "") AppGlobal.instance.set_mc_serial(serial_no)
                if (machine_no != "") AppGlobal.instance.set_mc_no1(machine_no)
                if (model_idx != "" && model_name != "") {
                    AppGlobal.instance.set_mc_model_idx(model_idx)
                    AppGlobal.instance.set_mc_model(model_name)
                }
            }
            sendAppStartTime()
        }, {
            sendAppStartTime()
        })
    }

    private fun sendAppStartTime() {
        val now = DateTime()
        val uri = "/setting1.php"
        var params = listOf(
            "code" to "time",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "start_time" to now.toString("yyyy-MM-dd HH:mm:ss"))

        request(this, uri, true, params, { result ->
            var code = result.getString("code")
            if(code != "00") {
                ToastOut(this, result.getString("msg"))
            }
            moveToNext()
        }, {
            moveToNext()
        })
    }
}