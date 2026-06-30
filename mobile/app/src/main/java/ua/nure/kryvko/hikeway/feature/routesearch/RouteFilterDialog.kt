package ua.nure.kryvko.hikeway.feature.routesearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedAssistChip
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
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun FilterDialog(
    criteria: RouteSearchCriteria,
    onCriteriaChange: (RouteSearchCriteria) -> Unit,
    onDifficultyToggle: (Difficulty) -> Unit,
    onTerrainToggle: (Terrain) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    var minimumDistance by remember {
        mutableStateOf(
            criteria.distanceKm?.start?.toString().orEmpty()
        )
    }
    var maximumDistance by remember {
        mutableStateOf(
            criteria.distanceKm?.endInclusive?.toString().orEmpty()
        )
    }
    var minimumTime by remember {
        mutableStateOf(
            criteria.estimatedTimeMinutes?.first?.toString().orEmpty()
        )
    }
    var maximumTime by remember {
        mutableStateOf(
            criteria.estimatedTimeMinutes?.last?.toString().orEmpty()
        )
    }
    var maximumProximity by remember {
        mutableStateOf(
            criteria.maxProximityKm?.toString().orEmpty()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Route filters") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Distance, km", style = MaterialTheme.typography.labelLarge)
                RangeFields(
                    minimum = minimumDistance,
                    maximum = maximumDistance,
                    onChange = { minimum, maximum ->
                        minimumDistance = minimum
                        maximumDistance = maximum
                        onCriteriaChange(criteria.copy(distanceKm = decimalRange(minimum, maximum)))
                    },
                )
                Text("Estimated time, minutes", style = MaterialTheme.typography.labelLarge)
                RangeFields(
                    minimum = minimumTime,
                    maximum = maximumTime,
                    onChange = { minimum, maximum ->
                        minimumTime = minimum
                        maximumTime = maximum
                        onCriteriaChange(
                            criteria.copy(
                                estimatedTimeMinutes = integerRange(
                                    minimum,
                                    maximum
                                )
                            )
                        )
                    },
                )
                OutlinedTextField(
                    value = maximumProximity,
                    onValueChange = {
                        maximumProximity = it
                        onCriteriaChange(criteria.copy(maxProximityKm = it.toDoubleOrNull()))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Maximum distance from you, km") },
                    singleLine = true,
                )
                Text("Difficulty", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { difficulty ->
                        ChoiceChip(
                            label = difficulty.label(),
                            selected = difficulty in criteria.difficulties,
                            onClick = { onDifficultyToggle(difficulty) },
                        )
                    }
                }
                Text("Terrain", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Terrain.entries.forEach { terrain ->
                        ChoiceChip(
                            label = terrain.label(),
                            selected = terrain in criteria.terrains,
                            onClick = { onTerrainToggle(terrain) },
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onApply) { Text("Apply") } },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        ElevatedAssistChip(onClick = onClick, label = { Text(label) })
    } else {
        AssistChip(onClick = onClick, label = { Text(label) })
    }
}

@Composable
private fun RangeFields(
    minimum: String,
    maximum: String,
    onChange: (String, String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = minimum,
            onValueChange = { onChange(it, maximum) },
            modifier = Modifier.weight(1f),
            label = { Text("Min") },
            singleLine = true,
        )
        OutlinedTextField(
            value = maximum,
            onValueChange = { onChange(minimum, it) },
            modifier = Modifier.weight(1f),
            label = { Text("Max") },
            singleLine = true,
        )
    }
}

private fun decimalRange(minimum: String, maximum: String): ClosedFloatingPointRange<Double>? {
    val min = minimum.toDoubleOrNull() ?: return null
    val max = maximum.toDoubleOrNull() ?: return null
    return min..max
}

private fun integerRange(minimum: String, maximum: String): IntRange? {
    val min = minimum.toIntOrNull() ?: return null
    val max = maximum.toIntOrNull() ?: return null
    return min..max
}
