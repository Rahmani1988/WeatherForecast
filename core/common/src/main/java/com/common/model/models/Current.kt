package com.common.model.models

data class Current(
    val cloud: Int,
    val condition: Condition,
    val feelsLikeC: Double,
    val feelsLikeF: Double,
    val humidity: Int,
    val isDay: Int,
    val lastUpdated: String,
    val precipIn: Double,
    val precipMm: Double,
    val pressureIn: Double,
    val pressureMb: Double,
    val tempC: Double,
    val tempF: Double,
    val uv: Double,
    val windDegree: Int,
    val windDir: String,
    val windKph: Double,
    val windMph: Double,
    val windchillC: Double,
    val windchillF: Double
)