package fr.free.nrw.commons.customselector.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ui.theme.CommonsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSelectorTopBar(
    primaryText: String,
    onNavigateBack: ()-> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    showAlertIcon: Boolean = false,
    onAlertAction: ()-> Unit = { },
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                secondaryText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Navigate Back",
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        actions = {
            if(showAlertIcon) {
                IconButton(onClick = onAlertAction) {
                    Icon(
                        painter = painterResource(R.drawable.ic_error_red_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

@PreviewLightDark
@Composable
private fun CustomSelectorTopBarPreview() {
    CommonsTheme {
        Surface(tonalElevation = 1.dp) {
            CustomSelectorTopBar(
                primaryText = "My Folder",
                secondaryText = "10 images",
                onNavigateBack = { },
                showAlertIcon = true
            )
        }
    }
}