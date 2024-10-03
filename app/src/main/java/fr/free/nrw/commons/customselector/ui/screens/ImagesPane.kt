package fr.free.nrw.commons.customselector.ui.screens

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
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import androidx.window.core.layout.WindowWidthSizeClass
import coil.compose.rememberAsyncImagePainter
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.components.CustomSelectorTopBar
import fr.free.nrw.commons.customselector.ui.components.PartialStorageAccessDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagesPane(
    selectedFolder: Folder,
    selectedImages: List<Long>,
    imageList: List<Image>,
    onNavigateBack: ()-> Unit,
    onToggleImageSelection: (Long) -> Unit,
    adaptiveInfo: WindowAdaptiveInfo
) {
    val inSelectionMode by remember { derivedStateOf { selectedImages.isNotEmpty() } }
    val lazyGridState = rememberLazyGridState()
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }

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
                showNavigationIcon = adaptiveInfo.windowSizeClass
                    .windowWidthSizeClass == WindowWidthSizeClass.COMPACT
            )
        }
    ) { innerPadding->
        Column(modifier = Modifier.padding(innerPadding)) {
            PartialStorageAccessDialog(
                isVisible = true,
                onManage = { /*TODO*/ },
                modifier = Modifier.padding(8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(116.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .imageGridDragHandler(
                        gridState = lazyGridState,
                        imageList = imageList,
                        selectedImageIds = { selectedImages },
                        onImageSelect = { onToggleImageSelection(it) },
                        autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() },
                        setAutoScrollSpeed = { autoScrollSpeed = it }
                    ),
                state = lazyGridState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(imageList, key = { it.id }) { image->
                    val isSelected by remember {
                        derivedStateOf { selectedImages.contains(image.id) }
                    }

                    ImageItem(
                        imagePainter = rememberAsyncImagePainter(model = image.uri),
                        isSelected = isSelected,
                        inSelectionMode = inSelectionMode,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if(inSelectionMode) {
                                    onToggleImageSelection(image.id)
                                }
                            },
                            onLongClick = {
                                onToggleImageSelection(image.id)
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

        if(inSelectionMode) {
            if(isSelected) {
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

fun Modifier.imageGridDragHandler(
    gridState: LazyGridState,
    imageList: List<Image>,
    selectedImageIds:()-> List<Long>,
    autoScrollThreshold: Float,
    onImageSelect: (Long) -> Unit = { },
    setAutoScrollSpeed: (Float) -> Unit = { },
) = pointerInput(autoScrollThreshold, setAutoScrollSpeed, onImageSelect) {

    fun imageIndexAtOffset(hitPoint: Offset): Int? =
        gridState.layoutInfo.visibleItemsInfo.find { itemInfo ->
            itemInfo.size.toIntRect().contains(hitPoint.round() - itemInfo.offset)
        }?.index

    var dragStartIndex: Int? = null
    var currentDragIndex: Int? = null
    var isSelecting = true

    detectDragGestures(
        onDragStart = { offset->
            imageIndexAtOffset(offset)?.let {
                val imageId = imageList[it].id
                if(!selectedImageIds().contains(imageId)) {
                    dragStartIndex = it
                    currentDragIndex = it
                    onImageSelect(imageList[it].id)
                }
            }
        },
        onDragEnd = { setAutoScrollSpeed(0f); dragStartIndex = null },
        onDragCancel = { setAutoScrollSpeed(0f); dragStartIndex = null },
        onDrag = { change, _->
            dragStartIndex?.let { startIndex->
                currentDragIndex?.let { endIndex->
                    val start = minOf(startIndex, endIndex)
                    val end = maxOf(start, endIndex)

                    (start..end).forEach { index->
                        val imageId = imageList[index].id
                        val ifContains = selectedImageIds().contains(imageId)
                        if (isSelecting && !selectedImageIds().contains(imageId)) {
                            println("Selecting...")
                            println("contains: $ifContains")
                            onImageSelect(imageId)
                        } else if (!isSelecting && selectedImageIds().contains(imageId)) {
                            onImageSelect(imageId)
                        }
                    }
                }
            }
        }
    )
}

private fun Set<Int>.addUpTo(
    initialKey: Int?,
    pointerKey: Int?
): Set<Int> {
    return if(initialKey == null || pointerKey == null) {
        this
    } else {
        this.plus(initialKey..pointerKey)
            .plus(pointerKey..initialKey)
    }
}

private fun Set<Int>.removeUpTo(
    initialKey: Int?,
    previousPointerKey: Int?
): Set<Int> {
    return if(initialKey == null || previousPointerKey == null) {
        this
    } else {
        this.minus(initialKey..previousPointerKey)
            .minus(previousPointerKey..initialKey)
    }
}