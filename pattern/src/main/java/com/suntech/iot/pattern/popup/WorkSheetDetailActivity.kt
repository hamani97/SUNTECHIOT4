package com.suntech.iot.pattern.popup

import android.net.Uri
import android.os.Bundle
import android.view.View
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.util.UtilFile
import kotlinx.android.synthetic.main.activity_work_sheet_detail.*

class WorkSheetDetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.suntech.iot.pattern.R.layout.activity_work_sheet_detail)
        initView()
    }

    private fun initView() {
        val file_url = intent.getStringExtra("file_url")
        val ext = UtilFile.getFileExt(file_url)
        if (ext.toLowerCase()=="pdf") {
            wv_view.visibility = View.GONE
            //pdf_view.visibility = View.VISIBLE
            val uri = Uri.parse(file_url)
            //pdf_view.fromUri(uri)
        } else {
            wv_view.setInitialScale(100)
            var data = "<html><head><title>Example</title></head>"
            data += "<body style=\"margin:0; padding:0; text-align:center;\"><center><img width=\"100%\" src=\"${file_url}\" /></center></body></html>"
            wv_view.loadData(data, "text/html", null)

            wv_view.visibility = View.VISIBLE
            //pdf_view.visibility = View.GONE
//            wv_view.loadUrl(file_url)
        }

        btn_start.setOnClickListener {
            finish(true, 1, "ok", null)
        }
        btn_back.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }
}
