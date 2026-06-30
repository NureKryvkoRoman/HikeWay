package ua.nure.kryvko.hikeway.feature.pois

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PointOfInterest

@Composable
fun UploadPhotoRow(
    caption: String,
    onCaptionChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = caption,
            onValueChange = onCaptionChange,
            label = { Text("Photo caption") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
        )
        Button(onClick = onPickPhoto, enabled = enabled) {
            Text("Add photo")
        }
    }
}

@Composable
fun PoiOwnerActions(
    poi: PointOfInterest,
    onUpdatePoi: ((String, String, GeoPoint) -> Unit)?,
    onDeletePoi: (() -> Unit)?,
    enabled: Boolean,
    isAdmin: Boolean,
) {
    if (!poi.ownedByCurrentUser && !isAdmin) return
    var name by remember(poi.id, poi.name) { mutableStateOf(poi.name) }
    var description by remember(poi.id, poi.description) { mutableStateOf(poi.description) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Manage PoI", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onUpdatePoi?.let {
                Button(
                    onClick = { it(name, description, poi.location) },
                    enabled = enabled,
                ) {
                    Text("Save PoI")
                }
            }
            onDeletePoi?.let {
                TextButton(onClick = it, enabled = enabled) {
                    Text("Delete PoI")
                }
            }
        }
    }
}
