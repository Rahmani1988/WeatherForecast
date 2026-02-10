package com.weatherforcast

import android.util.Log
import com.datastore.weather.WeatherLocalDataSource
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
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
                event.dataItem.uri.path == "/current_weather") {

                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val city = dataMap.getString("city")
                val summary = dataMap.getString("summary")

                // TODO: Save this to your wear module's local DataStore or Database
                // This ensures the Wear UI has data even when disconnected from the phone
                Log.d("WearListener", "Received weather: $city, $summary")
            }
        }
    }
}