package fr.free.nrw.commons

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(21), application = TestCommonsApplication::class)
class MediaTest {
    @Test
    fun displayTitleShouldStripExtension() {
        val m = Media("File:Example.jpg")
        assertEquals("Example", m.displayTitle)
    }

    @Test
    fun displayTitleShouldUseSpaceForUnderscore() {
        val m = Media("File:Example 1_2.jpg")
        assertEquals("Example 1 2", m.displayTitle)
    }
}
