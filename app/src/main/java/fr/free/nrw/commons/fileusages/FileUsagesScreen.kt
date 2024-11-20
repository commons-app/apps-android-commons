package fr.free.nrw.commons.fileusages

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items

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
        TabRow(selectedTabIndex = currentScreenIndex) {
            Tab(selected = currentScreenIndex == 0, onClick = {
                currentScreenIndex = 0
            }, text = { Text("Commons") })

            Tab(selected = currentScreenIndex == 1, onClick = {
                currentScreenIndex = 1
            }, text = { Text("Other Wikis") })
        }


        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (currentScreenIndex) {
                0 -> {
                    when (fileUsagesLazyPagingItems.loadState.refresh) {
                        is LoadState.Error -> {
                            val error =
                                (fileUsagesLazyPagingItems.loadState.refresh as LoadState.Error).error

                            if (error is NoContributionsError) {
                                Text(
                                    text = error.message!!,
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
                                    Button(onClick = { fileUsagesLazyPagingItems.retry() }) {
                                        Text("Try again")
                                    }
                                }
                            }

                        }

                        LoadState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        is LoadState.NotLoading -> {
                            FileUsagesListContent(fileUsagesLazyPagingItems)
                        }
                    }
                }

                1 -> {
//                    if (screenState.isOtherWikisScreenLoading) {
//                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
//                    } else {
//                        OtherWikisListContent(
//                            screenState.otherWikisUsagesList
//                        )
//                    }

                    when (globalFileUsagesLazyPagingItems.loadState.refresh) {
                        is LoadState.Error -> {
                            val error =
                                (globalFileUsagesLazyPagingItems.loadState.refresh as LoadState.Error).error

                            if (error is NoContributionsError) {
                                Text(
                                    text = error.message!!,
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
                                    Button(onClick = { globalFileUsagesLazyPagingItems.retry() }) {
                                        Text("Try again")
                                    }
                                }
                            }
                        }

                        LoadState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        is LoadState.NotLoading -> {
                            GlobalUsagesListContent(globalFileUsagesLazyPagingItems)
                        }
                    }
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
                    is UiModel.HeaderModel -> Text(
                        text = item.group,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    is UiModel.ItemModel -> Text(
                        text = item.item.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            when (data.loadState.append) {
                is LoadState.Error -> Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Error occurred while loading"
                )

                LoadState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                is LoadState.NotLoading -> {
                    val ifLastPage = data.loadState.append.endOfPaginationReached
                    if (ifLastPage) {
                        Text(
                            "End Reached",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
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
            when (data.loadState.append) {
                is LoadState.Error -> Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Error occurred while loading"
                )

                LoadState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                is LoadState.NotLoading -> {
                    val ifLastPage = data.loadState.append.endOfPaginationReached
                    if (ifLastPage) {
                        Text(
                            "End Reached",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

//@Composable
//fun OtherWikisListContent(data: List<GlobalFileUsage>?) {
//    data?.let {
//        LazyColumn {
//            items(data.size) { index ->
//                val globalFileUsageItem = data[index]
//                ListItem(leadingContent = {
//                    Text(text = "*")
//                }, headlineContent = {
//                    Text(text = globalFileUsageItem.wiki)
//                })
//            }
//        }
//    }
//}


sealed class UiModel {
    data class HeaderModel(val group: String) : UiModel()
    data class ItemModel(val item: GlobalUsageItem) : UiModel()
}

data class GlobalUsageItem(val group: String, val title: String)