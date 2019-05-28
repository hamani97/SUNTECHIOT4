package com.suntech.iot.pattern

import android.os.Bundle
import android.widget.ArrayAdapter
import com.suntech.iot.pattern.base.BaseActivity
import kotlinx.android.synthetic.main.activity_popup_select_list.*

/**
 * Created by rightsna on 2016. 5. 9..
 */
class PopupSelectList : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popup_select_list)
        initViews()
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun initViews() {
        var list = intent.getStringArrayListExtra("list")

        ll_popup_background.setOnClickListener {
            finish(false, 0, "ok", null)
        }
        btn_close.setOnClickListener {
            finish(false, 0, "ok", null)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        lv_lists.adapter = adapter
        lv_lists.setOnItemClickListener { adapterView, view, i, l ->
            finish(true, i, "ok", hashMapOf("year" to ""+list[i]))
        }
    }
}