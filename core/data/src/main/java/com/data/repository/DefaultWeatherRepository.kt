package com.data.repository

import com.common.model.error.RepositoryError
import com.data.model.current.CurrentWeather
import com.data.model.current.toCurrentWeatherResult
import com.data.model.forecast.Forecast
import com.data.model.forecast.toForecastResult
import com.network.ApiService
import com.reza.threading.common.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultWeatherRepository @Inject constructor(
    private val apiService: ApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WeatherRepository {

    override suspend fun fetchCurrentWeather(location: String): Result<CurrentWeather> =
        withContext(ioDispatcher) {
            try {
                apiService.fetchCurrentWeather(location).let { response ->
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            body.toCurrentWeatherResult().fold(
                                onSuccess = { currentWeather -> Result.success(currentWeather) },
                                onFailure = { throwable ->
                                    Result.failure(
                                        RepositoryError.MappingError(
                                            throwable
                                        )
                                    )
                                }
                            )
                        } ?: Result.failure(RepositoryError.NoDataError("Response body is null"))
                    } else {
                        Result.failure(
                            RepositoryError.NetworkError(
                                response.code(),
                                response.errorBody()?.string()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(RepositoryError.UnknownError(e))
            }
        }

    override suspend fun fetchForecast(
        location: String,
        days: Int
    ): Result<Forecast> =
        withContext(ioDispatcher) {
            try {
                apiService.fetchForecast(location, days).let { response ->
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            val conversionResult: Result<Forecast> = body.toForecastResult().fold(
                                onSuccess = { forecast -> Result.success(forecast) },
                                onFailure = { throwable ->
                                    Result.failure(
                                        RepositoryError.MappingError(
                                            throwable
                                        )
                                    )
                                }
                            )
                            conversionResult
                        } ?: Result.failure(RepositoryError.NoDataError("Empty response body"))
                    } else {
                        Result.failure(
                            RepositoryError.NetworkError(
                                response.code(),
                                response.errorBody()?.string()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(RepositoryError.UnknownError(e))
            }
        }
}