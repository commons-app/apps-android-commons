package fr.free.nrw.commons.customselector.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import androidx.window.core.layout.WindowWidthSizeClass
import coil.compose.rememberAsyncImagePainter
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.ui.components.CustomSelectorBottomBar
import fr.free.nrw.commons.customselector.ui.components.CustomSelectorTopBar
import fr.free.nrw.commons.customselector.ui.components.PartialStorageAccessDialog
import fr.free.nrw.commons.customselector.ui.states.CustomSelectorUiState
import fr.free.nrw.commons.customselector.ui.states.ImageUiState
import fr.free.nrw.commons.ui.theme.CommonsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagesPane(
    uiState: CustomSelectorUiState,
    selectedFolder: Folder,
    selectedImages: () -> Set<Long>,
    onNavigateBack: () -> Unit,
    onViewImage: (id: Long)-> Unit,
    onEvent: (CustomSelectorEvent) -> Unit,
    adaptiveInfo: WindowAdaptiveInfo,
    hasPartialAccess: Boolean = false
) {
    val lazyGridState = rememberLazyGridState()
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }
    val isCompatWidth by remember(adaptiveInfo.windowSizeClass) {
        derivedStateOf {
            adaptiveInfo.windowSizeClass
                .windowWidthSizeClass == WindowWidthSizeClass.COMPACT
        }
    }

    LaunchedEffect(autoScrollSpeed) {
        if (autoScrollSpeed != 0f) {
            while (isActive) {
                lazyGridState.scrollBy(autoScrollSpeed)
                delay(10)
            }
        }
    }

    Scaffold(
        topBar = {
            CustomSelectorTopBar(
                primaryText = selectedFolder.bucketName,
                secondaryText = "${selectedFolder.itemsCount} images",
                onNavigateBack = onNavigateBack,
                showNavigationIcon = isCompatWidth,
                showAlertIcon = selectedImages().size > 20,
                selectionCount = selectedImages().size,
                showSelectionCount = uiState.inSelectionMode,
                onUnselectAllAction = { onEvent(CustomSelectorEvent.OnUnselectAll) }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = uiState.inSelectionMode && isCompatWidth,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(tonalElevation = 1.dp) {
                    CustomSelectorBottomBar(
                        onPrimaryAction = { /*TODO("Implement action to upload selected images")*/ },
                        onSecondaryAction = { /*TODO("Implement action to mark/unmark as not for upload")*/ },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (hasPartialAccess) {
                PartialStorageAccessDialog(
                    onManageAction = { /*TODO("Request permission[READ_MEDIA_IMAGES]")*/ },
                    modifier = Modifier.padding(8.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .imageGridDragHandler(
                        gridState = lazyGridState,
                        imageList = uiState.filteredImages,
                        selectedImageIds = selectedImages,
                        setSelectedImageIds = {
                            onEvent(CustomSelectorEvent.OnDragImageSelection(it))
                        },
                        autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() },
                        setAutoScrollSpeed = { autoScrollSpeed = it }
                    ),
                state = lazyGridState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(uiState.filteredImages, key = { it.id }) { image ->
                    val isSelected by remember {
                        derivedStateOf { selectedImages().contains(image.id) }
                    }

                    ImageItem(
                        imagePainter = rememberAsyncImagePainter(model = image.uri),
                        isSelected = isSelected,
                        inSelectionMode = uiState.inSelectionMode,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (uiState.inSelectionMode) {
                                    onEvent(CustomSelectorEvent.OnImageSelection(image.id))
                                } else {
                                    onViewImage(image.id)
                                }
                            },
                            onLongClick = {
                                onEvent(CustomSelectorEvent.OnImageSelection(image.id))
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ImageItem(
    imagePainter: Painter,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    inSelectionMode: Boolean = false
) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
        Image(
            painter = imagePainter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Crop
        )

        if (inSelectionMode) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(bottomEnd = 12.dp))
                        .background(color = MaterialTheme.colorScheme.primary)
                        .padding(2.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(bottomEnd = 12.dp))
                        .background(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(2.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun ImageItemPreview() {
    CommonsTheme {
        Surface {
            ImageItem(
                imagePainter = painterResource(id = R.drawable.image_placeholder_96),
                isSelected = false,
                inSelectionMode = true,
                modifier = Modifier
                    .padding(16.dp)
                    .size(116.dp)
            )
        }
    }
}

/**
 * A modifier that handles drag gestures on an image grid to allow for selecting multiple images.
 *
 * This modifier detects drag gestures and updates the selected images based on the drag position.
 * It also handles auto-scrolling when the drag reaches the edges of the grid.
 *
 * @param gridState The state of the lazy grid.
 * @param imageList The list of images displayed in the grid.
 * @param selectedImageIds A function that returns the currently selected image IDs.
 * @param autoScrollThreshold The distance from the edge of the grid at which auto-scrolling should start.
 * @param setSelectedImageIds A callback function that is invoked when the selected images change.
 * @param setAutoScrollSpeed A callback function that is invoked to set the auto-scroll speed.
 */
fun Modifier.imageGridDragHandler(
    gridState: LazyGridState,
    imageList: List<ImageUiState>,
    selectedImageIds: () -> Set<Long>,
    autoScrollThreshold: Float,
    setSelectedImageIds: (Set<Long>) -> Unit,
    setAutoScrollSpeed: (Float) -> Unit,
) = pointerInput(autoScrollThreshold, setAutoScrollSpeed, imageList) {

    fun imageIndexAtOffset(hitPoint: Offset): Int? =
        gridState.layoutInfo.visibleItemsInfo.find { itemInfo ->
            itemInfo.size.toIntRect().contains(hitPoint.round() - itemInfo.offset)
        }?.index

    var dragStartIndex: Int? = null
    var currentDragIndex: Int? = null
    var isSelecting = true

    detectDragGestures(
        onDragStart = { offset ->
            imageIndexAtOffset(offset)?.let {
                val imageId = imageList[it].id
                dragStartIndex = it
                currentDragIndex = it

                if (!selectedImageIds().contains(imageId)) {
                    isSelecting = true
                    setSelectedImageIds(selectedImageIds().plus(imageId))
                } else {
                    isSelecting = false
                    setSelectedImageIds(selectedImageIds().minus(imageId))
                }
            }
        },
        onDragEnd = { setAutoScrollSpeed(0f); dragStartIndex = null },
        onDragCancel = { setAutoScrollSpeed(0f); dragStartIndex = null },
        onDrag = { change, _ ->
            dragStartIndex?.let { startIndex ->
                val distFromBottom = gridState.layoutInfo.viewportSize.height - change.position.y
                val distFromTop = change.position.y
                setAutoScrollSpeed(
                    when {
                        distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                        distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
                        else -> 0f
                    }
                )

                currentDragIndex?.let { currentIndex ->
                    imageIndexAtOffset(change.position)?.let { pointerIndex ->
                        if (currentIndex != pointerIndex) {
                            if (isSelecting) {
                                setSelectedImageIds(
                                    selectedImageIds().minus(
                                        imageList.getImageIdsInRange(startIndex, currentIndex)
                                    ).plus(
                                        imageList.getImageIdsInRange(startIndex, pointerIndex)
                                    )
                                )
                            } else {
                                setSelectedImageIds(
                                    selectedImageIds().plus(
                                        imageList.getImageIdsInRange(currentIndex, pointerIndex)
                                    ).minus(
                                        imageList.getImageIdsInRange(startIndex, pointerIndex)
                                    )
                                )
                            }
                            currentDragIndex = pointerIndex
                        }
                    }
                }
            }
        }
    )
}

/**
 * Calculates a set of image IDs within a given range of indices in a list of images.
 *
 * @param initialKey The starting index of the range.
 * @param pointerKey The ending index of the range.
 * @return A set of image IDs within the specified range.
 */
fun List<ImageUiState>.getImageIdsInRange(initialKey: Int, pointerKey: Int): Set<Long> {
    val setOfKeys = mutableSetOf<Long>()
    if (initialKey < pointerKey) {
        (initialKey..pointerKey).forEach {
            setOfKeys.add(this[it].id)
        }
    } else {
        (pointerKey..initialKey).forEach {
            setOfKeys.add(this[it].id)
        }
    }
    return setOfKeys
}