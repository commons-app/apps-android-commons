package fr.free.nrw.commons.bookmarks.category

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.android.support.DaggerFragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryDetailsActivity
import javax.inject.Inject


class BookmarkCategoriesFragment : DaggerFragment() {

    @Inject
    lateinit var bookmarkCategoriesDao: BookmarkCategoriesDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    val listOfBookmarks by bookmarkCategoriesDao.getAllCategories()
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            if (listOfBookmarks.isEmpty()) {
                                Text(text = stringResource(R.string.bookmark_empty))
                            } else {
                                LazyColumn {
                                    items(items = listOfBookmarks) { bookmarkItem ->
                                        CategoryItem(
                                            categoryName = bookmarkItem.categoryName,
                                            onClick = {
                                                val categoryDetailsIntent = Intent(
                                                    requireContext(),
                                                    CategoryDetailsActivity::class.java
                                                ).putExtra("categoryName", it)
                                                startActivity(categoryDetailsIntent)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun CategoryItem(
        modifier: Modifier = Modifier,
        onClick: (String) -> Unit,
        categoryName: String
    ) {
        Row(modifier = modifier.clickable {
            onClick(categoryName)
        }) {
            ListItem(
                leadingContent = {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        painter = painterResource(R.drawable.commons),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(text = categoryName)
                }
            )
        }
    }

    @Preview
    @Composable
    private fun CategoryItemPreview() {
        CategoryItem(onClick = {}, categoryName = "Test Category")
    }
}
