package fr.free.nrw.commons.customselector.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.free.nrw.commons.ui.theme.CommonsTheme

@Composable
fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val duration = 300

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = duration),
        label = "thumbOffset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = duration),
        label = "trackColor"
    )

    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else Color(0xFF5F6368),
        animationSpec = tween(durationMillis = duration),
        label = "trackBorderColor"
    )

    val thumbBackgroundColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimary else Color.Transparent,
        animationSpec = tween(durationMillis = duration),
        label = "thumbBackgroundColor"
    )

    val thumbBorderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimary else Color(0xFF5F6368),
        animationSpec = tween(durationMillis = duration),
        label = "thumbBorderColor"
    )

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(
                width = 1.dp,
                color = trackBorderColor,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .clip(CircleShape)
                .background(thumbBackgroundColor)
                .border(
                    width = 1.dp,
                    color = thumbBorderColor,
                    shape = CircleShape
                )
        )
    }
}

@PreviewLightDark
@Composable
private fun ToggleSwitchPreview() {
    var isChecked by remember { mutableStateOf(false) }

    CommonsTheme {
        Surface {
            Box(modifier = Modifier.padding(8.dp)) {
                ToggleSwitch(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it }
                )
            }
        }
    }
}
