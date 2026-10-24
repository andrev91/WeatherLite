package com.tehuberz.weather.lite.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.tehuberz.weather.lite.network.NetworkModule
import com.tehuberz.weather.lite.ui.screen.MissingApiKeyScreen
import com.tehuberz.weather.lite.ui.screen.SettingsScreen
import com.tehuberz.weather.lite.ui.screen.WeatherScreen
import com.tehuberz.weather.lite.ui.theme.AdventureTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
object WeatherRoute : NavKey

@Serializable
object SettingsRoute : NavKey

@Serializable
object MissingApiKeyRoute : NavKey

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startRoute: NavKey =
            if (NetworkModule.isApiKeyConfigured()) {
                WeatherRoute
            } else {
                MissingApiKeyRoute
            }

        setContent {
            AdventureTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val backStack = rememberNavBackStack(startRoute)
                    val customPopTransition: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith slideOutHorizontally(targetOffsetX = { it })
                    }
                    NavDisplay(
                        backStack = backStack,
                        onBack = {
                            backStack.removeLastOrNull()
                        },
                        transitionSpec = {
                            slideInHorizontally(initialOffsetX = { it }) togetherWith slideOutHorizontally(targetOffsetX = { -it })
                        },
                        popTransitionSpec = customPopTransition,
                        predictivePopTransitionSpec = { _ -> customPopTransition() },
                        entryProvider =
                            entryProvider {
                                entry<MissingApiKeyRoute> {
                                    MissingApiKeyScreen()
                                }
                                entry<WeatherRoute> {
                                    WeatherScreen(
                                        onSettingsClick = { backStack.add(SettingsRoute) },
                                    )
                                }
                                entry<SettingsRoute> {
                                    SettingsScreen(
                                        onNavigateBack = { backStack.removeLastOrNull() },
                                    )
                                }
                            },
                    )
                }
            }
        }
    }
}
