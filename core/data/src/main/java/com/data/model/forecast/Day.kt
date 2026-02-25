package com.data.model.forecast

import com.common.model.models.Condition
import com.network.models.reponse.forecast.DayDTO

data class Day(
    val maxTempC: Double,
    val maxTempF: Double,
    val minTempC: Double,
    val minTempF: Double,
    val avgTempC: Double,
    val avgTempF: Double,
    val maxWindMph: Double,
    val maxWindKph: Double,
    val totalPrecipitationIn: Double,
    val totalSnowCm: Double,
    val avgVisKm: Double,
    val avgVisMiles: Double,
    val avgHumidity: Int,
    val dailyWillItRain: Int,
    val dailyChanceOfRain: Int,
    val dailyWillItSnow: Int,
    val dailyChanceOfSnow: Int,
    val condition: Condition,
    val uv: Double
)

fun DayDTO.toDayResult(): Result<Day> {
    return runCatching {
        val requiredCondition = condition
            ?: throw IllegalArgumentException("condition is a mandatory field for Day and cannot be null.")

        Day(
            maxTempC = maxTempC ?: 0.0,
            maxTempF = maxTempF ?: 0.0,
            minTempC = minTempC ?: 0.0,
            minTempF = minTempF ?: 0.0,
            avgTempC = avgTempC ?: 0.0,
            avgTempF = avgTempF ?: 0.0,
            maxWindMph = maxWindMph ?: 0.0,
            maxWindKph = maxWindKph ?: 0.0,
            totalPrecipitationIn = totalPrecipitationIn ?: 0.0,
            totalSnowCm = totalSnowCm ?: 0.0,
            avgVisKm = avgVisKm ?: 0.0,
            avgVisMiles = avgVisMiles ?: 0.0,
            avgHumidity = avgHumidity ?: 0,
            dailyWillItRain = dailyWillItRain ?: 0,
            dailyChanceOfRain = dailyChanceOfRain ?: 0,
            dailyWillItSnow = dailyWillItSnow ?: 0,
            dailyChanceOfSnow = dailyChanceOfSnow ?: 0,
            condition = requiredCondition.toCondition(),
            uv = uv ?: 0.0
        )
    }
}