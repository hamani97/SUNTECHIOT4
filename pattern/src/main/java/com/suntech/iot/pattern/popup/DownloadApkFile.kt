package com.suntech.iot.pattern.popup

import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

class DownloadApkFile : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_apkfile)
        startDownload()
    }

    fun startDownload() {

        try {
            val addr = "http://115.68.227.31"
            val path = "/apk/Pattern App"
            val file = "pattern-hwaseung_1.5.1.apk"
            val url = URL(addr + path + "/" + file)

            val conn = url.openConnection()
            conn.doOutput = true
            conn.connect()
            Log.e("Download", "Connect")    // 확인

            val sd_root = Environment.getExternalStorageDirectory()
            val output_stream = File(sd_root, file).outputStream()
            Log.e("Download", "Output stream")    // 확인
            Log.e("Download", "Output stream. env = " + sd_root.toString())    // 확인

            val input_stream = conn.getInputStream()

            while (true) {
                val buffer = input_stream.readBytes()
                if (buffer.size == 0) break
                output_stream.write(buffer, 0, buffer.size)
                Log.e("Download", buffer.size.toString() + " bytes writing...")    // 확인
            }

//            val total_size = conn.contentLength
//            var down_size = 0
//
//            var len = 0
//            while ((len = input_stream.read(buffer)) > 0) {
//                output_stream.write(buffer, 0, len)
//                down_size += len
//                Log.e("Download", "while loading...")    // 확인
//            }
            output_stream.close()
            input_stream.close()

        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Log.e("DOWNLOAD", "end");

        installAPK()
    }

    fun installAPK() {

    }
}