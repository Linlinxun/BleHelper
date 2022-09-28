package com.linx.kahabatteryapp.service

import android.Manifest
import android.app.*
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import com.linx.kahabatteryapp.App
import com.linx.kahabatteryapp.constant.Constant
import java.util.*

class BluetoothService : Service() {

    companion object {
        const val TAG = "BluetoothService"
    }

    private var hardVerChar: BluetoothGattCharacteristic? = null
    private var firmVerChar: BluetoothGattCharacteristic? = null

    private var mBluetoothManager: BluetoothManager? = null

    private var mBluetoothAdapter: BluetoothAdapter? = null

    /**
     * 记录连接到的蓝牙地址
     */
    private val gattMap: HashMap<String, BluetoothGatt> = HashMap()

    private var handler = Handler(Looper.getMainLooper())

    private var service: BluetoothGattService? = null

    private val myBinder: MyBinder by lazy { MyBinder() }

    inner class MyBinder : Binder() {
        fun getService(): BluetoothService {
            return this@BluetoothService
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return myBinder
    }


    override fun onCreate() {
        super.onCreate()
        init()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * 初始化蓝牙管理器
     */
    private fun init(): Boolean {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false
        }

        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        } else {
            return true
        }

        if (mBluetoothManager == null) {
            return false
        }

        mBluetoothAdapter = mBluetoothManager?.adapter


        if (mBluetoothAdapter == null) {
            return false
        }

        return true

    }


    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            return false
        }
        val device = mBluetoothAdapter?.getRemoteDevice(address) ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                App.instance,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw RuntimeException("Bluetooth scan permission denied")
        }
        // 如果已存在则移除
        val gatt = gattMap[address]
        if (gatt != null && gatt.device.address == address) {
            gatt.disconnect()
            gattMap.remove(address)
        }
        device.connectGatt(this, false, mGattCallback)
        return true
    }

    /**
     * 写入数据到蓝牙
     */
    fun writeCharacteristic(address: String, data: ByteArray?) {
        if (mBluetoothAdapter == null) {
            return
        }
        val gatt = gattMap[address] ?: return
        val service = gatt.getService(UUID.fromString(Constant.NORDIC_UART)) ?: return
        val rxCharacteristic =
            service.getCharacteristic(UUID.fromString(Constant.RX_CHARACTERISTIC)) ?: return
        rxCharacteristic.value = data
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                App.instance,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw RuntimeException("Bluetooth scan permission denied")
        }

        val success = gatt.writeCharacteristic(rxCharacteristic)
        if (!success) {
            broadcastUpdate(Constant.CHARACTERISTIC_WRITE_FAILURE, address)
        }

    }

    /**
     * 断开蓝牙连接
     */
    fun disconnect(address: String?) {
        if (mBluetoothAdapter == null) {
            return
        }
        val gatt = gattMap[address]
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                App.instance,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw RuntimeException("Bluetooth scan permission denied")
        }

        gatt?.disconnect()
        gattMap.remove(address)
    }


    fun close() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                App.instance,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw RuntimeException("Bluetooth scan permission denied")
        }

        for (gatt in gattMap.values) {
            gatt.disconnect()
            setTXCharacteristicNotification(gatt, false) //开启通知
            gatt.close()
        }
    }

    /**
     * 蓝牙连接回调方法
     */
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                    App.instance,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw RuntimeException("Bluetooth scan permission denied")
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) { // 收到连接回调
                val intentAction = Constant.ACTION_GATT_CONNECTED
                gattMap[gatt.device.address] = gatt
                gatt.discoverServices()
                broadcastUpdate(intentAction, gatt)

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) { // 收到断开回调
                val intentAction = Constant.ACTION_GATT_DISCONNECTED
                // 断开后将句柄从map中移除
                gattMap.remove(gatt.device.address)
                broadcastUpdate(intentAction, gatt)
                gatt.close() // 关闭句柄
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(
                    Constant.ACTION_GATT_SERVICES_DISCOVERED,
                    gatt
                )
                setTXCharacteristicNotification(gatt, true) //开启通知
                //获取固件版本
                handler.postDelayed({
                    try {
                        service = gatt.getService(UUID.fromString(Constant.DEVICE_INFORMATION))
                        //读取固件版本
                        firmVerChar =
                            service?.getCharacteristic(UUID.fromString(Constant.FIRMWARE_VERSION))
                        readCharacteristic(firmVerChar, gatt.device.address)
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    }
                }, 500)
                //获取硬件版本
                handler.postDelayed({
                    try {
                        service = gatt.getService(UUID.fromString(Constant.DEVICE_INFORMATION))
                        //读取硬件版本
                        hardVerChar =
                            service?.getCharacteristic(UUID.fromString(Constant.HARDWARE_VERSION))
                        readCharacteristic(hardVerChar, gatt.device.address)
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    }
                }, 1000)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) { //读取数据
            val bytes = characteristic.value
            val str = String(bytes)
            broadcastReadUpdate(Constant.CHARACTERISTIC_READ_SUCCESS, characteristic.uuid, str)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            broadcastUpdate(Constant.CHARACTERISTIC_WRITE_SUCCESS, gatt)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            // characteristic改变时发送广播
            broadcastUpdate(Constant.ACTION_DATA_AVAILABLE, characteristic, gatt)
        }
    }


    private fun broadcastUpdate(action: String, address: String) {
        val intent = Intent(action)
        intent.putExtra("device_address", address)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, gatt: BluetoothGatt) {
        val address = gatt.device.address
        val intent = Intent(action)
        intent.putExtra(Constant.DEVICE_ADDRESS, address)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(
        action: String, characteristic: BluetoothGattCharacteristic, gatt: BluetoothGatt
    ) {
        val intent = Intent(action)
        val address = gatt.device.address
        intent.putExtra(Constant.DEVICE_ADDRESS, address)
        if (Constant.UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
            val flag = characteristic.properties
            var format = -1
            if (flag and 0x01 != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8
            }
            val heartRate = characteristic.getIntValue(format, 1)
            intent.putExtra(Constant.EXTRA_DATA, heartRate.toString().trimEnd())
        } else {
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                val stringBuilder = StringBuilder(data.size)
                for (byteChar in data) {
                    stringBuilder.append(String.format("%02X ", byteChar))
                }
                intent.putExtra(Constant.EXTRA_DATA, stringBuilder.toString().trimEnd())
            }
        }
        sendBroadcast(intent)
    }

    private fun broadcastReadUpdate(ac: String, uuid: UUID, value: String) {
        val intent = Intent(ac)
        val uuidStr = uuid.toString()
        intent.putExtra(Constant.CHARACTERISTIC_UUID, uuidStr)
        intent.putExtra(Constant.READ_VALUE, value)
        Log.d(TAG, "broadcastReadUpdate: $value")
        sendBroadcast(intent)
    }


    fun setTXCharacteristicNotification(gatt: BluetoothGatt?, enabled: Boolean) {
        if (mBluetoothAdapter == null || gatt == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                App.instance,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw RuntimeException("Bluetooth scan permission denied")
        }
        val service = gatt.getService(UUID.fromString(Constant.NORDIC_UART))
        val txCharacteristic = service.getCharacteristic(UUID.fromString(Constant.TX_CHARACTERISTIC))
         gatt.setCharacteristicNotification(txCharacteristic, enabled)
        val descriptor = txCharacteristic.getDescriptor(UUID.fromString(Constant.CLIENT_CHARACTERISTIC_CONFIG))
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }



    private fun readCharacteristic(characteristic: BluetoothGattCharacteristic?, address: String?) {
        val gatt = gattMap[address]
        if (mBluetoothAdapter == null || gatt == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                App.instance,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw RuntimeException("Bluetooth scan permission denied")
        }

        gatt.readCharacteristic(characteristic)
    }

}



