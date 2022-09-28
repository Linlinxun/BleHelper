package com.linx.kahabatteryapp.ui.battery

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.linx.kahabatteryapp.R
import com.linx.kahabatteryapp.base.BaseActivity
import com.linx.kahabatteryapp.bean.BatteryInfoBean
import com.linx.kahabatteryapp.constant.Constant
import com.linx.kahabatteryapp.databinding.ActivityBatteryInfoBinding
import com.linx.kahabatteryapp.dialog.NumberSetDialog
import com.linx.kahabatteryapp.utils.FileHelper
import com.linx.kahabatteryapp.utils.HideSoftInputUtil
import com.linx.kahabatteryapp.utils.SharePerferenceUtils
import com.linx.kahabatteryapp.utils.StringUtil

/**
 * @author：linxun
 * @exception：获取电量信息
 * @create on:2022.09.05
 */
class BatteryInfoActivity : BaseActivity() {
    private lateinit var binding: ActivityBatteryInfoBinding

    private var deviceAddress = ""

    private val mHandle = Handler(Looper.getMainLooper())

    private var isTiming = false

    private var timerRunnable: Runnable? = null

    private val CONNECT_TIME_OUT: Long = 40 * 1000L

    private val DELAY_TIME: Long = 1500L

    private var isConnecting = false

    private val batteryInfoList = mutableListOf<BatteryInfoBean>()

    private var mBackPressed: Long = 0

    private val TIME_EXIT = 1500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatteryInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.materialToolbar)
        supportActionBar?.title = ""
        binding.materialToolbar.run {
            setNavigationOnClickListener { finish() }
        }
        initView()
    }

    private fun initView() {
        SharePerferenceUtils.putBoolean(this, "switch_timing", false)
        SharePerferenceUtils.putBoolean(this, "start_timing", false)
        deviceAddress = intent?.getStringExtra(Constant.ADDRESS) ?: ""
        showProgressBar(true)
        mHandle.postDelayed({
            connect(deviceAddress)
        }, DELAY_TIME)
        mHandle.postDelayed({
            if (!isConnecting) {
                showProgressBar(false)
                displayContent(" Connection timed out, please try again")
            }
        }, CONNECT_TIME_OUT)
    }

    override fun onConnectSuccess(address: String) {
        super.onConnectSuccess(address)
        isConnecting = true
        showProgressBar(false)
        displayContent(" Connected")
    }

    override fun onDisConnectSuccess(address: String) {
        super.onDisConnectSuccess(address)
        isConnecting = false
        showProgressBar(false)
        displayContent(" Disconnected")
    }

    override fun onReceiveData(intent: Intent, address: String) {
        super.onReceiveData(intent, address)
        val data = intent.getStringExtra(Constant.EXTRA_DATA)
        if (data == null) {
            displayContent("Receive: data=null")
            return
        }
        val dataList = data.split(" ").toTypedArray()
        val builder = StringBuilder()
        for (i in dataList.indices) builder.append(dataList[i]).append(" ")
        displayContent("Receive: $builder")
        dealWithData(dataList)
    }

    private fun dealWithData(dataList: Array<String>) {
        if (dataList[0] =="88"&& dataList[1] =="01"){
            val batteryPercent = dataList[4]
            val voltage = (dataList[6] + dataList[5]).toInt(16).toFloat()
            val electricity = batteryPercent.toInt(16)
            val mVoltage = voltage / 1000
            displayContent("Battery percent: $electricity% Voltage: $mVoltage")
            batteryInfoList.add(BatteryInfoBean(electricity, mVoltage))
        }
    }

    override fun onResume() {
        super.onResume()
        HideSoftInputUtil.hideSoftInputMethod(binding.displayEdit)
    }

    private fun displayContent(text: String) {
        binding.displayEdit.append(text + "\n")
    }

    private fun showProgressBar(isShow: Boolean) {
        if (isShow) {
            binding.llShowProgressView.visibility = View.VISIBLE
        } else {
            binding.llShowProgressView.visibility = View.INVISIBLE
        }
    }

    private fun displayCmd(cmd: ByteArray) {
        val builder = StringBuilder()
        cmd.forEachIndexed { index, byte ->
            builder.append(String.format("%02X ", byte))
        }
        displayContent("Send:$builder")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_battery, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.getInfo -> {
                if (!isConnecting){
                    Toast.makeText(this, "Not connected yet", Toast.LENGTH_SHORT).show()
                    return true
                }
                SharePerferenceUtils.putBoolean(this, "timer", true)
                val dialog = object : NumberSetDialog(this@BatteryInfoActivity) {
                    override fun sendCmdData(data: String?) {
                            val list: List<Byte> = StringUtil.stringToByte(data)
                            val cmdList = ByteArray(list.size)
                            for (i in list.indices) {
                                cmdList[i] = list[i]
                            }
                            displayCmd(cmdList)
                            writeCharacteristic(deviceAddress, cmdList)
                    }

                    override fun setTitle(titleTxt: TextView?) {
                        titleTxt?.text = "Get battery info regularly"
                    }

                    override fun setTimer(interval: Int, s: String?) {
                        isTiming = true
                        SharePerferenceUtils.putBoolean(context, "start_timing", true)
                        timerRunnable = Runnable {
                            if (!isTiming) {
                                return@Runnable
                            }
                            val list: List<Byte> = StringUtil.stringToByte(s)
                            val cmdList = ByteArray(list.size)
                            for (i in list.indices) {
                                cmdList[i] = list[i]
                            }
                            displayCmd(cmdList)
                            writeCharacteristic(deviceAddress, cmdList)
                            mHandle.postDelayed(timerRunnable!!, interval.toLong())
                        }
                        mHandle.postDelayed(timerRunnable!!, 0)
                        displayContent("Enable scheduled sending: " + interval / 1000 + " Sec")
                    }

                    override fun cancelTimer() {
                        isTiming = false
                        timerRunnable?.let { mHandle.removeCallbacks(it) } //移除定时器
                        if (SharePerferenceUtils.getBoolean(
                                context,
                                "start_timing",
                                false
                            )
                        ) //是否开启了定时器
                            displayContent("Cancel scheduled delivery")
                        SharePerferenceUtils.putBoolean(context, "switch_timing", false) //是否打开开关
                        SharePerferenceUtils.putBoolean(context, "start_timing", false)
                    }

                }
                dialog.show()
                return true
            }
            R.id.export -> {
                if (batteryInfoList.size<=0){
                    Toast.makeText(this, "current data is empty", Toast.LENGTH_SHORT).show()
                    return true
                }
                 if(mBackPressed + TIME_EXIT > System.currentTimeMillis()) {

                }else{
                     FileHelper.getInstance().resetTimeName()
                     saveData()
                     mBackPressed = System.currentTimeMillis()
                 }
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item);

    }

    private fun saveData() {
        if (batteryInfoList.size > 0) {
            FileHelper.getInstance().saveBatteryInfoExcel(this, deviceAddress, batteryInfoList)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        saveData()
        FileHelper.getInstance().resetTimeName()
        bluetoothService?.disconnect(deviceAddress)
        mHandle.removeCallbacksAndMessages(null)
    }

}