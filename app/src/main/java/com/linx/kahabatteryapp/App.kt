package com.linx.kahabatteryapp

import android.app.Application

/**
 *author : linxun
 *create on 2022/9/5
 *explain:
 */
class App :Application() {

    companion object{
       lateinit var instance:Application
    }

    override fun onCreate() {
        super.onCreate()
          instance=this
    }
}