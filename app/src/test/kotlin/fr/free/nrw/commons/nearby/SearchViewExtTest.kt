package fr.free.nrw.commons.nearby

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestCommonsApplication::class)
class SearchViewExtTest {
    @Test
    fun `search icon focuses query input and hint is visible in light theme`() {
        val searchView = searchView(R.style.LightAppTheme)
        val context = searchView.context
        val icon = searchView.findViewById<View>(androidx.appcompat.R.id.search_mag_icon)
        val input = searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)

        assertTrue(icon.isClickable)
        assertEquals(
            context.getString(R.string.nearby_filter_search),
            icon.contentDescription,
        )
        assertEquals(
            context.getString(R.string.nearby_search_hint),
            input.hint.toString().trim(),
        )
        assertEquals(themeColor(context, R.attr.nearbySearchTextColor), input.currentTextColor)

        icon.performClick()

        assertTrue(input.isFocused)
    }

    @Test
    fun `search field follows the dark theme`() {
        val lightView = searchView(R.style.LightAppTheme)
        val darkView = searchView(R.style.DarkAppTheme)
        val lightInput = lightView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
        val darkInput = darkView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)

        assertEquals(themeColor(darkView.context, R.attr.nearbySearchTextColor), darkInput.currentTextColor)
        assertNotEquals(lightInput.currentTextColor, darkInput.currentTextColor)
        assertEquals(
            themeColor(darkView.context, R.attr.nearbySearchBackground),
            background(darkView),
        )
        assertEquals(
            themeColor(lightView.context, R.attr.nearbySearchBackground),
            background(lightView),
        )
    }

    private fun searchView(theme: Int): SearchView {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        activity.setTheme(theme)
        val view = LayoutInflater.from(activity).inflate(R.layout.filter_search_view_layout, null)
        return view.findViewById<SearchView>(R.id.search_view).apply {
            isIconified = false
            enableSearchIconFocus(R.string.nearby_filter_search)
        }
    }

    private fun background(searchView: SearchView): Int {
        val drawable =
            ContextCompat.getDrawable(
                searchView.context,
                R.drawable.nearby_search_view_background,
            ) as GradientDrawable
        return drawable.getColor()!!.defaultColor
    }

    private fun themeColor(
        context: Context,
        attr: Int,
    ): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(attr, value, true)
        return value.data
    }
}
