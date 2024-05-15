package com.jetbeep.app.lockersdkdemo

import android.app.Application
import android.util.Log
import com.jetbeep.lockersdk.LockerSdk
import com.jetbeep.lockersdk.config.Environment
import com.jetbeep.lockersdk.config.dsl.config


class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // init locker sdk
        LockerSdk.init(applicationContext, config {
            environment = Environment.PRODUCTION
            logLevel = Log.INFO
        })
    }
}