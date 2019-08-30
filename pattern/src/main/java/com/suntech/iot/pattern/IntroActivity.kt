package com.suntech.iot.pattern

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
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
                sendAppStartTime()
            }
        }, 600)
    }

    private fun moveToNext() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
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
            var msg = result.getString("msg")
            if(code != "00"){
                ToastOut(this, msg)
            }
            moveToNext()
        }, {
            moveToNext()
        })
    }
}