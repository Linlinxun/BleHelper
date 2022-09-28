package com.linx.kahabatteryapp.base

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import com.linx.kahabatteryapp.constant.Constant
import com.linx.kahabatteryapp.service.BluetoothService

/**
 * @author：linxun
 * @exception：继承activity基类 连接蓝牙直接使用
 * @create:2022.09.05
 */
open class BaseActivity : AppCompatActivity(), ServiceConnection {

    var bluetoothService: BluetoothService? = null

    fun connect(address: String) {
        bluetoothService?.connect(address)
    }

    fun writeCharacteristic(deviceAddress: String, cmd: ByteArray) {
        bluetoothService?.writeCharacteristic(deviceAddress, cmd)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, BluetoothService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        // 注册广播
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constant.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(Constant.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(Constant.CHARACTERISTIC_READ_SUCCESS)
        intentFilter.addAction(Constant.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(Constant.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(Constant.ACTION_STOP_RECEIVE)
        intentFilter.addAction(Constant.CHARACTERISTIC_WRITE_FAILURE)
        intentFilter.addAction(Constant.CHARACTERISTIC_WRITE_SUCCESS)
        intentFilter.addAction(Constant.GATT_SERVICE_NOT_FOUND)
        registerReceiver(receiver, intentFilter)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val address = intent.getStringExtra(Constant.DEVICE_ADDRESS) ?: return
            when (intent.action) {
                // 处理连接广播
                Constant.ACTION_GATT_CONNECTED -> {
                    onConnectSuccess(address)
                }
                //连接断开
                Constant.ACTION_GATT_DISCONNECTED -> {
                    onDisConnectSuccess(address)
                }
                //找不到服务
                Constant.GATT_SERVICE_NOT_FOUND -> {
                }
                //真正建立了可通信的连接
                Constant.ACTION_GATT_SERVICES_DISCOVERED -> {
                    onRequest(address)
                }
                //读取设备成功
                Constant.CHARACTERISTIC_READ_SUCCESS -> {
                }
                //处理接收到的数据
                Constant.ACTION_DATA_AVAILABLE -> {
                    onReceiveData(intent, address)
                }
                //写入数据成功
                Constant.CHARACTERISTIC_WRITE_SUCCESS -> {
                    onWriteSuccess(address)
                }
                //写入数据失败
                Constant.CHARACTERISTIC_WRITE_FAILURE -> {
                    onWriteFail(address)
                }

            }
        }
    }

    open fun onReceiveData(intent: Intent, address: String) {}
    open fun onRequest(address: String) {}

    open fun onConnectSuccess(address: String) {}
    open fun onDisConnectSuccess(address: String) {}

    open fun onWriteSuccess(address: String) {}
    open fun onWriteFail(address: String) {}

    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        bluetoothService = (p1 as BluetoothService.MyBinder).getService()
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
    }

}