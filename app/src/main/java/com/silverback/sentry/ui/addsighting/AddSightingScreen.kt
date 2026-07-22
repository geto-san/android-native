package com.silverback.sentry.ui.addsighting

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSightingScreen(
    onDone: () -> Unit,
    onNavigateToCamera: () -> Unit,
    viewModel: AddSightingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
        if (granted) viewModel.loadCurrentLocation()
    }
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            viewModel.loadCurrentLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(uiState.savedObservationId) {
        if (uiState.savedObservationId != null) {
            viewModel.consumeSavedEvent()
            onDone()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("New Gorilla Sighting") })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                LocationPreview(uiState, hasLocationPermission)

                OutlinedTextField(
                    value = uiState.gorillaGroup,
                    onValueChange = viewModel::updateGorillaGroup,
                    label = { Text("Gorilla Group *") },
                    placeholder = { Text("e.g. Susa Group") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )

                OutlinedTextField(
                    value = uiState.healthStatus,
                    onValueChange = viewModel::updateHealthStatus,
                    label = { Text("Health Status") },
                    placeholder = { Text("e.g. Healthy, Respiratory signs") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::updateNotes,
                    label = { Text("Notes") },
                    placeholder = { Text("Group size, behavior, threats...") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )

                PhotoGallery(
                    photoUris = uiState.photoUris,
                    onAddPhoto = onNavigateToCamera,
                    onRemovePhoto = viewModel::removePhoto,
                )

                uiState.saveError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Button(
                    onClick = viewModel::save,
                    enabled = uiState.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Save Sighting")
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPreview(uiState: AddSightingUiState, hasLocationPermission: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = when {
                !hasLocationPermission -> "Location permission needed"
                uiState.isLoadingLocation -> "Getting your location..."
                uiState.locationError != null -> uiState.locationError
                else -> uiState.locationName ?: "${uiState.latitude}, ${uiState.longitude}"
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun PhotoGallery(photoUris: List<String>, onAddPhoto: () -> Unit, onRemovePhoto: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text("Photos (${photoUris.size})", style = MaterialTheme.typography.labelLarge)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            item {
                OutlinedButton(onClick = onAddPhoto, modifier = Modifier.size(80.dp)) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photo")
                }
            }
            items(photoUris) { uri ->
                Box(modifier = Modifier.size(80.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    IconButton(onClick = { onRemovePhoto(uri) }, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove photo",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
