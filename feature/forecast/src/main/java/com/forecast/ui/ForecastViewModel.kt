package com.forecast.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.datastore.user.UserPreferenceManager
import com.notification.DEEP_LINK_LOCATION_KEY
import com.notification.NotificationHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userPreferenceManager: UserPreferenceManager,
    private val notificationHandler: NotificationHandler
) : ViewModel() {

    val deepLinkedNewsResource = savedStateHandle.getStateFlow<String?>(
        key = DEEP_LINK_LOCATION_KEY,
        null,
    )

    private val _uiState = MutableStateFlow(ForecastUiState())
    val uiState = _uiState.asStateFlow()
}

data class ForecastUiState(
    val isNotificationEnabled: Boolean = false
)