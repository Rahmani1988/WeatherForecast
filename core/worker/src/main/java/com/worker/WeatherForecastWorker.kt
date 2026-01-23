package com.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.datastore.UserPreferenceManager
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.network.ApiService
import com.network.models.reponse.currentweather.toWeatherSummaryPair
import com.notification.NotificationHandler
import com.reza.threading.common.IoDispatcher
import com.worker.initializers.WorkerConstraints
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class WeatherForecastWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiService: ApiService,
    private val userPreferenceManager: UserPreferenceManager,
    private val notificationHandler: NotificationHandler,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CoroutineWorker(appContext, workerParams) {

    private val dataClient by lazy { Wearable.getDataClient(appContext) }

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        val coordinates = userPreferenceManager.userCoordinatesFlow.firstOrNull()

        if (coordinates?.latitude == null || coordinates.longitude == null) {
            Log.e(TAG, "No user coordinates found to fetch weather.")
            Result.failure()
        } else {
            val location = "${coordinates.latitude},${coordinates.longitude}"
            try {
                val response = apiService.fetchCurrentWeather(location = location)
                if (response.isSuccessful && response.body() != null) {
                    val (city, weatherSummary) = response.body()!!.toWeatherSummaryPair()

                    // todo working on bridging to wear os
                    sendWeatherToWatch(city, weatherSummary)

                    if (notificationHandler.isNotificationsEnabled()) {
                        notificationHandler.postWeatherForecastNotification(
                            location = city,
                            weatherForecast = weatherSummary
                        )

                        Result.success()
                    } else {
                        Log.e(TAG, "Notification is disabled.")
                        Result.failure()
                    }
                } else {
                    Log.e(TAG, "Weather API failed with code: ${response.code()}")
                    Result.retry()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching weather forecast. $e")
                Result.retry()
            }
        }
    }

    // todo working on bridging to wear app
    private suspend fun sendWeatherToWatch(city: String, summary: String) {
        try {
            // Create the request and immediately convert/send it in one chain
            val request = PutDataMapRequest.create("/current_weather").run {
                dataMap.putString("city", city)
                dataMap.putString("summary", summary)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                asPutDataRequest().setUrgent()
            }

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Successfully synced weather to Wear OS")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync weather to Wear OS", e)
        }
    }

    companion object {
        private const val TAG = "WeatherForecastWorker"

        fun startUpWork() = PeriodicWorkRequestBuilder<DelegatingWorker>(
            repeatInterval = 12,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(WorkerConstraints)
            .setInputData(WeatherForecastWorker::class.delegatedData()).build()
    }
}