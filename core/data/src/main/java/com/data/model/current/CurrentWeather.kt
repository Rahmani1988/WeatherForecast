package com.data.model.current

import com.common.model.models.Current
import com.common.model.models.Location
import com.common.model.models.Condition
import com.network.models.reponse.currentweather.ConditionDTO
import com.network.models.reponse.currentweather.CurrentDTO
import com.network.models.reponse.currentweather.LocationDTO
import com.network.models.reponse.currentweather.CurrentWeatherDTO

data class CurrentWeather(
    val current: Current,
    val location: Location
)

fun CurrentWeatherDTO.toCurrentWeatherResult(): Result<CurrentWeather> {
    return runCatching {
        val currentDtoNonNull = currentDTO
            ?: throw IllegalArgumentException("CurrentDTO is missing for CurrentWeather and cannot be null.")
        val locationDtoNonNull = locationDTO
            ?: throw IllegalArgumentException("LocationDTO is missing for CurrentWeather and cannot be null.")

        val current = currentDtoNonNull.toCurrentResult().getOrThrow()
        val location = locationDtoNonNull.toLocationResult().getOrThrow()

        CurrentWeather(
            current = current,
            location = location
        )
    }
}

fun CurrentDTO.toCurrentResult(): Result<Current> {
    return runCatching {
        val requiredTempC = tempC
            ?: throw IllegalArgumentException("Temperature (C) is a mandatory field for Current and cannot be null.")
        val requiredTempF = tempF
            ?: throw IllegalArgumentException("Temperature (F) is a mandatory field for Current and cannot be null.")
        val requiredLastUpdated = lastUpdated
            ?: throw IllegalArgumentException("Last updated time is a mandatory field for Current and cannot be null.")
        val requiredConditionDTO = conditionDTO
            ?: throw IllegalArgumentException("Condition data is mandatory for Current and cannot be null.")

        Current(
            cloud = cloud ?: 0,
            condition = requiredConditionDTO.toCondition(),
            feelsLikeC = feelsLikeC ?: 0.0,
            feelsLikeF = feelsLikeF ?: 0.0,
            humidity = humidity ?: 0,
            isDay = isDay ?: 0,
            lastUpdated = requiredLastUpdated,
            precipIn = precipIn ?: 0.0,
            precipMm = precipMm ?: 0.0,
            pressureIn = pressureIn ?: 0.0,
            pressureMb = pressureMb ?: 0.0,
            tempC = requiredTempC,
            tempF = requiredTempF,
            uv = uv ?: 0.0,
            windDegree = windDegree ?: 0,
            windDir = windDir.orEmpty(),
            windKph = windKph ?: 0.0,
            windMph = windMph ?: 0.0,
            windchillC = windchillC ?: 0.0,
            windchillF = windchillF ?: 0.0
        )
    }
}

fun LocationDTO.toLocationResult(): Result<Location> {
    return runCatching {
        Location(
            country = country
                ?: throw IllegalStateException("Country is a mandatory field and cannot be null"),
            lat = lat
                ?: throw IllegalStateException("Latitude is a mandatory field and cannot be null"),
            localtime = localtime.orEmpty(),
            long = lon
                ?: throw IllegalStateException("Longitude is a mandatory field and cannot be null"),
            name = name.orEmpty(),
            region = region.orEmpty(),
        )
    }
}

fun ConditionDTO.toCondition(): Condition {
    return Condition(
        code = code ?: 0,
        icon = icon.orEmpty(),
        text = text.orEmpty()
    )
}