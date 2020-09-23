package fr.free.nrw.commons.nearby

import fr.free.nrw.commons.R
import fr.free.nrw.commons.R.*
import org.junit.Before
import org.junit.Test

class LabelTest {
    private lateinit var label: Label

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        label = Label.fromText("Q44539")
    }

    /**
     * test if a random label icon matches with intended one
     */
    @Test
    fun testLabelIcon() {
        assert(label.icon.equals(R.drawable.round_icon_church))
    }

    /**
     * test if label is not found in label set, unknown icon is used
     */
    @Test
    fun testNullLabelIcon() {
        var nullLabel: Label = Label.fromText("a random text not exist in label texts")
        assert(nullLabel.icon.equals(R.drawable.round_icon_unknown))
    }

}
