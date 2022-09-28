package com.linx.kahabatteryapp.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.linx.kahabatteryapp.ui.activity.MainActivity
import com.linx.kahabatteryapp.R
import com.linx.kahabatteryapp.base.BaseActivity

/**
 * @author：linxun
 * @exception：开屏页
 * @create on:2022.09.06
 */
class SplashActivity : BaseActivity() {

    private val mHandle by lazy { Handler(Looper.getMainLooper()) }
    private val DELAY_TIME: Long = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        mHandle.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, DELAY_TIME)
    }
}