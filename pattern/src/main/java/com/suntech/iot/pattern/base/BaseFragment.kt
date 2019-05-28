package com.suntech.iot.pattern.base

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.AttributeSet
import android.view.View
import com.suntech.iot.pattern.base.BaseActivity

/**
 * Created by rightsna on 2016. 10. 7..
 */

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [BaseFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [BaseFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
open class BaseFragment : Fragment() {

    override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle) {
        super.onInflate(context, attrs, savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        updateViews()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    protected open fun initViews() {}

    protected open fun updateViews() {}

    protected fun getBaseActivity() : BaseActivity {
        return activity as BaseActivity
    }

    open fun onSelected() {}
}
