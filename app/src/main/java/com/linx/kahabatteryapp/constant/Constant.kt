package com.linx.kahabatteryapp.constant

import java.util.*

/**
 *author : linxun
 *create on 2022/5/13
 *explain:常量
 */
interface Constant {
    companion object {
        const val DEVICE_ADDRESS = "device_address"
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val CHARACTERISTIC_READ_SUCCESS = "CHARACTERISTIC_READ_SUCCESS"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
        const val ACTION_STOP_RECEIVE = "ACTION_STOP_RECEIVE"
        const val CHARACTERISTIC_WRITE_FAILURE = "CHARACTERISTIC_WRITE_FAILURE"
        const val CHARACTERISTIC_WRITE_SUCCESS = "CHARACTERISTIC_WRITE_SUCCESS"
        const val GATT_SERVICE_NOT_FOUND = "GATT_SERVICE_NOT_FOUND"
        val UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        var DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb"
        var FIRMWARE_VERSION = "00002a26-0000-1000-8000-00805f9b34fb"
        var HARDWARE_VERSION = "00002a27-0000-1000-8000-00805f9b34fb"
        var NORDIC_UART = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
        var TX_CHARACTERISTIC = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
        var CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
        var RX_CHARACTERISTIC = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"

        const val OTA_SERVICE_UUID = "0000d0ff-3c17-d293-8e48-14fe2e4da212"

        const val ADDRESS = "address"
        const val CHARACTERISTIC_UUID = "characteristic_uuid"
        const val READ_VALUE = "read_value"

    }
}