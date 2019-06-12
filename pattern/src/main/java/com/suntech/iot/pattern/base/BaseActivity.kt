package com.suntech.iot.pattern.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import cc.cloudist.acplibrary.ACProgressConstant
import cc.cloudist.acplibrary.ACProgressFlower
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.core.Json
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Handler
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.util.UtilString
import org.json.JSONObject
import java.util.HashMap

open class BaseActivity : AppCompatActivity() {

    protected var _dialog: ACProgressFlower? = null

    // 액티비티 관련
    private var _br_activity_callback_id = "br.base.activity.callback"
    protected var _callbackFunc: ((state: Boolean, code: Int, message: String, data: HashMap<String, String?>?) -> Unit )? = { r,c,m,d -> }

    val _base_activity_callback = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val r = intent.getBooleanExtra("r", false)
            val c = intent.getIntExtra("c", 0)
            val m = intent.getStringExtra("m")
            val d = intent.getSerializableExtra("d") as HashMap<String, String?>?
            _callbackFunc?.invoke(r, c, m, d)
            _callbackFunc = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _br_activity_callback_id += UtilString.getRandomString(8)
        registerReceiver(_base_activity_callback, IntentFilter(_br_activity_callback_id))
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(_base_activity_callback)
    }

    fun startActivity(intent: Intent, callbackFunc: (state: Boolean, code: Int, message: String, data: HashMap<String, String?>?)-> Unit) {
        this._callbackFunc = callbackFunc
        intent.putExtra("_br_activity_callback_id", _br_activity_callback_id)
        startActivity(intent)
    }

    protected fun finish(state: Boolean, code: Int, message: String, data: HashMap<String, String>?) {
        super.finish()
        val _br_activity_callback_id = intent.getStringExtra("_br_activity_callback_id")
        val intent = Intent(_br_activity_callback_id)
        intent.putExtra("r", state)
        intent.putExtra("c", code)
        intent.putExtra("m", message)
        intent.putExtra("d", data)
        sendBroadcast(intent)
    }
    override fun finish () {
        this.finish(true, 0, "", null)
    }

    // 프로그래스 다이얼로그 관련
    fun showProgressDialog(context: Context, text:String = "") {
        hideProgressDialog()
        _dialog = ACProgressFlower.Builder(context)
            .direction(ACProgressConstant.DIRECT_CLOCKWISE)
            .themeColor(Color.WHITE)
            .text(text)
            .fadeColor(Color.DKGRAY).build()
        _dialog?.show()
    }
    fun hideProgressDialog() {
        _dialog?.hide()
        _dialog?.dismiss()
    }

    // 네트워크 관련
    fun request (context: Context, url:String, port:String, uri:String, is_post:Boolean= false, is_log:Boolean = false, progress:Boolean= false,
                 params:List<Pair<String, Any?>>? = null, callbackFunc: ((JSONObject)-> Unit)? = null, failedCallbackFunc: (() -> Unit)? = null) {
        if (progress) showProgressDialog(context)

        var full_url = url
        if (port!="") full_url += ":"+port

        if (is_log) Log.d("BaseActivity", "url = " + full_url + uri)
        if (is_log && params != null) Log.d("BaseActivity", "params = " + params.toString())
        val currentTimeMillisStart = System.currentTimeMillis()

        val obj = object : Handler<Json> {
            override fun success(request: Request, response: Response, value: Json) {

                if (progress) hideProgressDialog()

                val currentTimeMillisEnd = System.currentTimeMillis()
                val millis = currentTimeMillisEnd - currentTimeMillisStart
                try {
                    if (is_log) Log.d("BaseActivity", "response = " + value.obj().toString() + " , ms = "+ millis)

                    var r = value.obj().getString("code")
                    if(r == "00" || r == "99") callbackFunc?.invoke(value.obj())
                    else handle_network_error(context, "unknown error = " + uri)
                } catch (e:Exception) {
                    failedCallbackFunc?.invoke()
                    if (failedCallbackFunc==null) handle_network_error(context, "server parsing error = " + uri)
                }
            }
            override fun failure(request: Request, response: Response, error: FuelError) {
                if (progress) hideProgressDialog()
                failedCallbackFunc?.invoke()
                if (failedCallbackFunc==null) handle_network_error(context, error.toString())
            }
        }
        if (is_post)
            Fuel.post(full_url + uri, params).responseJson (obj)
        else
            Fuel.get(full_url + uri, params).responseJson (obj)
    }
    fun request(context: Context, uri:String, is_post:Boolean, is_log:Boolean, progress:Boolean, params:List<Pair<String, Any?>>?, callbackFunc: (JSONObject)-> Unit) {
        val port = AppGlobal.instance.get_server_port()
        var url = "http://" + AppGlobal.instance.get_server_ip()
        this.request(context, url, port, uri, is_post, is_log, progress, params, callbackFunc)
    }

    fun request(context: Context, uri:String, is_post:Boolean, progress:Boolean, params:List<Pair<String, Any?>>? = null, callbackFunc: (JSONObject)-> Unit) {
        val port = AppGlobal.instance.get_server_port()
        var url = "http://" + AppGlobal.instance.get_server_ip()
        this.request(context, url, port, uri, is_post, false, progress, params, callbackFunc)
    }

    fun request(context: Context, uri:String, is_post:Boolean, progress:Boolean, params:List<Pair<String, Any?>>? = null, callbackFunc: (JSONObject)-> Unit, failedCallbackFunc: (()-> Unit)? = null) {
        val port = AppGlobal.instance.get_server_port()
        var url = "http://" + AppGlobal.instance.get_server_ip()
        this.request(context, url, port, uri, is_post, false, progress, params, callbackFunc, failedCallbackFunc)
    }

    fun request(context: Context, uri:String, progress:Boolean, params:List<Pair<String, Any?>>? = null, callbackFunc: (JSONObject)-> Unit) {
        val port = AppGlobal.instance.get_server_port()
        var url = "http://" + AppGlobal.instance.get_server_ip()
        this.request(context, url, port, uri, false, false, progress, params, callbackFunc)
    }

    fun request(context: Context, uri:String, progress:Boolean, params:List<Pair<String, Any?>>? = null, callbackFunc: (JSONObject)-> Unit, failedCallbackFunc: (()-> Unit)? = null) {
        val port = AppGlobal.instance.get_server_port()
        var url = "http://" + AppGlobal.instance.get_server_ip()
        this.request(context, url, port, uri, false, false, progress, params, callbackFunc, failedCallbackFunc)
    }

    private fun handle_network_error(context: Context, error:String) {
        Log.e("BaseActivity", error)
//        Toast.makeText(context, R.string.msg_connection_fail, Toast.LENGTH_SHORT).show()
        ToastOut(context, R.string.msg_connection_fail)
        hideProgressDialog()
    }

    fun ToastOut(context: Context, msg: String) {
        if (AppGlobal.instance.get_message_enable()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
    fun ToastOut(context: Context, msg: Int) {
        if (AppGlobal.instance.get_message_enable()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}