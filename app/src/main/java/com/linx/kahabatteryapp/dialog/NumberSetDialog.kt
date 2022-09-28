package com.linx.kahabatteryapp.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import com.linx.kahabatteryapp.R
import com.linx.kahabatteryapp.utils.SharePerferenceUtils
import com.linx.kahabatteryapp.utils.Utils

/**
 *author : linxun
 *create on 2022/9/5
 *explain: 发送定时弹窗
 */
abstract class NumberSetDialog(context: Context) :
    Dialog(context, R.style.DefaultDialog), View.OnClickListener {

    private var titleTxt: TextView? = null

    private var numberPicker: NumberPicker? = null

    private var switchButton: SwitchCompat? = null
    private var send: Button? = null

   private val READ_BATTERY_CMD = "08010400"


   private var timerSet = 5

    override fun onStop() {
        super.onStop()
        SharePerferenceUtils.putBoolean(context, "timer", false) //是否显示定时器
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_set_number)
        setCanceledOnTouchOutside(true) // 外部点击取消
        initView()
        initData()
    }

    private fun initData() {
        val interval = arrayOf("5", "10", "20", "30", "60","300","600")
        numberPicker?.displayedValues = interval
        numberPicker?.minValue = 0
        numberPicker?.maxValue = interval.size - 1
        numberPicker?.setOnValueChangedListener { numberPicker: NumberPicker?, i: Int, i1: Int ->
            timerSet = interval[i1].toInt()
            Log.d(TAG, "onCreate: $timerSet")
        }
        setTitle(titleTxt)
        switchButton?.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                numberPicker?.visibility = View.VISIBLE
            } else {
                numberPicker?.visibility = View.GONE
                cancelTimer()
            }
        }
    }

    private fun initView() {
        switchButton?.isChecked = SharePerferenceUtils.getBoolean(
            context,
            "switch_timing",
            false
        )
        if (switchButton?.visibility == View.VISIBLE) numberPicker?.visibility = View.VISIBLE
        if (SharePerferenceUtils.getBoolean(context, "timer", false)) {
            switchButton?.visibility = View.VISIBLE
        }
        val window = this.window
        if (window != null) {
            window.setGravity(Gravity.CENTER)
            val lp = window.attributes
            lp.width = Utils.dp2px(200f) //设置宽度
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = lp
        }

        numberPicker=findViewById(R.id.interval_picker)
        titleTxt=findViewById(R.id.txt_title)
        switchButton=findViewById(R.id.scOpenTimer)
        send=findViewById(R.id.btn_set)
        send?.setOnClickListener(this)


    }

   override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_set -> {
                SharePerferenceUtils.putBoolean(context, "switch_timing",
                    switchButton?.isChecked == true
                )
                if (switchButton?.isChecked ==true) {
                    setTimer(timerSet * 1000, READ_BATTERY_CMD)
                } else {
                    sendCmdData(READ_BATTERY_CMD)
                    cancelTimer()
                }
                cancel()
            }
        }
    }

    override fun cancel() {
        super.cancel()
        SharePerferenceUtils.putBoolean(context, "timer", false)
    }

    abstract fun sendCmdData(data: String?)
    abstract fun setTitle(titleTxt: TextView?)
    abstract fun setTimer(interval: Int, s: String?)
    abstract fun cancelTimer()

    companion object {
        private val TAG = NumberSetDialog::class.java.name
    }

}
