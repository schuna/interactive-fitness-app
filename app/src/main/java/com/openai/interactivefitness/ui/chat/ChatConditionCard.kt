package com.openai.interactivefitness.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.openai.interactivefitness.R
import com.openai.interactivefitness.domain.DailyCondition

@Composable
fun ChatConditionCard(
    condition: DailyCondition,
    onFatigueChanged: (Int) -> Unit,
    onSorenessChanged: (Int) -> Unit,
    onPainChanged: (Boolean) -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Spa,
                    contentDescription = stringResource(R.string.coach_avatar_description),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        Card(
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.condition_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(R.string.condition_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChatConditionSlider(
                    title = stringResource(R.string.condition_fatigue),
                    value = condition.fatigue,
                    onValueChanged = onFatigueChanged,
                )
                ChatConditionSlider(
                    title = stringResource(R.string.condition_soreness),
                    value = condition.soreness,
                    onValueChanged = onSorenessChanged,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = condition.hasPain,
                        onCheckedChange = onPainChanged,
                    )
                    Text(
                        stringResource(R.string.condition_pain),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (condition.hasPain) {
                    Text(
                        stringResource(R.string.condition_pain_notice),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(stringResource(R.string.condition_apply))
                }
            }
        }
    }
}

@Composable
private fun ChatConditionSlider(
    title: String,
    value: Int,
    onValueChanged: (Int) -> Unit,
) {
    val normalLevel = stringResource(R.string.level_normal)
    val level = listOf(
        stringResource(R.string.level_very_low),
        stringResource(R.string.level_low),
        normalLevel,
        stringResource(R.string.level_high),
        stringResource(R.string.level_very_high),
    ).getOrElse(value - 1) { normalLevel }
    val accessibilityDescription = stringResource(
        R.string.condition_accessibility,
        title,
        value,
        level,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = accessibilityDescription
            },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.condition_value, value, level),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChanged(it.toInt().coerceIn(1, 5)) },
            valueRange = 1f..5f,
            steps = 3,
        )
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.level_low), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.level_high), style = MaterialTheme.typography.bodySmall)
        }
    }
}
