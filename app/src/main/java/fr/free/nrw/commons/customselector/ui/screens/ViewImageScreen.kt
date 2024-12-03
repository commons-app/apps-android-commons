package fr.free.nrw.commons.customselector.ui.screens

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fr.free.nrw.commons.customselector.domain.model.Image
import kotlin.math.abs

@Composable
fun ViewImageScreen(
    currentImageIndex: Int,
    imageList: List<Image>,
) {
    var imageScale by remember { mutableFloatStateOf(1f) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(imageSize) {
        println("Image Size : $imageSize")
    }

    val pagerState = rememberPagerState(initialPage = currentImageIndex) { imageList.size }

    val scrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            return if (imageScale > 1f) {
                // If zoomed in, consume the scroll for panning
                println("Consuming for panning...")
                available
            } else if (
                source == NestedScrollSource.UserInput && abs(pagerState.currentPageOffsetFraction) > 1e-6
            ) {
                println("Handling swipe gestures...")
                // Handle page swipes only if the image isn't zoomed
                val delta = available.x
                val consumed = -pagerState.dispatchRawDelta(-delta)
                Offset(consumed, 0f)
            } else {
                println("Just passing the as it is...")
                Offset.Zero
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        key = { imageList[it].id },
        pageSpacing = 16.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    containerSize = it
                },
            contentAlignment = Alignment.Center
        ) {
//            val state = rememberTransformableState { zoomChange, panChange, _ ->
//                imageScale = (imageScale * zoomChange).coerceIn(1f, 7f)
//
//                val imageWidth = imageSize.width * imageScale
//                val imageHeight = imageSize.height * imageScale
//
//                val extraWidth = (imageWidth - constraints.maxWidth).coerceAtLeast(0f)
//                val extraHeight = (imageHeight - constraints.maxHeight).coerceAtLeast(0f)
//
//                val maxX = extraWidth / 2
//                val maxY = extraHeight / 2
//
//                imageOffset = Offset(
//                    x = (imageOffset.x + imageScale * panChange.x).coerceIn(-maxX, maxX),
//                    y = (imageOffset.y + imageScale * panChange.y).coerceIn(-maxY, maxY)
//                )
//            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageList[it].uri)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { imageSize = it }
//                    .pointerInput(Unit) {
//                        detectTransformGestures { centroid, pan, zoom, _ ->
//                            imageScale = (imageScale * zoom).coerceIn(1f, 7f)
//
//                            val imageWidth = imageSize.width * imageScale
//                            val imageHeight = imageSize.height * imageScale
//
//                            val extraWidth = (imageWidth-constraints.maxWidth).coerceAtLeast(0f)
//                            val extraHeight = (imageHeight-constraints.maxHeight).coerceAtLeast(0f)
//
//                            val maxX = extraWidth / 2
//                            val maxY = extraHeight / 2
//
//                            imageOffset = Offset(
//                                x = (imageOffset.x + imageScale * pan.x).coerceIn(
//                                    -maxX,
//                                    maxX
//                                ),
//                                y = (imageOffset.y + imageScale * pan.y).coerceIn(-maxY, maxY)
//                            )
//                        }
//                    }
                    .graphicsLayer {
                        scaleX = imageScale
                        scaleY = imageScale
                        translationX = imageOffset.x
                        translationY = imageOffset.y
                    }
            )
        }
    }
}

suspend fun PointerInputScope.detectDragAndZoomGestures(
    onZoom: (Float) -> Unit,
    onDrag: (Offset) -> Unit
) {
    detectTransformGestures { _, pan, zoom, _ ->
        onZoom(zoom)
        onDrag(pan)
    }
}

fun Offset.calculateNewOffset(
    centroid: Offset,
    pan: Offset,
    zoom: Float,
    gestureZoom: Float,
    size: IntSize
): Offset {
    val newScale = maxOf(1f, zoom * gestureZoom)
    val newOffset = (this + centroid / zoom) -
            (centroid / newScale + pan / zoom)
    return Offset(
        newOffset.x.coerceIn(0f, (size.width / zoom) * (zoom - 1f)),
        newOffset.y.coerceIn(0f, (size.height / zoom) * (zoom - 1f))
    )
}

fun calculateDoubleTapOffset(
    zoom: Float,
    size: IntSize,
    tapOffset: Offset
): Offset {
    val newOffset = Offset(tapOffset.x, tapOffset.y)
    return Offset(
        newOffset.x.coerceIn(0f, (size.width / zoom) * (zoom - 1f)),
        newOffset.y.coerceIn(0f, (size.height / zoom) * (zoom - 1f))
    )
}