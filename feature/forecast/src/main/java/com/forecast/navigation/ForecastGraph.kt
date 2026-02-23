package com.forecast.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType.Companion.StringType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.forecast.ui.ForecastScreen
import com.notification.DEEP_LINK_URI_PATTERN

/**
 * The root route for the entire Forecast feature graph.
 * This is what the app module will use to navigate *into* the Forecast feature.
 */
const val FORECAST_GRAPH_ROUTE = "forecast_graph"

/**
 * Extension function on NavGraphBuilder to add the Forecast feature's navigation graph.
 * This is called from the main App NavHost.
 *
 * @param navController The NavController from the App module.
 * @param onNavigateOut The callback for actions that require navigation outside the Forecast feature.
 */
fun NavGraphBuilder.addForecastGraph(
    navController: NavController,
    onNavigateOut: (ForecastNavCallback) -> Unit
) {
    navigation(
        startDestination = ForecastRoutes.ItemForecast.route,
        route = FORECAST_GRAPH_ROUTE
    ) {
        composable(
            route = ForecastRoutes.ItemForecast.route,
            arguments = listOf(
                navArgument("locationName") { type = StringType }
            ),
            deepLinks = listOf(
                navDeepLink {
                    /**
                     * This destination has a deep link that enables a specific weather forecast to be
                     * opened from a notification (@see DefaultNotificationHandler for more). The location
                     * is sent in the URI rather than being modeled in the route type because it's
                     * transient data (stored in SavedStateHandle) that is cleared after the user has
                     * opened the weather forecast.
                     */
                    uriPattern = DEEP_LINK_URI_PATTERN
                },
            )
        ) { backStackEntry ->
            val locationName = backStackEntry.arguments?.getString("locationName")
            ForecastScreen(
                locationName = locationName ?: "N/A",
                onGoBackToForecast = {
                    // This is an external navigation request.
                    // The Forecast module requests the App module to handle navigation back to Current weather.
                    onNavigateOut(ForecastNavCallback.NavigateBackToForecastHome)
                }
            )
        }
    }
}