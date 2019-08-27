package com.suntech.iot.pattern

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.suntech.iot.pattern.base.BaseFragment
import com.suntech.iot.pattern.common.AppGlobal
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.layout_bottom_info.*
import kotlinx.android.synthetic.main.layout_top_menu.*

class HomeFragment : BaseFragment() {

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
        tv_app_version.text = "v " + activity.packageManager.getPackageInfo(activity.packageName, 0).versionName

        btn_count_view.setOnClickListener {
//            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
//                Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
//            } else {
                (activity as MainActivity).changeFragment(1)
//            }
        }
        // 신버전. 디자인 기능으로 대체됨
        btn_component_info.setOnClickListener { designInfofunc() }
        // 구버전. 콤포넌트 기능 삭제됨
//        btn_component_info.setOnClickListener {
////            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
////                Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
////            } else {
//                (activity as MainActivity).countViewType = 2
//                (activity as MainActivity).changeFragment(1)
////                val intent = Intent(activity, ComponentInfoActivity::class.java)
////                getBaseActivity().startActivity(intent, { r, c, m, d ->
////                    if (r && d != null) {
////                        (activity as MainActivity).countViewType = 2
////                        (activity as MainActivity).changeFragment(1)
////
////                        val wosno = d!!["wosno"]!!
////                        val styleno = d["styleno"]!!.toString()
////                        val model = d["model"]!!.toString()
////                        val size = d["size"]!!.toString()
////                        val target = d["target"]!!.toString()
////                        val actual = d["actual"]!!.toString()
////
////                        (activity as MainActivity).startComponent(wosno, styleno, model, size, target, actual)
////                    }
////                })
////            }
//        }
        btn_work_info.setOnClickListener {
            if (AppGlobal.instance.get_factory() == "" || AppGlobal.instance.get_room() == "" || AppGlobal.instance.get_line() == "") {
                Toast.makeText(activity, getString(R.string.msg_no_setting), Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(activity, WorkInfoActivity::class.java))
            }
        }
        btn_setting_view.setOnClickListener { startActivity(Intent(activity, SettingActivity::class.java)) }

        updateView()

        autoSettingCheck()      // 앱 처음 실행시 세팅 안된 메뉴를 실행하기 위함.
    }

    override fun onSelected() {
        activity?.tv_title?.visibility = View.GONE
        updateView()
    }

    private fun designInfofunc() {
        if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
            Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
        } else {
//            btn_component_info.isEnabled = false
            val intent = Intent(activity, DesignInfoActivity::class.java)
            getBaseActivity().startActivity(intent, { r, c, m, d ->
//                btn_component_info.isEnabled = true
                if (r && d!=null) {
                    val idx = d!!["idx"]!!
                    val cycle_time = d["ct"]!!.toInt()
                    val model = d["model"]!!.toString()
                    val article = d["article"]!!.toString()
                    val material_way = d["material_way"]!!.toString()
                    val component = d["component"]!!.toString()

                    (activity as MainActivity).startNewProduct(idx, cycle_time, model, article, material_way, component)
                }
            })
        }
    }

    private fun autoSettingCheck() {
        if (AppGlobal.instance.get_auto_setting()) {
            if (AppGlobal.instance.get_factory() == "" || AppGlobal.instance.get_room() == "" || AppGlobal.instance.get_line() == "") {
                val intent = Intent(activity, SettingActivity::class.java)
                getBaseActivity().startActivity(intent, { r, c, m, d ->
                    autoOperatorDetailCheck()
                })
            } else {
                autoOperatorDetailCheck()
            }
        }
    }
    private fun autoOperatorDetailCheck() {
        if (AppGlobal.instance.get_auto_setting()) {
            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                val intent = Intent(activity, WorkInfoActivity::class.java)
                getBaseActivity().startActivity(intent, { r, c, m, d ->
                    autoDesignInfoCheck()
                })
            } else {
                autoDesignInfoCheck()
            }
        }
    }
    private fun autoDesignInfoCheck() {
        if (AppGlobal.instance.get_auto_setting()) {
            if (AppGlobal.instance.get_design_info_idx() == "") {
                AppGlobal.instance.set_auto_setting(false)
                designInfofunc()
            }
        }
    }

    private fun updateView() {
        tv_factory.text = AppGlobal.instance.get_factory()
        tv_room.text = AppGlobal.instance.get_room()
        tv_line.text = AppGlobal.instance.get_line()
        tv_mc_no.text = AppGlobal.instance.get_mc_no1() //+ "-" + AppGlobal.instance.get_mc_no2()
//        tv_mc_model.text = AppGlobal.instance.get_mc_model()
        tv_employee_no.text = AppGlobal.instance.get_worker_no()
        tv_employee_name.text = AppGlobal.instance.get_worker_name()
        tv_shift.text = AppGlobal.instance.get_current_shift_name()
    }
}