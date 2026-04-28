package com.reza.weatherforcast

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.notification.NotificationHandler
import com.reza.weatherforcast.navigation.AppNavHost
import com.reza.weatherforcast.ui.theme.WeatherForcastTheme
import com.worker.initializers.WorkInitializer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var notificationHandler: NotificationHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherForcastTheme {
                AppNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (notificationHandler.isNotificationsEnabled()) {
            Log.d("MainActivity", "Notifications enabled. Ensuring worker is enqueued.")
            WorkInitializer.initialize(context = this, notificationHandler = notificationHandler)
        } else {
            Log.d("MainActivity", "Notifications disabled. Canceling work.")
            WorkInitializer.cancelWork(this)
        }
    }
}