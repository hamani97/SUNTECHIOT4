package com.suntech.iot.pattern

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suntech.iot.pattern.base.BaseFragment
import com.suntech.iot.pattern.common.AppGlobal
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.layout_bottom_info.*
import kotlinx.android.synthetic.main.layout_top_menu.*

class HomeFragment : BaseFragment() {

    private var _running: Boolean = false

    private val _need_to_home_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateView()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_home_refresh, IntentFilter("need.refresh"))
        updateView()
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(_need_to_home_refresh)
    }

    override fun initViews() {
        tv_app_version?.text = "v " + activity.packageManager.getPackageInfo(activity.packageName, 0).versionName

        btn_count_view.setOnClickListener {
//            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
//                (activity as MainActivity).ToastOut(activity, R.string.msg_no_operator, true)
//                Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
//            } else {
                (activity as MainActivity).changeFragment(1)
//            }
        }
        // 디자인 기능으로 대체됨
        btn_component_info.setOnClickListener {
            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                (activity as MainActivity).ToastOut(activity, R.string.msg_no_operator, true)
            } else if (AppGlobal.instance.get_design_info() == null) {
                (activity as MainActivity).ToastOut(activity, R.string.msg_please_wait_for_loading, true)
            } else {
                designInfofunc()
            }
        }

        btn_work_info.setOnClickListener {
            if (AppGlobal.instance.get_factory() == "" || AppGlobal.instance.get_zone() == "" || AppGlobal.instance.get_line() == "") {
                (activity as MainActivity).ToastOut(activity, R.string.msg_no_setting, true)
            } else if (AppGlobal.instance.get_current_work_time() == null) {
                (activity as MainActivity).ToastOut(activity, R.string.msg_please_wait_for_loading, true)
            } else {
                startActivity(Intent(activity, WorkInfoActivity::class.java))
            }
        }
        btn_setting_view.setOnClickListener {
            getBaseActivity().startActivity(Intent(activity, SettingActivity::class.java), { r, c, m, d ->
                if (AppGlobal.instance.get_target_by_group()) (activity as MainActivity).fetchServerTargetData()     // 특정 업체를 위한 서버 타겟값 가져오기
            })
        }

        updateView()
    }

    override fun onSelected() {
        activity?.tv_title?.visibility = View.GONE
        updateView()
    }

    private fun designInfofunc() {
        _running = true
//        btn_component_info.isEnabled = false
        val intent = Intent(activity, DesignInfoActivity::class.java)
        getBaseActivity().startActivity(intent, { r, c, m, d ->
            _running = false
//            btn_component_info.isEnabled = true
            if (r && d!=null) {
                val idx = d!!["idx"]!!
                val cycle_time = (d["ct"]!!.trim())?.toInt()
                val model = d["model"]!!.toString()
                val article = d["article"]!!.toString()
                val stitch = d["stitch"]!!.toString()           // 베트남 공장의 전산에 보내야 할 값(새로생김)
                val material_way = d["material_way"]!!.toString()
                val component = d["component"]!!.toString()

                (activity as MainActivity).startNewProduct(idx, cycle_time, model, article, stitch, material_way, component)
            }
        })
    }

    private fun updateView() {
        tv_factory?.text = AppGlobal.instance.get_factory()
        tv_room?.text = AppGlobal.instance.get_zone()
        tv_line?.text = AppGlobal.instance.get_line()
        tv_mc_no?.text = AppGlobal.instance.get_mc_no() //+ "-" + AppGlobal.instance.get_mc_no2()
//        tv_mc_model?.text = AppGlobal.instance.get_mc_model()
        tv_employee_no?.text = AppGlobal.instance.get_worker_no()
        tv_employee_name?.text = AppGlobal.instance.get_worker_name()
        tv_shift?.text = AppGlobal.instance.get_current_shift_name()
    }
}