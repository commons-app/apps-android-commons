package fr.free.nrw.commons.bookmarks.category

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.android.support.DaggerFragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryDetailsActivity
import javax.inject.Inject

/**
 * Tab fragment to show list of bookmarked Categories
 */
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
                MaterialTheme(
                    colorScheme = if (isSystemInDarkTheme()) darkColorScheme(
                        primary = colorResource(R.color.primaryDarkColor),
                        surface = colorResource(R.color.main_background_dark),
                        background = colorResource(R.color.main_background_dark)
                    ) else lightColorScheme(
                        primary = colorResource(R.color.primaryColor),
                        surface = colorResource(R.color.main_background_light),
                        background = colorResource(R.color.main_background_light)
                    )
                ) {
                    val listOfBookmarks by bookmarkCategoriesDao.getAllCategories()
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            if (listOfBookmarks.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.bookmark_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSystemInDarkTheme()) Color(0xB3FFFFFF)
                                    else Color(
                                        0x8A000000
                                    )
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                    Image(
                        modifier = Modifier.size(48.dp),
                        painter = painterResource(R.drawable.commons),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(
                        text = categoryName,
                        maxLines = 2,
                        color = if (isSystemInDarkTheme()) Color.White else Color.Black,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    }

    @Preview
    @Composable
    private fun CategoryItemPreview() {
        CategoryItem(
            onClick = {},
            categoryName = "Test Category"
        )
    }
}
