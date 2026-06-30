package ua.nure.kryvko.hikeway

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import ua.nure.kryvko.hikeway.app.HikeWayApp
import ua.nure.kryvko.hikeway.data.sync.SyncWorkScheduler
import ua.nure.kryvko.hikeway.feature.auth.AuthViewModel
import ua.nure.kryvko.hikeway.feature.completedhikes.CompletedHikesViewModel
import ua.nure.kryvko.hikeway.feature.routecreation.RouteCreationViewModel
import ua.nure.kryvko.hikeway.feature.routesearch.RouteSearchViewModel
import ua.nure.kryvko.hikeway.ui.theme.HikeWayTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val routeSearchViewModel: RouteSearchViewModel by viewModels()
    private val completedHikesViewModel: CompletedHikesViewModel by viewModels()
    private val routeCreationViewModel: RouteCreationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SyncWorkScheduler.schedule(applicationContext)
        requestLocationPermissionForProduction()

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
            routeSearchViewModel.refreshPointsOfInterest()
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
