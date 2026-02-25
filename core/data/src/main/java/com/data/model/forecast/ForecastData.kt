package com.data.model.forecast

import com.network.models.reponse.forecast.ForecastDataDTO

data class ForecastData(
    val forecastDay: List<ForecastDay>
)

fun ForecastDataDTO.toForecastData(): ForecastData {
    return ForecastData(
        forecastDay = forecastDay.orEmpty()
            .mapNotNull { forecastDayDTO -> forecastDayDTO?.toForecastDayResult()?.getOrThrow() }
    )
}