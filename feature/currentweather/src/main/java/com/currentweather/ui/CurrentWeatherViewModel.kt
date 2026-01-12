package com.currentweather.ui

import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.common.R
import com.common.model.ErrorType
import com.common.model.error.RepositoryError
import com.currentweather.data.model.currentweather.CurrentWeather
import com.currentweather.data.model.forecast.Forecast
import com.currentweather.data.repository.LocationRepository
import com.currentweather.data.repository.SearchLocationRepository
import com.currentweather.data.repository.WeatherRepository
import com.datastore.UserPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CurrentWeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val searchLocationRepository: SearchLocationRepository,
    private val userPreferenceManager: UserPreferenceManager
) : ViewModel() {

    private val _weatherUIState = MutableStateFlow<WeatherUIState>(WeatherUIState.Idle)
    val weatherUIState = _weatherUIState.asStateFlow()

    private val _weatherUIData = MutableStateFlow(getWeatherUI(""))
    val weatherUIData = _weatherUIData.asStateFlow()

    private val _searchLocation = MutableStateFlow("")
    val searchLocation = _searchLocation.asStateFlow()

    private val _searchLocationResults = MutableStateFlow<List<String>>(emptyList())
    val searchLocationResults = _searchLocationResults.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted = _locationPermissionGranted.asStateFlow()

    private val _locationEnabled = MutableStateFlow(false)
    val locationEnabled = _locationEnabled.asStateFlow()

    private val _requestLocationPermissions = MutableStateFlow(false)
    val requestLocationPermissions = _requestLocationPermissions.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(false)
    val hasNotificationPermission = _hasNotificationPermission.asStateFlow()

    private val _shouldLaunchNotificationRequest = MutableStateFlow(false)
    val shouldLaunchNotificationRequest = _shouldLaunchNotificationRequest.asStateFlow()

    fun onNotificationPermissionClicked() {
        _shouldLaunchNotificationRequest.value = true
    }

    fun notificationRequestHandled() {
        _shouldLaunchNotificationRequest.value = false
    }

    fun updateNotificationPermissionStatus(isGranted: Boolean) {
        _hasNotificationPermission.value = isGranted
    }

    fun observeSearchLocation() {
        _searchLocation
            .debounce(300)
            .distinctUntilChanged()
            .onEach { cityName ->
                if (cityName.isNotBlank()) {
                    searchLocationApiCall(cityName)
                } else {
                    _searchLocationResults.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchLocation(cityName: String) {
        _searchLocation.value = cityName
    }

    fun searchLocationApiCall(cityName: String) {
        viewModelScope.launch {
            val result = searchLocationRepository.searchLocation(cityName)
            result.onSuccess { locations ->
                _searchLocationResults.value = locations.map { "${it.name}, ${it.country}" }
            }.onFailure {
                _searchLocationResults.value = emptyList()
            }
        }
    }

    private fun extractCityName(formattedLocationString: String): String {
        return formattedLocationString.split(",").firstOrNull()?.trim() ?: formattedLocationString
    }

    fun handleSearchResultClick(formattedLocationString: String) {
        val cityName = extractCityName(formattedLocationString)
        updateSearchLocation(cityName)
        _searchLocationResults.value = emptyList()
        fetchWeatherOnLocation()
    }

    private fun checkLocationPermission() {
        val hasPermission = locationRepository.hasLocationPermissions()
        _locationPermissionGranted.value = hasPermission
        if (!hasPermission) {
            _requestLocationPermissions.value = true
            _weatherUIState.value =
                WeatherUIState.Error(errorType = ErrorType.LocationPermissionDenied)
        }
    }

    private fun trackLocationEnabledStatus() {
        locationRepository.isLocationEnabled()
            .onEach { isEnabled ->
                _locationEnabled.value = isEnabled
                if (!isEnabled) {
                    _weatherUIState.value =
                        WeatherUIState.Error(errorType = ErrorType.LocationServicesDisabled)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun fetchWeatherOnPermissionChange() {
        combine(locationPermissionGranted, locationEnabled) { permissionGranted, locationEnabled ->
            permissionGranted && locationEnabled
        }.onEach { shouldFetch ->
            if (shouldFetch) {
                fetchWeatherOnLocation()
            }
        }.launchIn(viewModelScope)
    }

    fun startLocationWeatherUpdates() {
        trackLocationEnabledStatus()
        checkLocationPermission()
        fetchWeatherOnPermissionChange()
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchWeatherOnLocation(isRefreshing: Boolean = false) {
        // Only fetch if it's a refresh, a specific location is provided (search),
        // or the current UI state isn't already a successful one to avoid extra api call on navigating back
        if (!isRefreshing && _searchLocation.value.isEmpty() && _weatherUIState.value is WeatherUIState.Success) {
            return
        }

        if (isRefreshing) {
            _isRefreshing.value = true
        } else {
            _weatherUIState.value = WeatherUIState.Loading
        }

        viewModelScope.launch {
            try {
                var location: String? = _searchLocation.value

                if (location?.isEmpty() == true) {
                    try {
                        val firstLocationUpdate = locationRepository.getLocationUpdates().first()
                        userPreferenceManager.saveUserCoordinates(latitude = firstLocationUpdate.latitude, longitude = firstLocationUpdate.longitude)
                        location = "${firstLocationUpdate.latitude},${firstLocationUpdate.longitude}"
                    } catch (e: Exception) {
                        // Current location fetch failed, fall back to the saved location.
                        userPreferenceManager.userCoordinatesFlow.firstOrNull()?.let { savedLocation ->
                            location = "${savedLocation.latitude},${savedLocation.longitude}"
                        }
                        Log.e("TAG", "fetchWeatherOnLocation: $e")
                    }
                }

                location?.let {
                    val (currentWeather, forecast) = coroutineScope {
                        val currentWeatherDeferred =
                            async { weatherRepository.fetchCurrentWeather(it) }
                        val forecastDeferred =
                            async { weatherRepository.fetchForecast(it, 1) }

                        currentWeatherDeferred.await().getOrThrow() to forecastDeferred.await()
                            .getOrThrow()
                    }

                    _weatherUIState.value = WeatherUIState.Success(
                        currentWeather = currentWeather,
                        forecast = forecast
                    )

                    _weatherUIData.value = getWeatherUI(currentWeather.current.condition.text)
                } ?: run {
                    _weatherUIState.value = WeatherUIState.Error(errorType = ErrorType.CityNameError)
                }
            } catch (e: Exception) {
                val errorType = when (e) {
                    is RepositoryError.NetworkError -> ErrorType.NetworkError
                    is RepositoryError.NoDataError -> ErrorType.NoDataError
                    is RepositoryError.MappingError -> ErrorType.MappingError
                    else -> ErrorType.UnknownError
                }
                _weatherUIState.value = WeatherUIState.Error(errorType = errorType)
            } finally {
                if (isRefreshing) {
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun onLocationPermissionsGranted() {
        _locationPermissionGranted.value = true
        _requestLocationPermissions.value = false
    }

    fun permissionRequestHandled() {
        _requestLocationPermissions.value = false
    }

    fun retryFetchWeather(isRefreshing: Boolean = false) {
        fetchWeatherOnLocation(isRefreshing = isRefreshing)
    }

    private fun getWeatherUI(text: String): WeatherUI {
        return when {
            text.lowercase().contains("sun") -> WeatherUI(
                backgroundImageResource = R.drawable.ic_sunny,
                backgroundColorResource = R.color.light_beige
            )

            text.lowercase().contains("cloud") || text.lowercase().contains("wind") -> WeatherUI(
                backgroundImageResource = R.drawable.ic_cloudy,
                backgroundColorResource = R.color.pale_blue
            )

            text.lowercase().contains("rain") || text.lowercase()
                .contains("drizzle") || text.lowercase()
                .contains("snow") -> WeatherUI(
                backgroundImageResource = R.drawable.ic_rainy,
                backgroundColorResource = R.color.dark_shade_blue,
                textColorResource = R.color.white
            )

            else -> WeatherUI(R.drawable.ic_cloudy, R.color.pale_blue)
        }
    }
}

/**
 * Represents the UI elements for displaying weather information.
 *
 * @property backgroundImageResource The resource ID for the background image.
 * @property backgroundColorResource The resource ID for the background color.
 * @property textColorResource The resource ID for the text color.
 */
data class WeatherUI(
    @DrawableRes val backgroundImageResource: Int,
    @ColorRes val backgroundColorResource: Int,
    @ColorRes val textColorResource: Int = R.color.black
)

/**
 * Represents the different states of the weather UI.
 */
sealed interface WeatherUIState {
    /**
     * Represents the idle state of the UI.
     */
    data object Idle : WeatherUIState

    /**
     * Represents the loading state of the UI.
     */
    data object Loading : WeatherUIState

    /**
     * Represents the success state of the UI, containing current weather and forecast data.
     * @param currentWeather The current weather information.
     * @param forecast The forecast information.
     */
    data class Success(
        val currentWeather: CurrentWeather?,
        val forecast: Forecast?
    ) : WeatherUIState

    /**
     * Represents the error state of the UI.
     * @param errorType The type of error that occurred.
     */
    data class Error(val errorType: ErrorType) : WeatherUIState
}