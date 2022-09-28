package com.linx.kahabatteryapp.blue

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.linx.kahabatteryapp.App
import com.linx.kahabatteryapp.bean.DeviceBean

/**
 *author : linxun
 *create on 2022/9/5
 *explain:蓝牙扫描工具类
 */
class BluetoothScanUtils {

    companion object {
        val INSTANCE: BluetoothScanUtils by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { BluetoothScanUtils() }
    }

    private var mBluetoothManager: BluetoothManager? = null

    private var mBluetoothAdapter: BluetoothAdapter? = null

    private var mDeviceCallback: DeviceCallback? = null

    private var isScanning = false

    private val mHandle by lazy { Handler(Looper.getMainLooper()) }

    private val DEFAULT_SCAN_TIME: Long = 15000L

    private var mFilterName = ""

    /**
     * 默认扫描时长
     */
    private var mDefaultScanTime = DEFAULT_SCAN_TIME

    fun init(context: Context): Boolean {
        mBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager?.adapter
        return mBluetoothAdapter != null
    }

    /**
     * 设置扫描时长
     */
    fun setScanTime(time: Long) {
        this.mDefaultScanTime = time
    }

    /**
     * 设置过滤的的条件
     */
    fun addFilterName(name: String) {
        this.mFilterName = name
    }

    /**
     * 蓝牙是否开启
     */
    fun isEnabled(): Boolean {
        if (mBluetoothAdapter == null) {
            return false
        }
        return mBluetoothAdapter?.isEnabled == true
    }

    fun addDeviceCallbackListener(callback: DeviceCallback) {
        this.mDeviceCallback = callback
    }

    /**
     * 扫描蓝牙设备
     */
    fun scanBlueDevice() {
        if (isScanning) {
            stopScan()
        }
        isScanning = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                App.instance,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            mDeviceCallback?.onError("Bluetooth scan permission denied")
            return
        }
        mBluetoothAdapter?.bluetoothLeScanner?.startScan(mScanCallback)
        mHandle.postDelayed(
            {
                isScanning = false
                mBluetoothAdapter?.bluetoothLeScanner?.stopScan(mScanCallback)
                mDeviceCallback?.onScanStop()
            }, mDefaultScanTime
        )
    }

    /**
     * 是否在扫描中
     */
    fun isScanning() = isScanning

    /**
     * 停止扫描设备
     */
    fun stopScan() {
        if (isScanning) {
            isScanning = false
            mHandle.removeCallbacksAndMessages(null)
            mDeviceCallback?.onScanStop()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                    App.instance,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                mDeviceCallback?.onError("Bluetooth scan permission denied")
                return
            }
            mBluetoothAdapter?.bluetoothLeScanner?.stopScan(mScanCallback)
        }
    }


    private val mScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                    App.instance,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                mDeviceCallback?.onError("Bluetooth scan permission denied")
                return
            }
            result?.let {
                val device = it.device
                if (device != null && device.name?.isNotEmpty() == true) {
                    if (mFilterName.isNotEmpty()) {
                        val deviceName = device.name ?: ""
                        if (deviceName.contains(mFilterName)) {
                            mDeviceCallback?.onScanResult(
                                DeviceBean(
                                    device.name,
                                    device.address,
                                    it.rssi
                                )
                            )
                        }
                        return
                    }
                    mDeviceCallback?.onScanResult(
                        DeviceBean(
                            it.device.name,
                            it.device.address,
                            it.rssi
                        )
                    )

                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }
    }

    interface DeviceCallback {
        fun onScanResult(device: DeviceBean)
        fun onScanStop()
        fun onError(msg: String)
    }
}