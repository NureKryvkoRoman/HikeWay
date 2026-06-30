package ua.nure.kryvko.hikeway.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ua.nure.kryvko.hikeway.feature.auth.AuthScreen
import ua.nure.kryvko.hikeway.feature.auth.AuthStatus
import ua.nure.kryvko.hikeway.feature.auth.AuthViewModel
import ua.nure.kryvko.hikeway.feature.completedhikes.CompletedHikesScreen
import ua.nure.kryvko.hikeway.feature.completedhikes.CompletedHikesViewModel
import ua.nure.kryvko.hikeway.feature.routecreation.RouteCreationScreen
import ua.nure.kryvko.hikeway.feature.routecreation.RouteCreationViewModel
import ua.nure.kryvko.hikeway.feature.routesearch.RouteSearchScreen
import ua.nure.kryvko.hikeway.feature.routesearch.RouteSearchViewModel

@Composable
fun HikeWayApp(
    authViewModel: AuthViewModel,
    routeSearchViewModel: RouteSearchViewModel,
    completedHikesViewModel: CompletedHikesViewModel,
    routeCreationViewModel: RouteCreationViewModel,
) {
    val authState by authViewModel.uiState.collectAsState()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var isCreatingRoute by rememberSaveable { mutableStateOf(false) }

    when (authState.status) {
        AuthStatus.CHECKING -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        AuthStatus.UNAUTHENTICATED -> {
            AuthScreen(authViewModel)
            return
        }
        AuthStatus.AUTHENTICATED -> Unit
    }

    LaunchedEffect(authState.username) {
        routeSearchViewModel.refreshCurrentSearch()
        routeSearchViewModel.refreshPointsOfInterest()
    }

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
                routeSearchViewModel.refreshPointsOfInterest()
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
                isAdmin = authState.isAdmin,
            )
            AppDestination.COMPLETED_HIKES -> CompletedHikesScreen(completedHikesViewModel)
            AppDestination.PROFILE -> ProfileScreen(onLogOut = authViewModel::logOut)
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

@Composable
private fun ProfileScreen(onLogOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Profile")
        Button(onClick = onLogOut, modifier = Modifier.padding(top = 16.dp)) {
            Text("Log out")
        }
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
