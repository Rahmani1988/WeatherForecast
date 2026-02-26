package com.data.repository

import com.common.model.error.RepositoryError
import com.common.util.CachedEntry
import com.data.model.current.CurrentWeather
import com.data.model.current.toCurrentWeatherResult
import com.data.model.forecast.Forecast
import com.data.model.forecast.toForecastResult
import com.network.ApiService
import com.reza.threading.common.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultWeatherRepository @Inject constructor(
    private val apiService: ApiService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WeatherRepository {

    companion object {
        private const val CACHE_TIMEOUT = 5 * 60 * 1000L // 5 minutes
    }

    // Cache and Mutex for Current Weather
    private val currentMutex = Mutex()
    private val currentWeatherCache = mutableMapOf<String, CachedEntry<CurrentWeather>>()

    // Cache and Mutex for Forecast (Unique to this resource)
    private val forecastMutex = Mutex()
    private val forecastCache = mutableMapOf<String, CachedEntry<Forecast>>()

    override suspend fun fetchCurrentWeather(location: String): Result<CurrentWeather> =
        withContext(ioDispatcher) {

            val cached = currentWeatherCache[location]
            if (cached != null && !cached.isExpired(CACHE_TIMEOUT)) {
                return@withContext Result.success(cached.data)
            }

            // Lock to ensure only one network request per location happens
            currentMutex.withLock {
                // Re-check cache inside lock (another thread might have finished the fetch)
                currentWeatherCache[location]?.let {
                    if (!it.isExpired(CACHE_TIMEOUT)) return@withLock Result.success(it.data)
                }

                try {
                    apiService.fetchCurrentWeather(location).let { response ->
                        if (response.isSuccessful) {
                            response.body()?.let { body ->
                                body.toCurrentWeatherResult().fold(
                                    onSuccess = { currentWeather ->
                                        currentWeatherCache[location] = CachedEntry(currentWeather)
                                        Result.success(currentWeather)
                                    },
                                    onFailure = { throwable ->
                                        Result.failure(
                                            RepositoryError.MappingError(
                                                throwable
                                            )
                                        )
                                    }
                                )
                            }
                                ?: Result.failure(RepositoryError.NoDataError("Response body is null"))
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

    override suspend fun fetchForecast(
        location: String,
        days: Int
    ): Result<Forecast> =
        withContext(ioDispatcher) {
            val cacheKey = "$location-$days"

            forecastCache[cacheKey]?.let {
                if (!it.isExpired(CACHE_TIMEOUT)) return@withContext Result.success(it.data)
            }

            // Synchronization: Only one forecast fetch at a time
            forecastMutex.withLock {
                // Double-check after acquiring lock
                forecastCache[cacheKey]?.let {
                    if (!it.isExpired(CACHE_TIMEOUT)) return@withLock Result.success(it.data)
                }

                try {
                    apiService.fetchForecast(location, days).let { response ->
                        if (response.isSuccessful) {
                            response.body()?.let { body ->
                                val conversionResult: Result<Forecast> =
                                    body.toForecastResult().fold(
                                        onSuccess = { forecast ->
                                            forecastCache[cacheKey] = CachedEntry(forecast)
                                            Result.success(forecast)
                                        },
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
}