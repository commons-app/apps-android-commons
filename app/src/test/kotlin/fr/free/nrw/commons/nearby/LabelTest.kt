package fr.free.nrw.commons.nearby

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.R
import org.junit.After
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
        var nullLabel: Label = Label.fromText("a random text not exist in label texts")
        assertThat(nullLabel.icon, equalTo(R.drawable.round_icon_unknown))
    }

    @After
    fun tearDown() {
        Label.setLocalizedNames(emptyMap())
    }

    /**
     * test that each place type uses the first Q-ID of its array as its Wikidata item
     */
    @Test
    fun testQid() {
        assertThat(Label.SCHOOL.qid, equalTo("Q3914"))
        assertThat(Label.BOOKMARKS.qid, equalTo(null))
        assertThat(Label.getQids().contains("Q22698"), equalTo(true))
    }

    /**
     * test that a readable English name is shown until localized names are loaded
     */
    @Test
    fun testDisplayNameFallback() {
        assertThat(Label.GAS_STATION.getDisplayName(context), equalTo("Gas station"))
        assertThat(Label.BOOKMARKS.getDisplayName(context), equalTo(context.getString(R.string.bookmarks)))
    }

    /**
     * test that the localized name is used and capitalized once loaded
     */
    @Test
    fun testLocalizedDisplayName() {
        Label.setLocalizedNames(mapOf("Q3914" to "école", "Q22698" to ""))
        assertThat(Label.SCHOOL.getDisplayName(context), equalTo("École"))
        // empty name (no label in any language) falls back to the English name
        assertThat(Label.PARK.getDisplayName(context), equalTo("Park"))
    }
}
