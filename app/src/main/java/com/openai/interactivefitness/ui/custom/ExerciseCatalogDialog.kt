package com.openai.interactivefitness.ui.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.openai.interactivefitness.domain.ExerciseCatalog
import com.openai.interactivefitness.domain.ExerciseDefinition
import com.openai.interactivefitness.domain.ExerciseEquipment
import com.openai.interactivefitness.domain.ExerciseIllustration
import com.openai.interactivefitness.domain.MuscleGroup

@Composable
fun ExerciseCatalogDialog(
    initiallySelectedIds: Set<String>,
    onConfirm: (List<ExerciseDefinition>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var muscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var equipment by remember { mutableStateOf<ExerciseEquipment?>(null) }
    var selectedIds by remember(initiallySelectedIds) {
        mutableStateOf(initiallySelectedIds)
    }
    val filtered = ExerciseCatalog.filter(query, muscleGroup, equipment)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("운동 종목 선택") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    label = { Text("종목, 부위 또는 장비 검색") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = muscleGroup == null,
                            onClick = { muscleGroup = null },
                            label = { Text("전체 부위") },
                        )
                    }
                    items(MuscleGroup.entries) { group ->
                        FilterChip(
                            selected = muscleGroup == group,
                            onClick = {
                                muscleGroup = group.takeUnless { it == muscleGroup }
                            },
                            label = { Text(group.label) },
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = equipment == null,
                            onClick = { equipment = null },
                            label = { Text("전체 장비") },
                        )
                    }
                    items(ExerciseEquipment.entries) { option ->
                        FilterChip(
                            selected = equipment == option,
                            onClick = {
                                equipment = option.takeUnless { it == equipment }
                            },
                            label = { Text(option.label) },
                        )
                    }
                }
                Text(
                    "${selectedIds.size}개 선택",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = ExerciseDefinition::id) { exercise ->
                        ExerciseCatalogRow(
                            exercise = exercise,
                            selected = exercise.id in selectedIds,
                            onClick = {
                                selectedIds = if (exercise.id in selectedIds) {
                                    selectedIds - exercise.id
                                } else {
                                    selectedIds + exercise.id
                                }
                            },
                        )
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "조건에 맞는 운동 종목이 없습니다.",
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedIds.isNotEmpty(),
                onClick = {
                    onConfirm(ExerciseCatalog.exercises.filter { it.id in selectedIds })
                },
            ) {
                Text("선택 완료 (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Composable
private fun ExerciseCatalogRow(
    exercise: ExerciseDefinition,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExerciseThumbnail(exercise.illustration)
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    exercise.muscleGroups.joinToString(" · ") { it.label } +
                        " · ${exercise.equipment.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (selected) Icons.Outlined.CheckCircle
                else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (selected) "선택됨" else "선택 안 됨",
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExerciseThumbnail(illustration: ExerciseIllustration) {
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.size(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(58.dp)) {
                val stroke = 3.dp.toPx()
                fun line(start: Offset, end: Offset, accent: Boolean = false) {
                    drawLine(
                        color = if (accent) accentColor else lineColor,
                        start = start,
                        end = end,
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(29f, 10f))
                when (illustration) {
                    ExerciseIllustration.VERTICAL_PULL -> {
                        line(Offset(8f, 4f), Offset(50f, 4f))
                        line(Offset(12f, 4f), Offset(12f, 54f))
                        line(Offset(29f, 15f), Offset(29f, 36f), true)
                        line(Offset(29f, 20f), Offset(17f, 8f))
                        line(Offset(29f, 20f), Offset(42f, 8f))
                        line(Offset(29f, 36f), Offset(20f, 52f))
                        line(Offset(29f, 36f), Offset(39f, 52f))
                    }
                    ExerciseIllustration.HORIZONTAL_PULL -> {
                        line(Offset(5f, 51f), Offset(54f, 51f))
                        line(Offset(23f, 18f), Offset(31f, 34f), true)
                        line(Offset(31f, 25f), Offset(49f, 21f))
                        line(Offset(31f, 34f), Offset(18f, 48f))
                        line(Offset(31f, 34f), Offset(42f, 49f))
                    }
                    ExerciseIllustration.HIP_HINGE -> {
                        line(Offset(6f, 52f), Offset(54f, 52f))
                        line(Offset(28f, 17f), Offset(20f, 33f), true)
                        line(Offset(20f, 33f), Offset(41f, 38f))
                        line(Offset(20f, 33f), Offset(15f, 51f))
                        line(Offset(41f, 38f), Offset(48f, 51f))
                    }
                    ExerciseIllustration.PRESS -> {
                        line(Offset(29f, 16f), Offset(29f, 37f), true)
                        line(Offset(29f, 22f), Offset(10f, 15f))
                        line(Offset(29f, 22f), Offset(48f, 15f))
                        line(Offset(29f, 37f), Offset(20f, 53f))
                        line(Offset(29f, 37f), Offset(39f, 53f))
                    }
                    ExerciseIllustration.SQUAT -> {
                        line(Offset(7f, 53f), Offset(52f, 53f))
                        line(Offset(29f, 16f), Offset(29f, 35f), true)
                        line(Offset(15f, 22f), Offset(43f, 22f))
                        line(Offset(29f, 35f), Offset(17f, 43f))
                        line(Offset(29f, 35f), Offset(42f, 43f))
                        line(Offset(17f, 43f), Offset(23f, 53f))
                        line(Offset(42f, 43f), Offset(36f, 53f))
                    }
                    ExerciseIllustration.PLANK -> {
                        line(Offset(5f, 52f), Offset(54f, 52f))
                        line(Offset(14f, 31f), Offset(42f, 38f), true)
                        line(Offset(14f, 31f), Offset(7f, 49f))
                        line(Offset(42f, 38f), Offset(52f, 50f))
                    }
                    ExerciseIllustration.CURL -> {
                        line(Offset(29f, 16f), Offset(29f, 38f), true)
                        line(Offset(29f, 23f), Offset(17f, 33f))
                        line(Offset(17f, 33f), Offset(12f, 23f))
                        line(Offset(29f, 23f), Offset(41f, 33f))
                        line(Offset(41f, 33f), Offset(46f, 23f))
                        line(Offset(29f, 38f), Offset(20f, 54f))
                        line(Offset(29f, 38f), Offset(38f, 54f))
                    }
                    ExerciseIllustration.SHOULDER_PRESS -> {
                        line(Offset(29f, 16f), Offset(29f, 38f), true)
                        line(Offset(29f, 22f), Offset(18f, 13f))
                        line(Offset(18f, 13f), Offset(18f, 4f))
                        line(Offset(29f, 22f), Offset(40f, 13f))
                        line(Offset(40f, 13f), Offset(40f, 4f))
                        line(Offset(29f, 38f), Offset(20f, 54f))
                        line(Offset(29f, 38f), Offset(38f, 54f))
                    }
                }
            }
        }
    }
}
