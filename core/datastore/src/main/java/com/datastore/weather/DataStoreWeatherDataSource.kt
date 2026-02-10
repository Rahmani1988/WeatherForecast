package com.datastore.weather

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.common.model.models.WeatherSyncModel
import com.reza.threading.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_cache")

private val CITY = stringPreferencesKey("weather_city")
private val SUMMARY = stringPreferencesKey("weather_summary")
private val TIMESTAMP = longPreferencesKey("weather_timestamp")

@Singleton
class DataStoreWeatherDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WeatherLocalDataSource {

    private val dataStore = context.dataStore

    override val weatherFlow: Flow<WeatherSyncModel> = dataStore.data
        .map { preferences ->
            WeatherSyncModel(
                city = preferences[CITY].orEmpty(),
                summary = preferences[SUMMARY].orEmpty(),
                timestamp = preferences[TIMESTAMP] ?: 0L
            )
        }

    override suspend fun saveWeather(model: WeatherSyncModel) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[CITY] = model.city
                preferences[SUMMARY] = model.summary
                preferences[TIMESTAMP] = model.timestamp
            }
        }
    }
}