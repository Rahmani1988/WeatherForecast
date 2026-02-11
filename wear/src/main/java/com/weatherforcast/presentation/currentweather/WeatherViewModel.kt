package com.weatherforcast.presentation.currentweather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.common.model.models.WeatherSyncModel
import com.datastore.weather.WeatherLocalDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherLocalDataSource: WeatherLocalDataSource
) : ViewModel() {

    val uiState: StateFlow<WeatherUiState> = weatherLocalDataSource.weatherFlow
        .map { model ->
            if (model.city.isEmpty()) {
                WeatherUiState.Empty
            } else {
                WeatherUiState.Success(model)
            }
        }
        .catch { emit(WeatherUiState.Error(it.message ?: "Unknown Error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WeatherUiState.Loading
        )
}

/**
 * Represents the state of the weather data.
 */
sealed interface WeatherUiState {
    /**
     * The weather data is loading.
     */
    object Loading : WeatherUiState

    /**
     * The weather data is empty.
     */
    data object Empty : WeatherUiState

    /**
     * The weather data is loaded.
     */
    data class Success(val data: WeatherSyncModel) : WeatherUiState

    /**
     * The weather data failed to load.
     */
    data class Error(val message: String) : WeatherUiState
}