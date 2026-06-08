package ua.nure.kryvko.hikeway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
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
import ua.nure.kryvko.hikeway.feature.routesearch.RouteSearchScreen
import ua.nure.kryvko.hikeway.feature.routesearch.RouteSearchViewModel
import ua.nure.kryvko.hikeway.ui.theme.HikeWayTheme

class MainActivity : ComponentActivity() {
    private val container = AppContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val routeSearchViewModel = ViewModelProvider(
            this,
            RouteSearchViewModel.factory(
                searchRoutes = container.searchRoutes,
                routeTrackingProvider = container.routeTrackingProvider,
            ),
        )[RouteSearchViewModel::class.java]

        setContent {
            HikeWayTheme {
                HikeWayApp(routeSearchViewModel)
            }
        }
    }
}

@Composable
fun HikeWayApp(routeSearchViewModel: RouteSearchViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }

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
            AppDestination.HOME -> RouteSearchScreen(routeSearchViewModel)
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
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}
