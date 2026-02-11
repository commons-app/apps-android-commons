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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    val pagerState = rememberPagerState(initialPage = currentImageIndex) { imageList.size }

    val isZoomed = imageScale > 1f

    BoxWithConstraints {
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()

        HorizontalPager(
            state = pagerState,
            key = { imageList[it].id },
            pageSpacing = 16.dp,
            userScrollEnabled = !isZoomed,
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        imageScale = (imageScale * zoom).coerceIn(1f, 7f)

                        val imageWidth = imageSize.width * imageScale
                        val imageHeight = imageSize.height * imageScale
                        val extraWidth = (imageWidth - maxWidth).coerceAtLeast(0f)
                        val extraHeight = (imageHeight- maxHeight).coerceAtLeast(0f)

                        val maxX = extraWidth / 2
                        val maxY = extraHeight / 2

                        imageOffset = Offset(
                            x = (imageOffset.x + pan.x).coerceIn(-maxX, maxX),
                            y = (imageOffset.y + pan.y).coerceIn(-maxY, maxY)
                        )
                    }
                }
        ) { pageNumber ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageList[pageNumber].uri)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .onSizeChanged { imageSize = it }
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