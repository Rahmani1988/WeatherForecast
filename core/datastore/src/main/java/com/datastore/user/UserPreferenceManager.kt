package com.datastore.user

import kotlinx.coroutines.flow.Flow

interface UserPreferenceManager {

    /**
     * Flow of the user's coordinates.
     */
    val userCoordinatesFlow: Flow<Coordinates?>

    /**
     * Saves the user's coordinates.
     */
    suspend fun saveUserCoordinates(latitude: Double, longitude: Double)
}

data class Coordinates(
    val latitude: Double? = null,
    val longitude: Double? = null
)