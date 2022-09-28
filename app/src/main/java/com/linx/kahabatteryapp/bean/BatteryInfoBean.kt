package com.linx.kahabatteryapp.bean

import com.linx.kahabatteryapp.utils.Utils

/**
 *author : linxun
 *create on 2022/9/6
 *explain:
 */
class BatteryInfoBean(electricity: Int, voltage: Float) {
    private var time: String
    private var electricity: Int
    private var voltage: Float

    fun getTime(): String {
        return time
    }

    fun setTime(time: String) {
        this.time = time
    }

    fun getElectricity(): Int {
        return electricity
    }

    fun setElectricity(electricity: Int) {
        this.electricity = electricity
    }

    fun getVoltage(): Float {
        return voltage
    }

    fun setVoltage(voltage: Float) {
        this.voltage = voltage
    }

    override fun toString(): String {
        return "BatteryInfoBean{" + "time='" + time + '\'' +
                ", electricity=" + electricity +
                ", voltage=" + voltage +
                '}'
    }

    init {
        time = Utils.getCurrentTime()?:""
        this.electricity = electricity
        this.voltage = voltage
    }
}
