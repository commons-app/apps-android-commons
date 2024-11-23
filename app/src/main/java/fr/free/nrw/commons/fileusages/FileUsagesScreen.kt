package fr.free.nrw.commons.fileusages

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import fr.free.nrw.commons.R
import fr.free.nrw.commons.fileusages.model.FileUsagesResponse
import fr.free.nrw.commons.fileusages.model.NoContributionsError

@Composable
fun FileUsagesScreen(modifier: Modifier = Modifier, viewModel: FileUsagesViewModel) {
    val fileUsagesLazyPagingItems =
        viewModel.fileUsagesPagingData.collectAsLazyPagingItems()

    val globalFileUsagesLazyPagingItems =
        viewModel.globalFileUsagesPagingData.collectAsLazyPagingItems()

    val screenState by viewModel.screenState.collectAsState()

    var currentScreenIndex by rememberSaveable { mutableStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TabRow(
            selectedTabIndex = currentScreenIndex,
            contentColor = Color.Transparent,
            containerColor = if (isSystemInDarkTheme())
                colorResource(R.color.contributionListDarkBackground)
            else colorResource(R.color.card_light_grey),
            indicator = { tabPositions ->
                if (currentScreenIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[currentScreenIndex]),
                        color = if (isSystemInDarkTheme()) colorResource(R.color.white)
                        else colorResource(R.color.primaryDarkColor)
                    )
                }
            }
        ) {
            Tab(
                selected = currentScreenIndex == 0,
                onClick = {
                    currentScreenIndex = 0
                },
                text = { Text("Commons") },
                selectedContentColor = if (isSystemInDarkTheme()) colorResource(R.color.white)
                else colorResource(R.color.primaryDarkColor),
                unselectedContentColor = if (isSystemInDarkTheme()) colorResource(R.color.white)
                else colorResource(R.color.primaryDarkColor)
            )

            Tab(selected = currentScreenIndex == 1, onClick = {
                currentScreenIndex = 1
            }, text = { Text("Other Wikis") },
                selectedContentColor = if (isSystemInDarkTheme()) colorResource(R.color.white)
                else colorResource(R.color.primaryDarkColor),
                unselectedContentColor = if (isSystemInDarkTheme()) colorResource(R.color.white)
                else colorResource(R.color.primaryDarkColor)
            )
        }


        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (currentScreenIndex) {
                0 -> {
                    FileUsages(
                        refreshLoadState = fileUsagesLazyPagingItems.loadState.refresh,
                        pagingItems = fileUsagesLazyPagingItems,
                        usagesType = 0
                    )
                }

                1 -> {
                    FileUsages(
                        refreshLoadState = globalFileUsagesLazyPagingItems.loadState.refresh,
                        pagingItems = globalFileUsagesLazyPagingItems,
                        usagesType = 1
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalUsagesListContent(data: LazyPagingItems<UiModel>) {
    LazyColumn {
        items(data) {
            it?.let { item ->
                when (item) {
                    is UiModel.HeaderModel -> ListItem(
                        headlineContent = {
                            Text(
                                text = item.group,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    )

                    is UiModel.ItemModel -> ListItem(headlineContent = {
                        Text(
                            text = item.item.title,
                            style = MaterialTheme.typography.bodyMedium
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
fun FileUsagesListContent(data: LazyPagingItems<FileUsagesResponse.FileUsage>) {
    LazyColumn {
        items(data) {
            it?.let { item ->
                ListItem(leadingContent = {
                    Text(text = "*")
                }, headlineContent = {
                    Text(text = item.title)
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
    usagesType: Int
) {
    when (refreshLoadState) {
        LoadState.Loading -> CircularProgressIndicator()

        is LoadState.NotLoading -> {
            when (usagesType) {
                0 -> FileUsagesListContent(pagingItems as LazyPagingItems<FileUsagesResponse.FileUsage>)
                1 -> GlobalUsagesListContent(pagingItems as LazyPagingItems<UiModel>)
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
            val ifLastPage = appendState.endOfPaginationReached
            if (ifLastPage) {
                Text(
                    "End Reached",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
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

sealed class UiModel {
    data class HeaderModel(val group: String) : UiModel()
    data class ItemModel(val item: GlobalUsageItem) : UiModel()
}

data class GlobalUsageItem(val group: String, val title: String)
