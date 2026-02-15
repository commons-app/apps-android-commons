package fr.free.nrw.commons.customselector.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fr.free.nrw.commons.customselector.ui.states.ImageUiState

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ViewImageScreen(
    currentImageIndex: Int,
    imageList: List<ImageUiState>,
) {
    var imageScale by remember { mutableFloatStateOf(1f) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) }
    val pagerState = rememberPagerState(initialPage = currentImageIndex) { imageList.size }
    val imageDimensions = remember { mutableStateMapOf<Long, IntSize>() }
    // Swipe is enabled only when the image is not zoomed.
    val isZoomed = imageScale > 1f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()

        HorizontalPager(
            state = pagerState,
            key = { imageList[it].id },
            pageSpacing = 16.dp,
            userScrollEnabled = !isZoomed,
            modifier = Modifier.fillMaxSize()
                .pointerInput(maxWidth, maxHeight) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        imageScale = (imageScale * zoom).coerceIn(1f, 7f)

                        val currentImageSize = imageDimensions[imageList[pagerState.currentPage].id]
                            ?: IntSize.Zero
                        val baseScale = minOf(
                            maxWidth / currentImageSize.width,
                            maxHeight / currentImageSize.height
                        )
                        val imageWidth = currentImageSize.width * imageScale * baseScale
                        val imageHeight = currentImageSize.height * imageScale * baseScale

                        // Max panning distance is half of the 'extra' width/height that exceeds the screen size.
                        val extraWidth = (imageWidth - maxWidth).coerceAtLeast(0f)
                        val extraHeight = (imageHeight - maxHeight).coerceAtLeast(0f)

                        val maxX = extraWidth / 2
                        val maxY = extraHeight / 2

                        val center = Offset(maxWidth / 2, maxHeight / 2)
                        // Adjust offset to keep the point under the centroid stable during zoom.
                        val newOffset = (imageOffset * zoom) + (centroid - center) * (1f - zoom) + pan

                        imageOffset = Offset(
                            x = newOffset.x.coerceIn(-maxX, maxX),
                            y = newOffset.y.coerceIn(-maxY, maxY)
                        )
                    }
                }
        ) { pageNumber ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageList[pageNumber].uri)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    imageDimensions[imageList[pageNumber].id] = IntSize(
                        state.result.drawable.intrinsicWidth,
                        state.result.drawable.intrinsicHeight,
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
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
