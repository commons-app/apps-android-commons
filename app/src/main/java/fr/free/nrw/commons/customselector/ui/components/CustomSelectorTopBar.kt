package fr.free.nrw.commons.customselector.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    selectionCount: Int = 0,
    showNavigationIcon: Boolean = true,
    showSelectionCount: Boolean = false,
    showAlertIcon: Boolean = false,
    onAlertAction: ()-> Unit = { },
    onUnselectAllAction: ()-> Unit = { }
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                secondaryText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = modifier,
        navigationIcon = {
            if(showNavigationIcon) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Navigate Back",
                        modifier = Modifier.fillMaxSize()
                    )
                }
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

            if(showSelectionCount) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(end = 8.dp)
                        .semantics { contentDescription = "$selectionCount Selected" }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            .widthIn(min = 52.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(text = "$selectionCount")
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onUnselectAllAction() }
                        )
                    }
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
                showAlertIcon = true,
                selectionCount = 2,
                showSelectionCount = true
            )
        }
    }
}