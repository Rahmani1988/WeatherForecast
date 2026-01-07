package com.currentweather.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.common.model.ErrorType
import com.common.model.extension.toFormattedTime
import com.currentweather.R
import com.currentweather.data.model.currentweather.CurrentWeather
import com.currentweather.data.model.forecast.Forecast
import com.currentweather.ui.component.ConnectedWavyLines
import com.currentweather.ui.component.CustomSearchBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CurrentWeatherScreen(
    onNavigateToDetail: (cityName: String) -> Unit,
    viewModel: CurrentWeatherViewModel
) {
    val weatherUIState by viewModel.weatherUIState.collectAsStateWithLifecycle()
    val weatherUIData by viewModel.weatherUIData.collectAsStateWithLifecycle()
    val searchLocation by viewModel.searchLocation.collectAsStateWithLifecycle()
    val searchLocationResults by viewModel.searchLocationResults.collectAsStateWithLifecycle()
    val locationPermissionGranted by viewModel.locationPermissionGranted.collectAsStateWithLifecycle()
    val locationEnabled by viewModel.locationEnabled.collectAsStateWithLifecycle()
    val requestLocationPermissions by viewModel.requestLocationPermissions.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        "" // a place holder
    }

    val notificationPermissionState = rememberPermissionState(
        permission = notificationPermission
    )

    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notificationPermissionState.status.isGranted
    } else {
        true // Permissions are granted by default on older OS versions
    }

    // todo check if this logic can be in vm
    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                notificationPermissionState.status.isGranted -> { /* already granted */ }
                notificationPermissionState.status.shouldShowRationale -> {
                    notificationPermissionState.launchPermissionRequest()
                }
                else -> {
                    notificationPermissionState.launchPermissionRequest()
                }
            }
        }
    }

    val appBarColor = colorResource(id = weatherUIData.backgroundColorResource)

    LaunchedEffect(Unit) {
        viewModel.startLocationWeatherUpdates()
    }

    LaunchedEffect(requestLocationPermissions) {
        if (requestLocationPermissions) {
            locationPermissionsState.launchMultiplePermissionRequest()
            viewModel.permissionRequestHandled()
        }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.onLocationPermissionsGranted()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.observeSearchLocation()
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(locationPermissionGranted && locationEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(appBarColor)
                ) {
                    CustomSearchBar(
                        query = searchLocation,
                        onQueryChange = viewModel::updateSearchLocation,
                        onSearch = viewModel::searchLocationApiCall,
                        searchResults = searchLocationResults,
                        onResultClick = {
                            viewModel.handleSearchResultClick(it)
                        }
                    )
                }
            }
        },
        snackbarHost = {
            // todo to be completed
        }
    ) { innerPadding ->
        PullToRefreshBox(
            contentAlignment = Alignment.Center,
            isRefreshing = isRefreshing,
            onRefresh = {
                if (locationPermissionGranted && locationEnabled) {
                    viewModel.retryFetchWeather(isRefreshing = true)
                }
            },
            modifier = Modifier
                .padding(innerPadding)
        ) {
            when (val result = weatherUIState) {
                is WeatherUIState.Error -> {
                    when (result.errorType) {
                        ErrorType.LocationServicesDisabled -> {
                            ErrorItem(
                                action = {
                                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    context.startActivity(intent)
                                },
                                buttonContent = {
                                    Text(
                                        textAlign = TextAlign.Center,
                                        text = stringResource(R.string.enable_location_services)
                                    )
                                }
                            )
                        }

                        ErrorType.LocationPermissionDenied -> {
                            val showRationale =
                                locationPermissionsState.permissions.any { it.status.shouldShowRationale }
                            val permanentlyDenied =
                                !locationPermissionsState.allPermissionsGranted &&
                                        !locationPermissionsState.permissions.any { it.status.shouldShowRationale }

                            when {
                                permanentlyDenied -> {
                                    ErrorItem(
                                        action = {
                                            val intent =
                                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts(
                                                        "package",
                                                        context.packageName,
                                                        null
                                                    )
                                                }
                                            context.startActivity(intent)
                                        },
                                        content = {
                                            Text(
                                                textAlign = TextAlign.Center,
                                                text = stringResource(R.string.location_permission_permanently_denied_please_enable_in_settings)
                                            )
                                        },
                                        buttonContent = {
                                            Text(
                                                textAlign = TextAlign.Center,
                                                text = stringResource(R.string.open_settings)
                                            )
                                        }
                                    )
                                }

                                showRationale -> {
                                    ErrorItem(
                                        action = {
                                            locationPermissionsState.launchMultiplePermissionRequest()
                                        },
                                        content = {
                                            Text(
                                                textAlign = TextAlign.Center,
                                                text = stringResource(R.string.location_permission_is_important_for_this_app_please_grant_it)
                                            )
                                        },
                                        buttonContent = {
                                            Text(
                                                textAlign = TextAlign.Center,
                                                text = stringResource(R.string.request_permission_again)
                                            )
                                        }
                                    )
                                }

                                else -> {
                                    ErrorItem(
                                        action = {
                                            locationPermissionsState.launchMultiplePermissionRequest()
                                        },
                                        buttonContent = {
                                            Text(
                                                textAlign = TextAlign.Center,
                                                text = stringResource(R.string.grant_location_permissions)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        ErrorType.UnknownError, ErrorType.MappingError, ErrorType.NetworkError, ErrorType.NoDataError, ErrorType.CityNameError -> {
                            ErrorItem(
                                action = {
                                    viewModel.retryFetchWeather()
                                },
                                buttonContent = {
                                    Text(
                                        textAlign = TextAlign.Center,
                                        text = stringResource(R.string.retry)
                                    )
                                }
                            )
                        }
                    }
                }

                WeatherUIState.Idle -> Box(modifier = Modifier.fillMaxSize())
                WeatherUIState.Loading -> {
                    LoadingContent(
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }

                is WeatherUIState.Success ->
                    SuccessContent(
                        modifier = Modifier.fillMaxSize(),
                        weatherData = weatherUIData,
                        forecast = result.forecast,
                        currentWeather = result.currentWeather,
                        onNavigateToDetail = onNavigateToDetail,
                        requestNotificationPermission = requestNotificationPermission,
                        hasNotificationPermission = hasNotificationPermission
                    )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = colorResource(com.common.R.color.pale_blue))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(20.dp)
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .size(150.dp, 200.dp)
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.height(60.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(30.dp)
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                repeat(6) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(12.dp)
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    currentWeather: CurrentWeather?,
    forecast: Forecast?,
    weatherData: WeatherUI,
    requestNotificationPermission: () -> Unit,
    onNavigateToDetail: (cityName: String) -> Unit,
    hasNotificationPermission: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = colorResource(weatherData.textColorResource)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(weatherData.backgroundColorResource))
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            painter = painterResource(weatherData.backgroundImageResource),
            contentDescription = null, // decorative image
        )

        currentWeather?.let { weather ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedVisibility(!hasNotificationPermission) {
                    Text(
                        modifier = Modifier
                            .padding(32.dp)
                            .clickable { requestNotificationPermission() },
                        text = stringResource(R.string.weather_updates),
                        style = MaterialTheme.typography.titleMedium,
                        color = color
                    )
                }

                Spacer(modifier = Modifier.height(56.dp))

                Text(
                    text = stringResource(R.string.today),
                    style = MaterialTheme.typography.titleLarge,
                    color = color
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${weather.current.tempC.roundToInt()}°",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 100.sp),
                    color = color
                )

                Spacer(modifier = Modifier.height(100.dp))

                weather.location.name.let { cityName ->
                    Icon(
                        painter = painterResource(com.common.R.drawable.ic_loaction),
                        contentDescription = null,
                        tint = color
                    )

                    Text(
                        text = cityName,
                        style = MaterialTheme.typography.displayLarge,
                        color = color
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                ConnectedWavyLines(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = color
                )

                forecast?.forecast?.forecastDay?.first()?.hour?.let { hours ->
                    val listState = rememberLazyListState()
                    LazyRow(state = listState) {
                        items(hours, key = { it.time.hashCode() }) {
                            ItemHourlyForecast(
                                iconUrl = it.condition.icon,
                                temperature = it.tempC.toString(),
                                time = it.time,
                                color = color
                            )
                        }
                    }

                    LaunchedEffect(key1 = hours) {
                        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val targetIndex = hours.indexOfFirst {
                            it.time.substringAfter(" ").substringBefore(":").toInt() == currentHour
                        }
                        if (targetIndex != -1) {
                            listState.scrollToItem(targetIndex) // scroll to current time
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // todo navigate to detail
                /*weather.location.name.let { cityName ->
                    Button(
                        onClick = { onNavigateToDetail(cityName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.view_detail, cityName)
                        )
                    }
                }*/
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.no_weather_data_available)
                )
            }
        }
    }
}

@Composable
private fun ItemHourlyForecast(
    iconUrl: String,
    temperature: String,
    time: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = time.toFormattedTime(),
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
        AsyncImage(
            model = "https:$iconUrl",
            contentDescription = stringResource(R.string.weather_condition_icon),
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = "$temperature°",
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
    }
}

@Composable
private fun ErrorItem(
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit) = {},
    action: () -> Unit,
    buttonContent: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        content()
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = action
        ) {
            buttonContent()
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmerAnimation"
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(x = translateAnimation.value, y = 0f),
            end = Offset(x = translateAnimation.value + 500f, y = 500f)
        )
    )
}