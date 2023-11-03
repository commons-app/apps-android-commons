package fr.free.nrw.commons.upload

import android.content.Context
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import media
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

class UploadModelUnitTest {

    private lateinit var uploadModel: UploadModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        uploadModel = UploadModel(
            listOf(),
            mock(JsonKvStore::class.java),
            mapOf(),
            mock(Context::class.java),
            mock(SessionManager::class.java),
            mock(FileProcessor::class.java),
            mock(ImageProcessingService::class.java)
        )
    }

    @Test
    fun `Test onDepictItemClicked when DepictedItem is selected`(){
        uploadModel.onDepictItemClicked(
            DepictedItem(
            "Test",
                "Test",
                "test",
                listOf(),
                listOf(),
                true,
                "depictionId"
            ), media(filename = "File:Example.jpg"))
    }

    @Test
    fun `Test onDepictItemClicked when DepictedItem is not selected`(){
        uploadModel.onDepictItemClicked(
            DepictedItem(
                "Test",
                "Test",
                "test",
                listOf(),
                listOf(),
                false,
                "depictionId"
            ), media(filename = "File:Example.jpg")
        )
    }

    @Test
    fun `Test onDepictItemClicked when DepictedItem is not selected and not included in media`(){
        uploadModel.onDepictItemClicked(
            DepictedItem(
                "Test",
                "Test",
                "test",
                listOf(),
                listOf(),
                false,
                "id"
            ), media(filename = "File:Example.jpg")
        )
    }

    @Test
    fun `Test onDepictItemClicked when media is null and DepictedItem is not selected`(){
        uploadModel.onDepictItemClicked(
            DepictedItem(
                "Test",
                "Test",
                "test",
                listOf(),
                listOf(),
                false,
                "id"
            ), null)
    }

    @Test
    fun `Test onDepictItemClicked when media is not null and DepictedItem is selected`(){
        uploadModel.onDepictItemClicked(
            DepictedItem(
                "Test",
                "Test",
                "test",
                listOf(),
                listOf(),
                true,
                "id"
            ), media(filename = "File:Example.jpg"))
    }

    @Test
    fun `Test onDepictItemClicked when media is null and DepictedItem is selected`(){
        uploadModel.onDepictItemClicked(
            DepictedItem(
                "Test",
                "Test",
                "test",
                listOf(),
                listOf(),
                true,
                "id"
            ), null)
    }

    @Test
    fun testGetSelectedExistingDepictions(){
        uploadModel.selectedExistingDepictions
    }

    @Test
    fun testSetSelectedExistingDepictions(){
        uploadModel.selectedExistingDepictions = listOf("")
    }
}