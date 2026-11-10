package fr.free.nrw.commons.contributions

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ContributionsListWikipediaHelperTest {
    @Test
    fun addImageToWikipediaFillsLanguageNameInDialog() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = mock<Fragment>()
        whenever(fragment.activity).thenReturn(activity)
        val helper = ContributionsListWikipediaHelper(fragment) { null }

        helper.addImageToWikipedia(null)

        val message = Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog()).message.toString()
        val expected =
            activity.getString(
                R.string.add_picture_to_wikipedia_article_desc,
                Locale.getDefault().displayLanguage,
            )
        assertEquals(expected, message)
        assertFalse(message.contains("%1\$s"))
    }
}
