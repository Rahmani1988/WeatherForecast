package com.reza.weatherforcast

import android.util.Log
import com.common.model.constants.WearSyncConfig
import com.common.model.models.WeatherSyncModel
import com.datastore.weather.WeatherLocalDataSource
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WeatherDataListenerService : WearableListenerService() {

    @Inject
    lateinit var weatherDataSource: WeatherLocalDataSource

    @Inject
    lateinit var coroutineScope: CoroutineScope

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == WearSyncConfig.WEATHER_PATH
            ) {
                DataMapItem.fromDataItem(event.dataItem).dataMap.apply {
                    val city = getString(WearSyncConfig.Keys.CITY).orEmpty()
                    val summary = getString(WearSyncConfig.Keys.SUMMARY).orEmpty()
                    val timestamp = getLong(WearSyncConfig.Keys.TIMESTAMP)
                    updateWatchWeather(city = city, summary = summary, timestamp = timestamp)
                }
            }
        }
    }

    private fun updateWatchWeather(city: String, summary: String, timestamp: Long) {
        Log.d("WearListener", "Received: city: $city, summary: $summary, timestamp: $timestamp")
        coroutineScope.launch {
            weatherDataSource.saveWeather(
                WeatherSyncModel(
                    city = city,
                    summary = summary,
                    timestamp = timestamp
                )
            )
        }
    }
}