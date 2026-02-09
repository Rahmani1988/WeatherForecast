package com.datastore.user

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Delegation property for DataStore instance (file name)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

// Preference Key
private val USER_LATITUDE_KEY = doublePreferencesKey("user_latitude")
private val USER_LONGITUDE_KEY = doublePreferencesKey("user_longitude")

@Singleton
class DataStoreUserPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferenceManager {

    private val dataStore = context.dataStore

    override val userCoordinatesFlow: Flow<Coordinates> = dataStore.data
        .map { preferences ->
            val latitude = preferences[USER_LATITUDE_KEY]
            val longitude = preferences[USER_LONGITUDE_KEY]
            Coordinates(latitude, longitude)
        }

    override suspend fun saveUserCoordinates(latitude: Double, longitude: Double) {
        dataStore.edit { preferences ->
            preferences[USER_LATITUDE_KEY] = latitude
            preferences[USER_LONGITUDE_KEY] = longitude
        }
    }
}