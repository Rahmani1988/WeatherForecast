package com.reza.weatherforcast.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.currentweather.navigation.CURRENT_WEATHER_GRAPH_ROUTE
import com.currentweather.navigation.CurrentWeatherNavCallback
import com.currentweather.navigation.addCurrentWeatherGraph
import com.forecast.navigation.ForecastNavCallback
import com.forecast.navigation.ForecastRoutes
import com.forecast.navigation.addForecastGraph

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = CURRENT_WEATHER_GRAPH_ROUTE
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        addCurrentWeatherGraph(
            navController = navController,
            onNavigateOut = { callback ->
                when (callback) {
                    is CurrentWeatherNavCallback.NavigateToDetail -> {
                        navController.navigate(ForecastRoutes.ItemForecast.createRoute(callback.locationName))
                    }
                }
            }
        )

        addForecastGraph(
            navController = navController,
            onNavigateOut = { callback ->
                when (callback) {
                    is ForecastNavCallback.NavigateBackToForecastHome -> {
                        if (!navController.popBackStack()) {
                            // Fallback: If for some reason there's no backstack, then navigate
                            navController.navigate(CURRENT_WEATHER_GRAPH_ROUTE) {
                                popUpTo(CURRENT_WEATHER_GRAPH_ROUTE) { inclusive = true }
                            }
                        }
                    }
                }
            }
        )

        // other top-level app screens or other feature graphs
    }
}

/**
 * Extension functions on NavController for navigating to the root of feature graphs.
 * These are defined in the app module because the app module knows about all top-level
 * feature graphs and their root routes.
 */
fun NavController.navigateToCurrentWeatherGraph() {
    navigate(CURRENT_WEATHER_GRAPH_ROUTE) {
        // Common navigation options for top-level tabs/features
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateToDetailGraph(locationName: String) {
    navigate(ForecastRoutes.ItemForecast.createRoute(locationName)) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}