package ua.nure.kryvko.hikeway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider
import ua.nure.kryvko.hikeway.app.AppContainer
import ua.nure.kryvko.hikeway.feature.completedhikes.CompletedHikesScreen
import ua.nure.kryvko.hikeway.feature.completedhikes.CompletedHikesViewModel
import ua.nure.kryvko.hikeway.feature.routecreation.RouteCreationScreen
import ua.nure.kryvko.hikeway.feature.routecreation.RouteCreationViewModel
import ua.nure.kryvko.hikeway.feature.routesearch.RouteSearchScreen
import ua.nure.kryvko.hikeway.feature.routesearch.RouteSearchViewModel
import ua.nure.kryvko.hikeway.ui.theme.HikeWayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = AppContainer(applicationContext)
        val routeSearchViewModel = ViewModelProvider(
            this,
            RouteSearchViewModel.factory(
                searchRoutes = container.searchRoutes,
                routeTrackingProvider = container.routeTrackingProvider,
                saveCompletedHike = container.saveCompletedHike,
                timeProvider = container.timeProvider,
                activeTimer = container.activeTimer,
            ),
        )[RouteSearchViewModel::class.java]
        val completedHikesViewModel = ViewModelProvider(
            this,
            CompletedHikesViewModel.factory(
                observeCompletedHikes = container.observeCompletedHikes,
            ),
        )[CompletedHikesViewModel::class.java]
        val routeCreationViewModel = ViewModelProvider(
            this,
            RouteCreationViewModel.factory(
                saveCustomRoute = container.saveCustomRoute,
            ),
        )[RouteCreationViewModel::class.java]

        setContent {
            HikeWayTheme {
                HikeWayApp(
                    routeSearchViewModel = routeSearchViewModel,
                    completedHikesViewModel = completedHikesViewModel,
                    routeCreationViewModel = routeCreationViewModel,
                )
            }
        }
    }
}

@Composable
fun HikeWayApp(
    routeSearchViewModel: RouteSearchViewModel,
    completedHikesViewModel: CompletedHikesViewModel,
    routeCreationViewModel: RouteCreationViewModel,
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var isCreatingRoute by rememberSaveable { mutableStateOf(false) }

    if (isCreatingRoute) {
        RouteCreationScreen(
            viewModel = routeCreationViewModel,
            onCancel = {
                routeCreationViewModel.reset()
                isCreatingRoute = false
            },
            onSaved = {
                isCreatingRoute = false
                currentDestination = AppDestination.HOME
                routeSearchViewModel.refreshCurrentSearch()
            },
        )
        return
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestination.entries.forEach { destination ->
                item(
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination },
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestination.HOME -> RouteSearchScreen(
                viewModel = routeSearchViewModel,
                onCreateRoute = { isCreatingRoute = true },
            )
            AppDestination.COMPLETED_HIKES -> CompletedHikesScreen(completedHikesViewModel)
            else -> PlaceholderScreen(currentDestination.label)
        }
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$label is not implemented yet.")
    }
}

enum class AppDestination(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    COMPLETED_HIKES("Completed hikes", Icons.AutoMirrored.Filled.List),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}
