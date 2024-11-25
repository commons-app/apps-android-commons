package fr.free.nrw.commons.fileusages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import fr.free.nrw.commons.R
import fr.free.nrw.commons.fileusages.model.FileUsagesResponse
import fr.free.nrw.commons.fileusages.model.NoContributionsError
import kotlinx.coroutines.launch

@Composable
fun FileUsagesScreen(modifier: Modifier = Modifier, viewModel: FileUsagesViewModel) {
    val fileUsagesLazyPagingItems =
        viewModel.fileUsagesPagingData.collectAsLazyPagingItems()

    val globalFileUsagesLazyPagingItems =
        viewModel.globalFileUsagesPagingData.collectAsLazyPagingItems()

    val screenState by viewModel.screenState.collectAsState()

    val pagerState = rememberPagerState { ListContentType.entries.size }

    val scope = rememberCoroutineScope()

    val listState1 = rememberLazyListState()
    val listState2 = rememberLazyListState()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            contentColor = Color.Transparent,
            containerColor = if (isSystemInDarkTheme())
                colorResource(R.color.contributionListDarkBackground)
            else colorResource(R.color.card_light_grey),
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = if (isSystemInDarkTheme()) colorResource(R.color.white)
                        else colorResource(R.color.primaryDarkColor)
                    )
                }
            }
        )

        {
            ListContentType.entries.onEachIndexed() { tabIndex, tabEntry ->

                Tab(
                    selected = pagerState.currentPage == tabIndex,
                    onClick = { scope.launch { pagerState.scrollToPage(tabIndex) } },
                    text = { Text(tabEntry.navTitle) },
                    selectedContentColor = if (isSystemInDarkTheme()) colorResource(R.color.white)
                    else colorResource(R.color.primaryDarkColor),
                    unselectedContentColor = if (isSystemInDarkTheme()) colorResource(R.color.white)
                    else colorResource(R.color.primaryDarkColor)
                )
            }
        }

        HorizontalPager(state = pagerState) { currentIndex ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (currentIndex) {
                    0 -> {
                        FileUsages(
                            refreshLoadState = fileUsagesLazyPagingItems.loadState.refresh,
                            pagingItems = fileUsagesLazyPagingItems,
                            usagesType = ListContentType.Commons,
                            state = listState1
                        )
                    }

                    1 -> {
                        FileUsages(
                            refreshLoadState = globalFileUsagesLazyPagingItems.loadState.refresh,
                            pagingItems = globalFileUsagesLazyPagingItems,
                            usagesType = ListContentType.OtherWikis,
                            state = listState2
                        )
                    }
                }
            }
        }

    }
}

@Composable
fun GlobalUsagesListContent(data: LazyPagingItems<UiModel>, state: LazyListState) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(state = state) {
        items(data) {
            it?.let { item ->
                when (item) {
                    is UiModel.HeaderModel -> {
                        Column {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = item.group,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            )
                            HorizontalDivider()
                        }
                    }

                    is UiModel.ItemModel -> ListItem(leadingContent = {
                        Text(text = "•", fontWeight = FontWeight.Bold)
                    }, headlineContent = {
                        Text(
                            modifier = Modifier.clickable {
                                uriHandler.openUri(item.item.linkUrl)
                            },
                            text = item.item.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF5A6AEC),
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    })
                }
            }
        }
        item {
            FileUsagesItemAppend(appendState = data.loadState.append)
        }
    }
}

//TODO [Parry] global usages are shown differently
@Composable
fun FileUsagesListContent(
    data: LazyPagingItems<FileUsagesResponse.FileUsage>,
    state: LazyListState
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(state = state) {
        items(data) {
            it?.let { item ->
                ListItem(leadingContent = {
                    Text(text = "•", fontWeight = FontWeight.Bold)
                }, headlineContent = {
                    Text(
                        modifier = Modifier.clickable {
                            //TODO: see if API can give us the link
                            val link = "https://commons.wikimedia.org/wiki/${item.title}"
                            uriHandler.openUri(link)
                        },
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color(0xFF5A6AEC),
                            textDecoration = TextDecoration.Underline
                        )
                    )
                })
            }
        }
        item {
            FileUsagesItemAppend(appendState = data.loadState.append)
        }
    }
}

@Composable
fun <T : Any> FileUsages(
    refreshLoadState: LoadState,
    pagingItems: LazyPagingItems<T>,
    usagesType: ListContentType,
    state: LazyListState
) {
    when (refreshLoadState) {
        LoadState.Loading -> CircularProgressIndicator()

        is LoadState.NotLoading -> {
            when (usagesType) {
                ListContentType.Commons -> FileUsagesListContent(
                    pagingItems as LazyPagingItems<FileUsagesResponse.FileUsage>,
                    state
                )

                ListContentType.OtherWikis -> GlobalUsagesListContent(
                    pagingItems as LazyPagingItems<UiModel>,
                    state
                )
            }
        }

        is LoadState.Error -> {
            RefreshErrorItem(
                errorState = refreshLoadState,
                onRetry = { pagingItems.retry() }
            )
        }
    }
}

@Composable
fun FileUsagesItemAppend(
    appendState: LoadState
) {
    when (appendState) {

        LoadState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        is LoadState.NotLoading -> {
            // TODO: maybe need to show something when reached end
        }

        is LoadState.Error -> Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Error occurred while loading"
        )
    }
}

@Composable
fun RefreshErrorItem(
    errorState: LoadState.Error,
    onRetry: () -> Unit
) {

    if (errorState.error is NoContributionsError) {
        Text(
            text = errorState.error.message!!,
            style = MaterialTheme.typography.headlineSmall
        )
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.headlineSmall
            )
            Button(onClick = onRetry) {
                Text("Try again")
            }
        }
    }

}

enum class ListContentType(val navTitle: String) {
    Commons(navTitle = "COMMONS"),
    OtherWikis(navTitle = "OTHER WIKIS")
}

sealed class UiModel {
    data class HeaderModel(val group: String) : UiModel()
    data class ItemModel(val item: GlobalUsageItem) : UiModel()
}

data class GlobalUsageItem(val group: String, val title: String, val linkUrl: String)
