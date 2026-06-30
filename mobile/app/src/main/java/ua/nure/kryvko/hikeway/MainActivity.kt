package ua.nure.kryvko.hikeway

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import ua.nure.kryvko.hikeway.app.AppContainer
import ua.nure.kryvko.hikeway.app.HikeWayApp
import ua.nure.kryvko.hikeway.feature.auth.AuthViewModel
import ua.nure.kryvko.hikeway.feature.completedhikes.CompletedHikesViewModel
import ua.nure.kryvko.hikeway.feature.routecreation.RouteCreationViewModel
import ua.nure.kryvko.hikeway.feature.routesearch.RouteSearchViewModel
import ua.nure.kryvko.hikeway.ui.theme.HikeWayTheme
import ua.nure.kryvko.hikeway.data.sync.SyncWorkScheduler

class MainActivity : ComponentActivity() {
    private lateinit var routeSearchViewModel: RouteSearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SyncWorkScheduler.schedule(applicationContext)
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
                getNearbyPointsOfInterest = container.getNearbyPointsOfInterest,
                getPointOfInterestDetail = container.getPointOfInterestDetail,
                createPointOfInterest = container.createPointOfInterest,
                updatePointOfInterest = container.updatePointOfInterest,
                deletePointOfInterest = container.deletePointOfInterest,
                ratePointOfInterest = container.ratePointOfInterest,
                removePointOfInterestRating = container.removePointOfInterestRating,
                addPoiComment = container.addPoiComment,
                updatePoiComment = container.updatePoiComment,
                deletePoiComment = container.deletePoiComment,
                uploadPoiPhoto = container.uploadPoiPhoto,
                updatePoiPhoto = container.updatePoiPhoto,
                deletePoiPhoto = container.deletePoiPhoto,
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
