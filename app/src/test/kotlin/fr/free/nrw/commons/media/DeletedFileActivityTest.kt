package fr.free.nrw.commons.media

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.databinding.ActivityDeletedFileBinding
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class DeletedFileActivityTest {
    @Test
    fun textIsReadableInLightTheme() {
        val (context, binding) = inflate(R.style.LightAppTheme)
        assertContrast(binding.deletedFileExplanation, themeColor(context, R.attr.mainBackground))

        val buttonBackground = themeColor(context, androidx.appcompat.R.attr.colorButtonNormal)
        assertContrast(binding.viewFilePage, buttonBackground)
        assertContrast(binding.viewDeletionDiscussion, buttonBackground)
        assertContrast(binding.viewPolicies, buttonBackground)
    }

    @Test
    fun explanationIsReadableInDarkTheme() {
        val (context, binding) = inflate(R.style.DarkAppTheme)
        assertContrast(binding.deletedFileExplanation, themeColor(context, R.attr.mainBackground))
    }

    private fun inflate(theme: Int): Pair<Context, ActivityDeletedFileBinding> {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), theme)
        return context to ActivityDeletedFileBinding.inflate(LayoutInflater.from(context))
    }

    private fun themeColor(
        context: Context,
        attr: Int,
    ): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(attr, value, true)
        return value.data
    }

    // 4.5:1 is the WCAG AA minimum for normal text
    private fun assertContrast(
        view: TextView,
        background: Int,
    ) {
        val contrast = ColorUtils.calculateContrast(view.currentTextColor, background)
        assertTrue("${view.text} contrast is $contrast", contrast >= 4.5)
    }
}
