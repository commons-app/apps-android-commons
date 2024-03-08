package fr.free.nrw.commons

import media
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class MediaTest {
    @Test
    fun displayTitleShouldStripExtension() {
        val m = media(filename = "File:Example.jpg")
        assertThat("Example", equalTo( m.displayTitle))
    }

    @Test
    fun displayTitleShouldUseSpaceForUnderscore() {
        val m = media(filename = "File:Example 1_2.jpg")
        assertThat("Example 1 2", equalTo( m.displayTitle))
    }
}


