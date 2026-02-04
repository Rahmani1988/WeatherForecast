package com.sync.repository

import com.common.model.constants.WearSyncConfig
import com.common.model.models.WeatherSyncModel
import com.common.repository.WearSyncRepository
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DefaultWearSyncRepository @Inject constructor(
    private val dataClient: DataClient
) : WearSyncRepository {

    override suspend fun syncWeather(city: String, summary: String): Result<Unit> = runCatching {
        val model = WeatherSyncModel(city, summary)

        val request = PutDataMapRequest.create(WearSyncConfig.WEATHER_PATH).apply {
            dataMap.putWeatherInfo(model)
        }.asPutDataRequest()

        // Marking as urgent ensures the data is synced immediately, rather than waiting for a battery-optimized batch.
        request.setUrgent()

        dataClient.putDataItem(request).await()
    }.map { Unit }
    // We map to Unit to hide the GMS specific return types from the caller
}

private fun DataMap.putWeatherInfo(model: WeatherSyncModel) {
    putString(WearSyncConfig.Keys.CITY, model.city)
    putString(WearSyncConfig.Keys.SUMMARY, model.summary)
    putLong(WearSyncConfig.Keys.TIMESTAMP, model.timestamp)
}