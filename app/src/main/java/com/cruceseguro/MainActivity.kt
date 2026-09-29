package com.cruceseguro

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cruceseguro.controller.MainViewModel
import com.cruceseguro.controller.MainViewModelFactory
import com.cruceseguro.ui.CrossingScreen
import com.cruceseguro.ui.theme.CruceSeguroTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(application) }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshLocationPermission() }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* El flash es opcional; la aplicación continúa aunque se deniegue. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
        setContent {
            CruceSeguroTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                CrossingScreen(
                    state = state,
                    onModeChange = viewModel::setMode,
                    onSimulatedDistanceChange = viewModel::setSimulatedDistance,
                    onAutoApproach = viewModel::startApproaching,
                    onMoveAway = viewModel::startMovingAway,
                    onStop = viewModel::stopSimulation,
                    onCreateCrossing = viewModel::createCrossingHere,
                    onRequestLocation = {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                    onRequestCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                )
            }
        }
    }
}
