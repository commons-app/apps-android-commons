package fr.free.nrw.commons.upload

import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.ImageCoordinates
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import java.util.*


/**
 * The clas contains unit test cases for UploadPresenter
 */
class UploadPresenterTest {

    @Mock
    internal lateinit var repository: UploadRepository
    @Mock
    internal lateinit var view: UploadContract.View
    @Mock
    lateinit var contribution: Contribution

    @Mock
    lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var uploadableFile: UploadableFile

    @Mock
    private lateinit var anotherUploadableFile: UploadableFile

    @Mock
    private lateinit var imageCoords: ImageCoordinates
    @Mock
    private lateinit var uploadItem: UploadItem

    @InjectMocks
    lateinit var uploadPresenter: UploadPresenter

    private var uploadableFiles: ArrayList<UploadableFile> = ArrayList()
    private var uploadableItems: ArrayList<UploadItem> = ArrayList()

    /**
     * initial setup, test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        uploadPresenter.onAttachView(view)
        `when`(repository.buildContributions()).thenReturn(Observable.just(contribution))
        uploadableFiles.add(uploadableFile)
        `when`(view.uploadableFiles).thenReturn(uploadableFiles)
        `when`(uploadableFile.filePath).thenReturn("data://test")
    }

    /**
     * unit test case for method UploadPresenter.handleSubmit
     */
    @Test
    fun handleSubmitTestUserLoggedIn() {
        `when`(view.isLoggedIn).thenReturn(true)
        uploadPresenter.handleSubmit()
        verify(view).isLoggedIn
        verify(view).showProgress(true)
        verify(repository).buildContributions()
        verify(repository).buildContributions()
    }

    @Test
    fun handleSubmitImagesNoLocationWithConsecutiveNoLocationUploads() {
        `when`(imageCoords.imageCoordsExists).thenReturn(false)
        `when`(uploadItem.getGpsCoords()).thenReturn(imageCoords)
        `when`(repository.uploads).thenReturn(uploadableItems)
        uploadableItems.add(uploadItem)

        // test 1 - insufficient count
        `when`(
            defaultKvStore.getInt(UploadPresenter.COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0))
                .thenReturn(UploadPresenter.CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES_REMINDER_THRESHOLD - 1)
        uploadPresenter.handleSubmit()
        // no alert dialog expected as insufficient consecutive count
        verify(view, times(0)).showAlertDialog(ArgumentMatchers.anyInt(), ArgumentMatchers.any<Runnable>())

        // test 2 - sufficient count
        `when`(
            defaultKvStore.getInt(UploadPresenter.COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0))
            .thenReturn(UploadPresenter.CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES_REMINDER_THRESHOLD)
        uploadPresenter.handleSubmit()
        // alert dialog expected as consecutive count is at threshold
        verify(view).showAlertDialog(ArgumentMatchers.anyInt(), ArgumentMatchers.any<Runnable>())
    }

    @Test
    fun handleSubmitImagesWithLocationWithConsecutiveNoLocationUploads() {
        `when`(
            defaultKvStore.getInt(UploadPresenter.COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0))
            .thenReturn(UploadPresenter.CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES_REMINDER_THRESHOLD)
        `when`(imageCoords.imageCoordsExists).thenReturn(true)
        `when`(uploadItem.getGpsCoords()).thenReturn(imageCoords)
        `when`(repository.uploads).thenReturn(uploadableItems)
        uploadableItems.add(uploadItem)
        uploadPresenter.handleSubmit()
        // no alert dialog expected
        verify(view, times(0))
            .showAlertDialog(ArgumentMatchers.anyInt(), ArgumentMatchers.any<Runnable>())
    }

    @Test
    fun handleSubmitTestUserLoggedInAndLimitedConnectionOn() {
        `when`(
            defaultKvStore
                .getBoolean(
                    CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED,
                    false
                )).thenReturn(true)
        `when`(view.isLoggedIn).thenReturn(true)
        uploadPresenter.handleSubmit()
        verify(view).isLoggedIn
        verify(view).showProgress(true)
        verify(repository).buildContributions()
        verify(repository).buildContributions()
    }

    /**
     * unit test case for method UploadPresenter.handleSubmit
     */
    @Test
    fun handleSubmitTestUserNotLoggedIn() {
        `when`(view.isLoggedIn).thenReturn(false)
        uploadPresenter.handleSubmit()
        verify(view).isLoggedIn
        verify(view).askUserToLogIn()

    }

    private fun deletePictureBaseTest(){
        uploadableFiles.clear()
    }

    /**
     * Test which asserts If the next fragment to be shown is not one of the MediaDetailsFragment, lets hide the top card
     */
    @Test
    fun hideTopCardWhenReachedTheLastFile(){
        deletePictureBaseTest()
        uploadableFiles.add(uploadableFile)
        uploadPresenter.deletePictureAtIndex(0)
        verify(view).showHideTopCard(false)
        verify(repository).deletePicture(ArgumentMatchers.anyString())
    }

    /**
     * Test media deletion during single upload
     */
    @Test
    fun testDeleteWhenSingleUpload(){
        deletePictureBaseTest()
        uploadableFiles.add(uploadableFile)
        uploadPresenter.deletePictureAtIndex(0)
        verify(view).showHideTopCard(false)
        verify(repository).deletePicture(ArgumentMatchers.anyString())
        verify(view).showMessage(ArgumentMatchers.anyInt())//As there is only one while which we are asking for deletion, upload should be cancelled and this flow should be triggered
        verify(view).finish()
    }

    /**
     * Test media deletion during multiple upload
     */
    @Test
    fun testDeleteWhenMultipleFilesUpload(){
        deletePictureBaseTest()
        uploadableFiles.add(uploadableFile)
        uploadableFiles.add(anotherUploadableFile)
        uploadPresenter.deletePictureAtIndex(0)
        verify(view).onUploadMediaDeleted(0)
        verify(view).updateTopCardTitle()
    }
}
