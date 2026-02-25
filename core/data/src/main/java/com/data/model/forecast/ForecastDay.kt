package com.data.model.forecast

import com.network.models.reponse.forecast.ForecastDayDTO

data class ForecastDay(
    val date: String,
    val dateEpoch: Long,
    val day: Day,
    val astro: Astro,
    val hour: List<Hour>,
)

fun ForecastDayDTO.toForecastDayResult(): Result<ForecastDay> {
    return runCatching {
        val requiredDay = day
            ?: throw IllegalArgumentException("day is a mandatory field for ForecastDay and cannot be null.")

        val requiredAstro = astro
            ?: throw IllegalArgumentException("astro is a mandatory field for ForecastDay and cannot be null.")

        ForecastDay(
            date = date.orEmpty(),
            dateEpoch = dateEpoch ?: 0L,
            day = requiredDay.toDayResult().getOrThrow(),
            astro = requiredAstro.toAstro(),
            hour = hour.orEmpty().mapNotNull { hourDTO -> hourDTO?.toHourResult()?.getOrThrow() }
        )
    }
}