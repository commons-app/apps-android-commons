package fr.free.nrw.commons.profile.achievements

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for [Achievements.notRevertPercentage]. */
class AchievementsTest {
    @Test
    fun notRevertPercentageIsOneHundredWithNoUploads() {
        assertEquals(100, Achievements().notRevertPercentage)
    }

    @Test
    fun notRevertPercentageUsesUploadedMinusDeleted() {
        assertEquals(
            90,
            Achievements(imagesUploaded = 10, revertCount = 1).notRevertPercentage,
        )
    }

    @Test
    fun notRevertPercentageIsZeroWhenAllUploadsWereDeleted() {
        assertEquals(
            0,
            Achievements(imagesUploaded = 5, revertCount = 5).notRevertPercentage,
        )
    }

    /**
     * Deleted uploads can exceed the upload count when the two values come from different
     * sources (issue #7378). The displayed rate must stay in 0..100.
     */
    @Test
    fun notRevertPercentageIsZeroWhenDeletedExceedsUploaded() {
        assertEquals(
            0,
            Achievements(imagesUploaded = 1, revertCount = 29).notRevertPercentage,
        )
    }
}
