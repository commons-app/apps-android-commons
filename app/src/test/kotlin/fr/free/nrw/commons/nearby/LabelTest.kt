package fr.free.nrw.commons.nearby

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.R
import org.junit.Before
import org.junit.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LabelTest {
    private lateinit var label: Label
    private lateinit var context: Context

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Label.init(context)
    }

    /**
     * test if a random label icon matches with intended one
     */
    @Test
    fun testLabelIcon() {
        label = Label.fromText("Q16970")
        assertThat(label.icon, equalTo(R.drawable.round_icon_church))
    }

    /**
     * test if label is not found in label set, unknown icon is used
     */
    @Test
    fun testNullLabelIcon() {
        val nullLabel: Label = Label.fromText("a random text not exist in label texts")
        assertThat(nullLabel.icon, equalTo(R.drawable.round_icon_unknown))
    }
}
