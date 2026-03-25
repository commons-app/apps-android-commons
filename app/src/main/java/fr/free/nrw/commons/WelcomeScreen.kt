package fr.free.nrw.commons

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    isBetaFlavour: Boolean,
    pageCount: Int = 5,
    onSkipClicked: () -> Unit,
    onBackPressedAtStart: () -> Unit

) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    var showDialog by rememberSaveable { mutableStateOf(isBetaFlavour) }
    val backgroundColor = if (isSystemInDarkTheme()) {
        Color(0xFF303030)
    } else {
        colorResource(id = R.color.primaryColor)
    }
    BackHandler {
        if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        } else {
            onBackPressedAtStart()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) {
            page ->
            when (page){
                0 -> WelcomeWikipediaPage()
                1 -> WelcomeDoUploadPage()
                2 -> WelcomeDontUploadPage()
                3 -> WelcomeImageExamplePage()
                4 -> WelcomeFinalPage(onFinishClicked = onSkipClicked)
            }

        }

        if (isBetaFlavour) {
            Text(
                text = stringResource(id = R.string.welcome_skip_button),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(16.dp)
                    .clickable { onSkipClicked() }
            )
        }

        PageIndicator(
            pageCount = pageCount,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 16.dp)
        )
    }

    if (showDialog) {
        CopyrightDialog(
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        repeat(pageCount) { index ->
            val color = if (index == currentPage) Color.White else Color.White.copy(alpha = 0.5f)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
@Composable
fun CopyrightDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .width(350.dp)
                .height(350.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(37.dp))

                Text(
                    text = stringResource(id = R.string.warning),
                    color = Color(0xFFFF0202),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(21.dp))

                Text(
                    text = stringResource(id = R.string.copyright_popup),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(37.dp))

                Button(
                    onClick = { onDismiss() }
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            }
        }
    }
}