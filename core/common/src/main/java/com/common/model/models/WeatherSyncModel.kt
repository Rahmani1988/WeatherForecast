package com.common.model.models

data class WeatherSyncModel(
    val city: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)