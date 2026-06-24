package ua.nure.kryvko.hikeway

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import androidx.lifecycle.ViewModelProvider
import ua.nure.kryvko.hikeway.app.AppContainer
import ua.nure.kryvko.hikeway.feature.auth.AuthScreen
import ua.nure.kryvko.hikeway.feature.auth.AuthStatus
import ua.nure.kryvko.hikeway.feature.auth.AuthViewModel
import ua.nure.kryvko.hikeway.feature.completedhikes.CompletedHikesScreen
import ua.nure.kryvko.hikeway.feature.completedhikes.CompletedHikesViewModel
import ua.nure.kryvko.hikeway.feature.routecreation.RouteCreationScreen
import ua.nure.kryvko.hikeway.feature.routecreation.RouteCreationViewModel
import ua.nure.kryvko.hikeway.feature.routesearch.RouteSearchScreen
import ua.nure.kryvko.hikeway.feature.routesearch.RouteSearchViewModel
import ua.nure.kryvko.hikeway.ui.theme.HikeWayTheme

class MainActivity : ComponentActivity() {
    private lateinit var routeSearchViewModel: RouteSearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestLocationPermissionForProduction()
        val container = AppContainer(applicationContext)
        val authViewModel = ViewModelProvider(
            this,
            AuthViewModel.factory(
                login = container.login,
                signUp = container.signUp,
                restoreSession = container.restoreSession,
                logout = container.logout,
                observeAuthSession = container.observeAuthSession,
            ),
        )[AuthViewModel::class.java]
        routeSearchViewModel = ViewModelProvider(
            this,
            RouteSearchViewModel.factory(
                searchRoutes = container.searchRoutes,
                getCurrentLocation = container.getCurrentLocation,
                routeTrackingProvider = container.routeTrackingProvider,
                saveCompletedHike = container.saveCompletedHike,
                timeProvider = container.timeProvider,
                activeTimer = container.activeTimer,
                getPointsOfInterest = container.getPointsOfInterest,
                ratePointOfInterest = container.ratePointOfInterest,
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
                getPointsOfInterest = container.getPointsOfInterest,
                ratePointOfInterest = container.ratePointOfInterest,
            ),
        )[RouteCreationViewModel::class.java]

        setContent {
            HikeWayTheme {
                HikeWayApp(
                    authViewModel = authViewModel,
                    routeSearchViewModel = routeSearchViewModel,
                    completedHikesViewModel = completedHikesViewModel,
                    routeCreationViewModel = routeCreationViewModel,
                )
            }
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        ) {
            routeSearchViewModel.centerOnCurrentLocation()
            routeSearchViewModel.refreshCurrentSearch()
        }
    }

    private fun requestLocationPermissionForProduction() {
        if (BuildConfig.USE_SIMULATED_GPS) return
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation && !hasCoarseLocation) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
                LOCATION_PERMISSION_REQUEST_CODE,
            )
        }
    }

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}

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
