package fr.free.nrw.commons.contributions

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.upload.ImageCoordinates
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadMediaDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks how the place where a picture was taken ends up in a [Contribution], next to the
 * depictions and the GPS coordinates that were already part of it.
 *
 * Robolectric is needed because the upload item looks up the file extension of its mime type.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class ContributionLocationOfCreationTest {
    private val sessionManager: SessionManager = mock()

    private fun uploadItem(decimalCoords: String? = null): UploadItem {
        val coordinates: ImageCoordinates? =
            decimalCoords?.let { coords ->
                mock<ImageCoordinates>().also { whenever(it.decimalCoords).thenReturn(coords) }
            }
        return UploadItem(
            mediaUri = null,
            mimeType = "image/jpeg",
            gpsCoords = coordinates,
            place = null,
            createdTimestamp = -1L,
            createdTimestampSource = null,
            contentUri = null,
            fileCreatedDateString = null,
        ).apply {
            uploadMediaDetails = mutableListOf(UploadMediaDetail("en", "A car engine", "Engine"))
        }
    }

    private fun contribution(
        item: UploadItem,
        depictions: List<String>,
        location: String?,
    ) = Contribution(
        item,
        sessionManager,
        depictions.map { depictedItem(id = it, name = it) },
        emptyList(),
        "sha1",
        location?.let { depictedItem(id = it, name = it) },
    )

    @Test
    fun `place used only as shooting location is not a depiction`() {
        val result = contribution(uploadItem(), depictions = listOf("Q_ENGINE"), location = "Q12439")

        assertEquals(listOf("Q_ENGINE"), result.depictedItems.map { it.id })
        assertEquals("Q12439", result.locationOfCreation?.id)
    }

    @Test
    fun `same item can be both a depiction and the shooting location`() {
        val result = contribution(uploadItem(), depictions = listOf("Q12439"), location = "Q12439")

        assertEquals(listOf("Q12439"), result.depictedItems.map { it.id })
        assertEquals("Q12439", result.locationOfCreation?.id)
    }

    @Test
    fun `no location selected keeps the previous behaviour`() {
        val result = contribution(uploadItem(), depictions = listOf("Q_ENGINE"), location = null)

        assertEquals(listOf("Q_ENGINE"), result.depictedItems.map { it.id })
        assertNull(result.locationOfCreation)
    }

    @Test
    fun `choosing a shooting location does not change the GPS coordinates`() {
        val withLocation = contribution(uploadItem("42.33|-83.04"), listOf("Q1"), "Q12439")
        val without = contribution(uploadItem("42.33|-83.04"), listOf("Q1"), null)

        assertEquals("42.33|-83.04", withLocation.decimalCoords)
        assertEquals(without.decimalCoords, withLocation.decimalCoords)
    }

    @Test
    fun `contribution without GPS coordinates stays without them when a location is chosen`() {
        assertNull(contribution(uploadItem(), listOf("Q1"), "Q12439").decimalCoords)
    }
}
