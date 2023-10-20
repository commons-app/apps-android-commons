package fr.free.nrw.commons.profile.achievements

import fr.free.nrw.commons.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * Test for the Level Controller Class for Achievements
 */
class LevelControllerTest {

    @Mock
    private lateinit var levelController: LevelController

    private val IMAGES_UPLOADED_SAMPLE_VALUE = 0

    private val UNIQUE_IMAGES_USED_SAMPLE_VALUE = 0

    private val NON_REVERT_RATE_SAMPLE_VALUE = 0

    private lateinit var levelInfo: LevelController.LevelInfo

    /**
     * Setups the mock objects for the tests
     */
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        levelController = LevelController()
        levelInfo = LevelController.LevelInfo.from(
            IMAGES_UPLOADED_SAMPLE_VALUE,
            UNIQUE_IMAGES_USED_SAMPLE_VALUE,
            NON_REVERT_RATE_SAMPLE_VALUE
        )
    }

    /**
     * Tests if Level number for sample values is equal to actual value or not
     */
    @Test
    fun testLevelNumber() {
        assertEquals(levelInfo.levelNumber, 1)
    }

    /**
     * Tests if Level Style for Level with sample values is equal to actual value or not
     */
    @Test
    fun testLevelStyle() {
        assertEquals(levelInfo.levelStyle, R.style.LevelOne)
    }

    /**
     * Tests if Maximum Unique Images for Level with sample values is equal to actual value
     * or not
     */
    @Test
    fun testMaxUniqueImages() {
        assertEquals(levelInfo.maxUniqueImages, 5)
    }

    /**
     * Tests if Maximum Upload Count for Level with sample values is equal to actual value
     * or not
     */
    @Test
    fun testMaxUploadCount() {
        assertEquals(levelInfo.maxUploadCount, 20)
    }

    /**
     * Tests if the Minimum Non Revert Percentage for Level with sample values is equal to
     * actual value or not
     */
    @Test
    fun testMinNonRevertPercentage() {
        assertEquals(levelInfo.minNonRevertPercentage, 85)
    }
}