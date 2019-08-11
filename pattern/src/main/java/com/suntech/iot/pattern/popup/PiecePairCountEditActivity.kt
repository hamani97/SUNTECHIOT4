package com.suntech.iot.pattern.popup

import android.os.Bundle
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

        tv_pieces_count.setText(pieces)
        et_pieces_count.setText(pieces)

        tv_pairs_count.setText(pairs + pairs_str)
        et_pairs_count.setText(pairs)

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
            finish(true, 0, "ok", hashMapOf("pieces" to ""+_pieces, "pairs" to ""+_pairs))
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }
}