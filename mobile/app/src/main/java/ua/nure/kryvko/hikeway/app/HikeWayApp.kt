package ua.nure.kryvko.hikeway.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ua.nure.kryvko.hikeway.R
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
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.ROUTES) }
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
                currentDestination = AppDestination.ROUTES
                routeSearchViewModel.refreshCurrentSearch()
                routeSearchViewModel.refreshPointsOfInterest()
            },
        )
        return
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestination.entries.forEach { destination ->
                val selected = destination == currentDestination
                item(
                    icon = {
                        Icon(
                            painter = painterResource(destination.iconRes(selected)),
                            contentDescription = destination.label,
                            modifier = Modifier.size(40.dp),
                            tint = Color.Unspecified,
                        )
                    },
                    label = { Text(destination.label) },
                    selected = selected,
                    onClick = { currentDestination = destination },
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestination.ROUTES -> RouteSearchScreen(
                viewModel = routeSearchViewModel,
                onCreateRoute = { isCreatingRoute = true },
                isAdmin = authState.isAdmin,
            )
            AppDestination.COMPLETED_HIKES -> CompletedHikesScreen(completedHikesViewModel)
            AppDestination.PROFILE -> ProfileScreen(onLogOut = authViewModel::logOut)
            AppDestination.MAP -> PlaceholderScreen(currentDestination.label)
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
    @param:DrawableRes val activeIconRes: Int,
    @param:DrawableRes val inactiveIconRes: Int,
) {
    ROUTES("Routes", R.drawable.ic_nav_routes, R.drawable.ic_nav_routes_inactive),
    MAP("Map", R.drawable.ic_nav_map, R.drawable.ic_nav_map_inactive),
    COMPLETED_HIKES("Saved", R.drawable.ic_nav_saved, R.drawable.ic_nav_saved_inactive),
    PROFILE("Profile", R.drawable.ic_nav_profile, R.drawable.ic_nav_profile_inactive),
    ;

    @DrawableRes
    fun iconRes(selected: Boolean): Int = if (selected) activeIconRes else inactiveIconRes
}
