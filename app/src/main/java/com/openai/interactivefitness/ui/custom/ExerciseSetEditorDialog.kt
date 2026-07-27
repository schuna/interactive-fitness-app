package com.openai.interactivefitness.ui.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.openai.interactivefitness.domain.PlannedExercise
import com.openai.interactivefitness.domain.PlannedSet

@Composable
fun ExerciseSetEditorDialog(
    exercise: PlannedExercise,
    onSave: (PlannedExercise) -> Unit,
    onDismiss: () -> Unit,
) {
    var sets by remember(exercise.id) { mutableStateOf(exercise.sets) }
    var restSeconds by remember(exercise.id) {
        mutableStateOf(exercise.restSeconds.toString())
    }
    val rest = restSeconds.toIntOrNull()
    val isValid = sets.isNotEmpty() &&
        sets.all { it.weightKg >= 0 && it.reps in 1..999 } &&
        rest != null && rest in 0..600
    val totalVolume = sets.sumOf { it.weightKg * it.reps }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.exerciseName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${sets.size}세트 · 총 볼륨 ${formatWeight(totalVolume)}kg",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("세트", modifier = Modifier.width(44.dp))
                    Text("무게(kg)", modifier = Modifier.weight(1f))
                    Text("횟수", modifier = Modifier.weight(1f))
                    Text("삭제", modifier = Modifier.width(40.dp))
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(sets, key = { _, set -> set.id }) { index, set ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("${index + 1}", modifier = Modifier.width(44.dp))
                            OutlinedTextField(
                                value = formatWeight(set.weightKg),
                                onValueChange = { value ->
                                    val parsed = value.toDoubleOrNull() ?: return@OutlinedTextField
                                    sets = sets.toMutableList().also {
                                        it[index] = set.copy(weightKg = parsed.coerceAtLeast(0.0))
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = set.reps.toString(),
                                onValueChange = { value ->
                                    val parsed = value.toIntOrNull() ?: return@OutlinedTextField
                                    sets = sets.toMutableList().also {
                                        it[index] = set.copy(reps = parsed.coerceIn(1, 999))
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                enabled = sets.size > 1,
                                onClick = {
                                    sets = sets.filterNot { it.id == set.id }
                                },
                                modifier = Modifier.width(40.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "${index + 1}세트 삭제",
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        val previous = sets.lastOrNull() ?: PlannedSet()
                        sets = sets + PlannedSet(
                            weightKg = previous.weightKg,
                            reps = previous.reps,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("세트 추가")
                }
                OutlinedTextField(
                    value = restSeconds,
                    onValueChange = { restSeconds = it.filter(Char::isDigit).take(3) },
                    label = { Text("세트 간 휴식(초)") },
                    supportingText = { Text("0~600초") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onSave(exercise.copy(sets = sets, restSeconds = checkNotNull(rest)))
                },
            ) { Text("구성 저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

private fun formatWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
