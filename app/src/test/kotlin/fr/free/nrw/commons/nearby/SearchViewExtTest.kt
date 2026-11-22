package fr.free.nrw.commons.nearby

import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestCommonsApplication::class)
class SearchViewExtTest {
    @Test
    fun `search icon focuses query input`() {
        val searchView = SearchView(ApplicationProvider.getApplicationContext())
        searchView.isIconified = false
        searchView.enableSearchIconFocus(R.string.nearby_filter_search)

        val icon = searchView.findViewById<View>(androidx.appcompat.R.id.search_mag_icon)
        val input = searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)

        assertTrue(icon.isClickable)
        assertEquals(
            ContextCompat.getColor(searchView.context, R.color.primaryDarkColor),
            input.currentTextColor,
        )
        assertEquals(
            searchView.context.getString(R.string.nearby_filter_search),
            icon.contentDescription,
        )

        icon.performClick()

        assertTrue(input.isFocused)
    }
}
