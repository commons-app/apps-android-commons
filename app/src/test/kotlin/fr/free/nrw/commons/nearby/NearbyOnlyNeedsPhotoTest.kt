package fr.free.nrw.commons.nearby

import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import org.junit.Assert.assertEquals
import org.junit.Test

class NearbyOnlyNeedsPhotoTest {
    private fun groupWithPic(pic: String?) = MarkerPlaceGroup(false, Place().apply { this.pic = pic })

    private val green = groupWithPic("https://commons.wikimedia.org/wiki/File:Example.jpg")
    private val red = groupWithPic("")
    private val notLoadedYet = groupWithPic(null)
    private val blank = groupWithPic("  ")

    @Test
    fun `all pins are shown when the toggle is off`() {
        val groups = listOf(green, red, notLoadedYet, blank)
        assertEquals(groups, NearbyParentFragment.placesToShow(groups, onlyNeedsPhoto = false))
    }

    @Test
    fun `pins with a picture are hidden when the toggle is on`() {
        val groups = listOf(green, red, notLoadedYet, blank)
        assertEquals(
            listOf(red, notLoadedYet, blank),
            NearbyParentFragment.placesToShow(groups, onlyNeedsPhoto = true),
        )
    }

    @Test
    fun `toggle is off by default`() {
        assertEquals(false, NearbyFilterState.getInstance().isNeedPhotoSelected)
    }
}
