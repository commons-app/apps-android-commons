package fr.free.nrw.commons.nearby.fragments

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.Place
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class NearbyParentFragmentHelpersTest {
    @Test
    fun isZeroLatLngOnlyForOrigin() {
        assertTrue(NearbyParentFragment.isZeroLatLng(LatLng(0.0, 0.0, 0f)))
        assertFalse(NearbyParentFragment.isZeroLatLng(LatLng(0.0, 1.0, 0f)))
        assertFalse(NearbyParentFragment.isZeroLatLng(LatLng(1.0, 0.0, 0f)))
        assertFalse(NearbyParentFragment.isZeroLatLng(null))
    }

    @Test
    fun matchesSelectedLabelsWhenPlaceHasSelectedLabel() {
        val group = markerPlaceGroup(Label.BANK, isBookmarked = false)
        assertTrue(NearbyParentFragment.matchesSelectedLabels(group, listOf(Label.BANK)))
        assertFalse(NearbyParentFragment.matchesSelectedLabels(group, listOf(Label.HOUSE)))
    }

    @Test
    fun matchesSelectedLabelsWhenBookmarksSelectedAndPlaceBookmarked() {
        val bookmarked = markerPlaceGroup(Label.BANK, isBookmarked = true)
        val notBookmarked = markerPlaceGroup(Label.BANK, isBookmarked = false)
        assertTrue(NearbyParentFragment.matchesSelectedLabels(bookmarked, listOf(Label.BOOKMARKS)))
        assertFalse(
            NearbyParentFragment.matchesSelectedLabels(notBookmarked, listOf(Label.BOOKMARKS)),
        )
    }

    private fun markerPlaceGroup(
        label: Label,
        isBookmarked: Boolean,
    ): MarkerPlaceGroup {
        val place = mock<Place>()
        whenever(place.label).thenReturn(label)
        return MarkerPlaceGroup(isBookmarked, place)
    }
}
