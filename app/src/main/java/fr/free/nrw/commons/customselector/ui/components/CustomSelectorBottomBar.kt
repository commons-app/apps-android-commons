package fr.free.nrw.commons.customselector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ui.theme.CommonsTheme

@Composable
fun CustomSelectorBottomBar(
    onPrimaryAction: ()-> Unit,
    onSecondaryAction: ()-> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SecondaryButton(
            text = stringResource(R.string.mark_as_not_for_upload).uppercase(),
            onClick = onSecondaryAction,
            modifier = Modifier.weight(1f)
        )

        PrimaryButton(
            text = stringResource(R.string.upload).uppercase(),
            onClick = onPrimaryAction,
            modifier = Modifier
                .weight(1f)
                .height(IntrinsicSize.Max)
        )
    }
}

@PreviewLightDark
@Composable
private fun CustomSelectorBottomBarPreview() {
    CommonsTheme {
        Surface(tonalElevation = 3.dp) {
            CustomSelectorBottomBar(
                onPrimaryAction = { },
                onSecondaryAction = { },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            )
        }
    }
}