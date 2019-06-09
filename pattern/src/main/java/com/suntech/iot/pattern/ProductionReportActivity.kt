package com.suntech.iot.pattern

import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.pattern.base.BaseActivity
import com.suntech.iot.pattern.common.AppGlobal
import com.suntech.iot.pattern.db.DBHelperForReport
import com.suntech.iot.pattern.db.DBHelperForTarget
import com.suntech.iot.pattern.util.OEEUtil
import kotlinx.android.synthetic.main.activity_production_report.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import org.joda.time.DateTime
import java.util.*

class ProductionReportActivity : BaseActivity() {

    var _current_time = DateTime()

    val _target_db = DBHelperForTarget(this)
    val _report_db = DBHelperForReport(this)

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    private var list_adapter2: ListAdapter? = null
    private var _list2: ArrayList<HashMap<String, String>> = arrayListOf()

    private val _broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)){
                    btn_wifi_state.isSelected = true
                } else {
                    btn_wifi_state.isSelected = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_production_report)
        initView()
        start_timer()
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
        updateView()
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(_broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    private fun onlineCheck() {
        if (AppGlobal.instance._server_state) btn_server_state.isSelected = true
        else btn_server_state.isSelected = false

        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false
    }

    private fun outputBlank() {
        _list.removeAll(_list)
        _list2.removeAll(_list2)

        val time_row3 = hashMapOf("type" to "NODATA", "name" to "", "target" to "0", "actual" to "", "accumulate" to "", "rate" to "")

        val time_row = hashMapOf("type" to "HEAD", "name" to "SHIFT 1", "target" to "Target : 0", "actual" to "", "accumulate" to "", "rate" to "")
        _list.add(time_row)
        _list.add(time_row3)

        val time_row2 = hashMapOf("type" to "HEAD", "name" to "SHIFT 2", "target" to "Target : 0", "actual" to "","accumulate" to "", "rate" to "")
        _list2.add(time_row2)
        _list2.add(time_row3)

        list_adapter = ListAdapter(this, _list)
        lv_reports.adapter = list_adapter

        list_adapter2 = ListAdapter(this, _list2)
        lv_reports2.adapter = list_adapter2
    }

    private fun updateView() {
        onlineCheck()
        val row1 = _target_db.gets()
        Log.e("DB", row1.toString())
//        val row = _report_db.gets()
//        Log.e("DB", row.toString())
        var current_dt = _current_time.toString("yyyy-MM-dd")
//        var current_tommorow_dt = _current_time.plusDays(1).toString("yyyy-MM-dd")

        // 해당 날짜의 Shift 별 타겟 수량
        val target_data = _target_db.gets(current_dt)
        val target_size = target_data?.size ?: 0

        if (target_size == 0) {
            outputBlank()
            return
        }

        _list.removeAll(_list)
        _list2.removeAll(_list2)

        var index = 0

        for (i in 0..(target_size-1)) {
            val item = target_data?.get(i)

            if (item != null) {
                val target_txt = item.get("target").toString()
                val shift_idx = item.get("shift_idx").toString()

                // Shift 정보와 제목줄을 위한 데이터
                val time_row = hashMapOf(
                    "type" to "HEAD",
                    "name" to item.get("shift_name").toString(),
                    "target" to "Target : " + target_txt,
                    "actual" to "", "accumulate" to "", "rate" to ""
                )

                // Shift가 두개만 있으면 각 Shift가 좌우로 표시되고 세개면 계산해서 12개씩 표시한다.
                if (target_size==2) {
                    if (i==0) _list.add(time_row)
                    else _list2.add(time_row)
                } else {
                    if (index < 12) _list.add(time_row)
                    else _list2.add(time_row)
                }

                index += 2      // 제목줄은 두칸을 차지하므로 2를 더한다.

                val now_millis = DateTime().millis

                val work_etime = item.get("work_etime").toString()
                val finish_millis = OEEUtil.parseDateTime(work_etime).millis        // 시간을 표시하다가 루프를 빠져나오기 위함

                val work_stime = item.get("work_stime").toString()
                var work_time_dt = OEEUtil.parseDateTime(work_stime)    // 2019-04-05 06:01:00

                val target = item.get("target").toString().toInt()
                var actual = 0
                var accumulate = 0

                var blank_yn = false        // 아직 작업 시간이 안된 시간은 빈칸으로 표시하기 위함.

                for (j in 0..23) {
                    if (work_time_dt.millis >= finish_millis) break
                    if (now_millis < work_time_dt.millis) blank_yn = true

                    val stime = work_time_dt.toString("HH")
                    work_time_dt = work_time_dt.plusHours(1)
                    val etime = work_time_dt.toString("HH")

                    val row = _report_db.get(current_dt, stime, shift_idx)

                    actual = if (row != null && row["actual"]!= null) row["actual"].toString().toInt() else 0

//                    if (actual == 0) continue

                    accumulate += actual
                    val rate = if (target != 0) (accumulate.toFloat() / target.toFloat() * 100).toInt().toString() + "%" else ""

                    // 아직 해당 시간이 안됐으면 공백으로 표시
                    val time_row = hashMapOf(
                        "type" to "DATA",
                        "name" to stime + " - " + etime,
                        "target" to target_txt,
                        "actual" to if (blank_yn) "" else actual.toString(),
                        "accumulate" to if (blank_yn) "" else accumulate.toString(),
                        "rate" to if (blank_yn) "" else rate
                    )

                    // Shift가 두개만 있으면 각 Shift가 좌우로 표시되고 세개면 계산해서 12개씩 표시한다.
                    if (target_size==2) {
                        if (i==0) _list.add(time_row)
                        else _list2.add(time_row)
                    } else {
                        if (index < 13) _list.add(time_row)
                        else _list2.add(time_row)
                    }
                    index++
                }
            }
        }

        list_adapter = ListAdapter(this, _list)
        lv_reports.adapter = list_adapter

        list_adapter2 = ListAdapter(this, _list2)
        lv_reports2.adapter = list_adapter2
    }

    private fun initView() {
        tv_title.text = "PRODUCTION REPORT"

        val current_shift_time = AppGlobal.instance.get_current_shift_time()
        _current_time = OEEUtil.parseDateTime(current_shift_time?.getString("work_stime"))

        tv_current_date.text = _current_time.toString("yyyy-MM-dd")

        ib_arrow_l.setOnClickListener {
            _current_time = _current_time.plusDays(-1)
            tv_current_date.text = _current_time.toString("yyyy-MM-dd")
            updateView()
        }
        ib_arrow_r.setOnClickListener {
            _current_time = _current_time.plusDays(+1)
            tv_current_date.text = _current_time.toString("yyyy-MM-dd")
            updateView()
        }
        ib_calendar.setOnClickListener {
            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { datePicker, year, month, date ->
                _current_time = DateTime().withYear(year).withMonthOfYear(month+1).withDayOfMonth(date)
                tv_current_date.text = _current_time.toString("yyyy-MM-dd")
                updateView()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE))
            dialog.datePicker.maxDate = Date().getTime()    //입력한 날짜 이후로 클릭 안되게 옵션
            dialog.show()
        }
        btn_production_report_exit.setOnClickListener { finish() }
    }

    private fun sendPing() {
        if (AppGlobal.instance.get_server_ip() == "") return
        val uri = "/ping.php"
        request(this, uri, false, false, null, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                btn_server_state.isSelected = true
                AppGlobal.instance._server_state = true
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }, {
            btn_server_state.isSelected = false
        })
    }

    /////// 쓰레드
    private val _timer_task1 = Timer()          // 서버 접속 체크 ping test.

    private fun start_timer() {
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    sendPing()
                }
            }
        }
        _timer_task1.schedule(task1, 5000, 10000)
    }
    private fun cancel_timer () {
        _timer_task1.cancel()
    }

    class ListAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

        private var _list: ArrayList<HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_item_report, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            if (_list[position]["type"]=="HEAD") {
                vh.ll_report_head.visibility = View.VISIBLE
                vh.ll_report_data.visibility = View.GONE
                vh.ll_report_nodata.visibility = View.GONE

                vh.tv_shift_name.text = _list[position]["name"]
                vh.tv_shift_target.text = _list[position]["target"]

            } else if (_list[position]["type"]=="DATA") {
                vh.ll_report_head.visibility = View.GONE
                vh.ll_report_data.visibility = View.VISIBLE
                vh.ll_report_nodata.visibility = View.GONE

                vh.tv_report_item_time.text = _list[position]["name"]
                vh.tv_report_item_target.text = _list[position]["actual"]
                vh.tv_report_item_product.text = _list[position]["accumulate"]
                vh.tv_report_item_rate.text = _list[position]["rate"]

            } else {
                vh.ll_report_head.visibility = View.GONE
                vh.ll_report_data.visibility = View.GONE
                vh.ll_report_nodata.visibility = View.VISIBLE
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val ll_report_head: LinearLayout
            val ll_report_data: LinearLayout
            val ll_report_nodata: LinearLayout
            val tv_shift_name: TextView
            val tv_shift_target: TextView
            val tv_report_item_time: TextView
            val tv_report_item_target: TextView
            val tv_report_item_product: TextView
            val tv_report_item_rate: TextView

            init {
                this.ll_report_head = row?.findViewById<LinearLayout>(R.id.ll_report_head) as LinearLayout
                this.ll_report_data = row?.findViewById<LinearLayout>(R.id.ll_report_data) as LinearLayout
                this.ll_report_nodata = row?.findViewById<LinearLayout>(R.id.ll_report_nodata) as LinearLayout
                this.tv_shift_name = row?.findViewById<TextView>(R.id.tv_shift_name) as TextView
                this.tv_shift_target = row?.findViewById<TextView>(R.id.tv_shift_target) as TextView
                this.tv_report_item_time = row?.findViewById<TextView>(R.id.tv_report_item_time) as TextView
                this.tv_report_item_target = row?.findViewById<TextView>(R.id.tv_report_item_target) as TextView
                this.tv_report_item_product = row?.findViewById<TextView>(R.id.tv_report_item_product) as TextView
                this.tv_report_item_rate = row?.findViewById<TextView>(R.id.tv_report_item_rate) as TextView
            }
        }
    }
}