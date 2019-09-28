package com.suntech.iot.pattern.popup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.suntech.iot.pattern.PopupSelectList
import com.suntech.iot.pattern.R
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import kotlinx.android.synthetic.main.activity_piece_pair_count_edit.*

class PiecePairCountEditActivity : BaseActivity() {
    private var _pieces = 0
    private var _pairs = 0

    private var _max_pieces = 10
    private var _max_pairs = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_piece_pair_count_edit)

        val pieces = intent.getStringExtra("pieces")
        val pairs = intent.getStringExtra("pairs")

        _pieces = pieces.toInt()
        _pairs = pairs.toInt()

        _max_pieces = AppGlobal.instance.get_pieces_info().toInt()
        val tmp = AppGlobal.instance.get_pairs_info()
        var pairs_str = ""
        when (tmp) {
            "1/2" -> { _max_pairs = 2; pairs_str = "/2" }
            "1/3" -> { _max_pairs = 3; pairs_str = "/3" }
            "1/4" -> { _max_pairs = 4; pairs_str = "/4" }
            "1/5" -> { _max_pairs = 5; pairs_str = "/5" }
            "1/6" -> { _max_pairs = 6; pairs_str = "/6" }
            "1/7" -> { _max_pairs = 7; pairs_str = "/7" }
            "1/8" -> { _max_pairs = 8; pairs_str = "/8" }
        }

        tv_design_pieces?.text = AppGlobal.instance.get_pieces_info()
        tv_design_pairs?.text = AppGlobal.instance.get_pairs_info()

        tv_pieces_count?.setText(pieces)
        et_pieces_count?.setText(pieces)

        tv_pairs_count?.setText(pairs + pairs_str)
        et_pairs_count?.setText(pairs)

        tv_design_pieces.setOnClickListener { fetchPiecesData() }
        tv_design_pairs.setOnClickListener { fetchPairsData() }

        btn_trim_count_plus.setOnClickListener {
            if (_pieces + 1 < _max_pieces) {
                _pieces++
                et_pieces_count.setText(_pieces.toString())
            }
        }
        btn_trim_count_minus.setOnClickListener {
            if (_pieces > 0) {
                _pieces--
                et_pieces_count.setText(_pieces.toString())
            }
        }
        btn_trim_pairs_plus.setOnClickListener {
            if (_pairs + 1 < _max_pairs) {
                _pairs++
                et_pairs_count.setText(_pairs.toString())
            }
        }
        btn_trim_pairs_minus.setOnClickListener {
            if (_pairs > 0) {
                _pairs--
                et_pairs_count.setText(_pairs.toString())
            }
        }
        btn_confirm.setOnClickListener {
            if (tv_design_pieces.text.toString() == "" || tv_design_pairs.text.toString() == "") {
                ToastOut(this, R.string.msg_require_info, true)
                return@setOnClickListener
            }
            AppGlobal.instance.set_pieces_info(tv_design_pieces.text.toString())
            AppGlobal.instance.set_pairs_info(tv_design_pairs.text.toString())

            finish(true, 0, "ok", hashMapOf("pieces" to ""+_pieces, "pairs" to ""+_pairs))
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }

    fun parentSpaceClick(view: View) {
        var view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun fetchPiecesData() {
        var arr: java.util.ArrayList<String> = arrayListOf<String>()

        for (i in 1..10) {
            arr.add(i.toString())
        }

        val intent = Intent(this, PopupSelectList::class.java)
        intent.putStringArrayListExtra("list", arr)
        startActivity(intent, { r, c, m, d ->
            if (r) {
                tv_design_pieces.text = arr[c]
            }
        })
    }
    private fun fetchPairsData() {
        var arr: java.util.ArrayList<String> = arrayListOf<String>()

        arr.add("1/8")
        arr.add("1/7")
        arr.add("1/6")
        arr.add("1/5")
        arr.add("1/4")
        arr.add("1/3")
        arr.add("1/2")

        for (i in 1..10) {
            arr.add(i.toString())
        }

        val intent = Intent(this, PopupSelectList::class.java)
        intent.putStringArrayListExtra("list", arr)
        startActivity(intent, { r, c, m, d ->
            if (r) {
                tv_design_pairs.text = arr[c]
            }
        })
    }
}