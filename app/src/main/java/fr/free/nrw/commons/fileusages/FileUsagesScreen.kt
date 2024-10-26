package fr.free.nrw.commons.fileusages

import android.graphics.Paint.Align
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun FileUsagesScreen(modifier: Modifier = Modifier, viewModel: FileUsagesViewModel) {
    // temp screen state
    // 0 -> Commons File usages
    // 1 -> Other wikis usages
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


        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreenIndex) {
                0 -> {
                    if (screenState.isCommonsScreenLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        CommonsListContent(
                            screenState.commonsFileUsagesList
                        )
                    }
                }

                1 -> {
                    if (screenState.isOtherWikisScreenLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        OtherWikisListContent(
                            screenState.otherWikisUsagesList
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommonsListContent(data: List<FileUsage>?) {
    println("list received $data")
    data?.let {
        println(it.size)
        LazyColumn {
            items(it) { fileUsageItem ->
                ListItem(leadingContent = {
                    Text(text = "*")
                }, headlineContent = {
                    Text(text = fileUsageItem.title)
                })
            }
        }
    }
}

@Composable
fun OtherWikisListContent(data: List<GlobalFileUsage>?) {
    data?.let {
        LazyColumn {
            items(it) { globalFileUsageItem ->
                ListItem(leadingContent = {
                    Text(text = "*")
                }, headlineContent = {
                    Text(text = globalFileUsageItem.wiki)
                })
            }
        }
    }
}
