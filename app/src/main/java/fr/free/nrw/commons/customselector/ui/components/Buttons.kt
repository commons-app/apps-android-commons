package fr.free.nrw.commons.customselector.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.free.nrw.commons.ui.theme.CommonsTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: ()-> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: ()-> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.primary),
        shape = shape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center
        )
    }
}

@PreviewLightDark
@Composable
private fun PrimaryButtonPreview() {
    CommonsTheme {
        Surface {
            PrimaryButton(
                text = "Primary Button",
                onClick = { },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SecondaryButtonPreview() {
    CommonsTheme {
        Surface {
            SecondaryButton(
                text = "Secondary Button",
                onClick = { },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}