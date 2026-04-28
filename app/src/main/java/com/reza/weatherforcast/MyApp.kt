package com.reza.weatherforcast

import android.app.Application
import com.notification.NotificationHandler
import com.worker.initializers.WorkInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp: Application() {

    @Inject
    lateinit var notificationHandler: NotificationHandler

    override fun onCreate() {
        super.onCreate()

        WorkInitializer.initialize(context = this, notificationHandler = notificationHandler)
    }
}