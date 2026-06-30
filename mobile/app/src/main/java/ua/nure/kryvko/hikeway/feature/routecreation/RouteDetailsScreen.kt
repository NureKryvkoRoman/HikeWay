package ua.nure.kryvko.hikeway.feature.routecreation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.Terrain

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun RouteDetailsScreen(
    state: RouteCreationUiState,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDifficultySelect: (Difficulty) -> Unit,
    onTerrainSelect: (Terrain) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Route details", style = MaterialTheme.typography.headlineSmall)
        Text("Distance: ${state.distanceKm.formatDistance()}")
        Text("Estimated time: ${state.estimatedTimeMinutes} min")
        Text("Elevation gain: 0 m")
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Route name") },
            singleLine = true,
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description") },
            minLines = 3,
        )
        Text("Difficulty", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { difficulty ->
                ChoiceChip(
                    label = difficulty.label(),
                    selected = state.difficulty == difficulty,
                    onClick = { onDifficultySelect(difficulty) },
                )
            }
        }
        Text("Terrain", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Terrain.entries.forEach { terrain ->
                ChoiceChip(
                    label = terrain.label(),
                    selected = state.terrain == terrain,
                    onClick = { onTerrainSelect(terrain) },
                )
            }
        }
        state.validationMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        state.saveErrorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) {
                Text("Back")
            }
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Button(onClick = onSave, enabled = !state.isSaving) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Save route")
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        ElevatedAssistChip(onClick = onClick, label = { Text(label) })
    } else {
        AssistChip(onClick = onClick, label = { Text(label) })
    }
}
