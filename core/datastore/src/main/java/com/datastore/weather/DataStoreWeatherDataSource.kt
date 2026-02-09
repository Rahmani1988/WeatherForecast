package com.datastore.weather

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.common.model.models.WeatherSyncModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_cache")

private val CITY = stringPreferencesKey("weather_city")
private val SUMMARY = stringPreferencesKey("weather_summary")

@Singleton
class DataStoreWeatherDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : WeatherLocalDataSource {

    private val dataStore = context.dataStore

    override val weatherFlow: Flow<WeatherSyncModel> = dataStore.data
        .map { preferences ->
            WeatherSyncModel(
                city = preferences[CITY].orEmpty(),
                summary = preferences[SUMMARY].orEmpty()
            )
        }

    override suspend fun saveWeather(model: WeatherSyncModel) {
        context.dataStore.edit { preferences ->
            preferences[CITY] = model.city
            preferences[SUMMARY] = model.summary
        }
    }
}