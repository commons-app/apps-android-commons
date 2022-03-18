package fr.free.nrw.commons.upload

import android.content.Context
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

class UploadModelUnitTest {

    private lateinit var uploadModel: UploadModel

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
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
    fun testOnDepictItemClicked(){
        uploadModel.onDepictItemClicked(mock(DepictedItem::class.java), mock(Media::class.java))
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