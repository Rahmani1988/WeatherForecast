package com.reza.weatherforcast.presentation.currentweather

import android.text.format.DateUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.common.model.models.WeatherSyncModel
import com.datastore.weather.WeatherLocalDataSource
import com.reza.weatherforcast.data.model.WeatherModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    weatherLocalDataSource: WeatherLocalDataSource
) : ViewModel() {

    private val minuteTicker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    val uiState: StateFlow<WeatherUiState> = weatherLocalDataSource.weatherFlow
        .combine(minuteTicker) { model, now ->
            if (model.city.isEmpty()) {
                WeatherUiState.Empty
            } else {
                mapToSuccessState(model, now)
            }
        }
        .catch { emit(WeatherUiState.Error(it.message ?: "Unknown Error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WeatherUiState.Loading
        )

    private fun mapToSuccessState(model: WeatherSyncModel, currentTime: Long): WeatherUiState.Success {
        val timeAgo = DateUtils.getRelativeTimeSpanString(
            model.timestamp,
            currentTime,
            DateUtils.MINUTE_IN_MILLIS
        ).toString()

        return WeatherUiState.Success(
            WeatherModel(
                city = model.city,
                summary = model.summary,
                timeAgo = timeAgo
            )
        )
    }
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
    data class Success(val data: WeatherModel) : WeatherUiState

    /**
     * The weather data failed to load.
     */
    data class Error(val message: String) : WeatherUiState
}