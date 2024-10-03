package fr.free.nrw.commons.customselector.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import coil.compose.rememberAsyncImagePainter
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.ui.components.CustomSelectorBottomBar
import fr.free.nrw.commons.customselector.ui.components.CustomSelectorTopBar
import fr.free.nrw.commons.customselector.ui.components.PartialStorageAccessDialog
import fr.free.nrw.commons.ui.theme.CommonsTheme

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CustomSelectorScreen(
    uiState: CustomSelectorState,
    onEvent: (CustomSelectorEvent)-> Unit,
    selectedImageIds: List<Long>
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val navigator = rememberListDetailPaneScaffoldNavigator<Folder>()

    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }
    LaunchedEffect(key1 = navigator.currentDestination, key2 = navigator.scaffoldValue) {
        println("Current Dest:- ${navigator.currentDestination} | Scaffold Value:- ${navigator.scaffoldValue}")
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective.copy(horizontalPartitionSpacerSize = 0.dp),
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                FoldersPane(uiState = uiState,
                    onFolderClick = {
                        onEvent(CustomSelectorEvent.OnFolderClick(it.bucketId))
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, it)
                    },
                    adaptiveInfo = adaptiveInfo
                )
            }
        },
        detailPane = {
            AnimatedPane(modifier = Modifier) {
                navigator.currentDestination?.content?.let { folder->
                    ImagesPane(
                        selectedFolder = folder,
                        selectedImages = selectedImageIds,
                        imageList = uiState.filteredImages,
                        onNavigateBack = { navigator.navigateBack() },
                        onToggleImageSelection = { onEvent(CustomSelectorEvent.OnImageSelect(it)) },
                        adaptiveInfo = adaptiveInfo
                    )
                }
            }
        },
    )
}

@Composable
fun FoldersPane(
    uiState: CustomSelectorState,
    onFolderClick: (Folder)-> Unit,
    adaptiveInfo: WindowAdaptiveInfo
) {
    Scaffold(
        topBar = {
            Surface(tonalElevation = 1.dp) {
                CustomSelectorTopBar(
                    primaryText = stringResource(R.string.custom_selector_title),
                    onNavigateBack = { /*TODO*/ },
                    showAlertIcon = true
                )
            }
        },
        bottomBar = {
            Surface(tonalElevation = 1.dp) {
                CustomSelectorBottomBar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    ) { innerPadding->
        Surface(tonalElevation = 0.dp) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if(adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT) {
                    PartialStorageAccessDialog(
                        isVisible = true,
                        onManage = { /*TODO*/ },
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if(uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(164.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.fillMaxSize(1f)
                    ) {
                        items(uiState.folders, key = { it.bucketId }) {
                            FolderItem(
                                previewPainter = rememberAsyncImagePainter(model = it.preview),
                                folderName = it.bucketName,
                                itemsCount = it.itemsCount,
                                onClick = { onFolderClick(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderItem(
    previewPainter: Painter,
    folderName: String,
    itemsCount: Int,
    onClick: ()-> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Image(
            painter = previewPainter,
            contentDescription = null,
            modifier = Modifier.aspectRatio(1f),
            contentScale = ContentScale.Crop
        )
        Text(
            text = "$itemsCount",
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 32.dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(bottomStart = 12.dp))
                .background(color = MaterialTheme.colorScheme.secondaryContainer)
                .padding(4.dp)
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomStart),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = folderName,
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FolderItemPreview() {
    CommonsTheme {
        Surface {
            FolderItem(
                previewPainter = painterResource(R.drawable.placeholder_image),
                folderName = "Folder Name",
                itemsCount = 12,
                onClick = { },
                modifier = Modifier
                    .padding(16.dp)
                    .size(164.dp)
            )
        }
    }
}

@Preview
@Composable
private fun CustomSelectorScreenPreview() {
    CommonsTheme {
        CustomSelectorScreen(
            uiState = CustomSelectorState(),
            onEvent = { },
            selectedImageIds = emptyList()
        )
    }
}