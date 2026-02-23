package com.network

import ForecastDTO
import com.network.models.reponse.currentweather.CurrentWeatherDTO
import com.network.models.reponse.search.SearchDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("current.json")
    suspend fun fetchCurrentWeather(@Query("q") location: String): Response<CurrentWeatherDTO>


    @GET("forecast.json")
    suspend fun fetchForecast(
        @Query("q") location: String,
        @Query("days") days: Int = 3,
        @Query("aqi") aqi: Boolean = false,
        @Query("alerts") alerts: Boolean = false
    ): Response<ForecastDTO>

    @GET("search.json")
    suspend fun searchLocation(@Query("q") cityName: String): Response<List<SearchDTO>>
}