package com.linx.kahabatteryapp.utils

import com.linx.kahabatteryapp.App
import java.text.SimpleDateFormat
import java.util.*

/**
 *author : linxun
 *create on 2022/9/5
 *explain:
 */
object Utils {
    fun dp2px(dpValue: Float): Int {
        val scale = App.instance.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
    fun getCurrentTime(): String? {
        val formatter = SimpleDateFormat("HH:mm:ss")
        val curDate = Date(System.currentTimeMillis())
        return formatter.format(curDate)
    }

    fun getCountCurrentDate(): String {
        val formatter1 = SimpleDateFormat("yyyyMMdd")
        val formatter2 = SimpleDateFormat("HHmmss")
        val curDate = Date(System.currentTimeMillis())
        return formatter1.format(curDate) + "_" + formatter2.format(curDate)
    }
}