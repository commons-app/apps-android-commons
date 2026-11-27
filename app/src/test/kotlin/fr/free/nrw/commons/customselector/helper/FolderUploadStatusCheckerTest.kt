package fr.free.nrw.commons.customselector.helper

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.customselector.database.NotForUploadStatus
import fr.free.nrw.commons.customselector.database.UploadedStatus
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.db.AppDatabase
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.CustomSelectorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Checks which folders of the custom selector are shown as handled. Uses real picture files and
 * the real upload status tables in an in-memory database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class FolderUploadStatusCheckerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var fileUtilsWrapper: FileUtilsWrapper
    private lateinit var checker: FolderUploadStatusChecker

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        fileUtilsWrapper = spy(FileUtilsWrapper(context))
        checker =
            FolderUploadStatusChecker(
                context,
                database.UploadedStatusDao(),
                database.NotForUploadStatusDao(),
                fileUtilsWrapper,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private var nextId = 1L

    /** Writes a picture with its own content, so that every picture has its own SHA1. */
    private fun picture(
        bucketId: Long = 1,
        content: String = "picture $nextId",
    ): Image {
        val id = nextId++
        val file = File(temporaryFolder.root, "IMG_$id.jpg").apply { writeText(content) }
        return Image(id, file.name, Uri.fromFile(file), file.path, bucketId, "Bucket $bucketId", date = "$id")
    }

    private fun folder(
        vararg images: Image,
        bucketId: Long = images.firstOrNull()?.bucketId ?: 1,
    ) = Folder(bucketId, "Bucket $bucketId", ArrayList(images.toList()))

    private fun sha1(image: Image): String =
        runBlocking {
            CustomSelectorUtils.getImageSHA1(
                image.uri,
                Dispatchers.IO,
                FileUtilsWrapper(context),
                context.contentResolver,
            )
        }

    /** Stores the result the upload worker or the picture grid saves for an uploaded picture. */
    private fun markUploaded(
        image: Image,
        imageResult: Boolean = true,
        modifiedImageResult: Boolean = false,
    ) = runBlocking {
        database.UploadedStatusDao().insertUploaded(
            UploadedStatus(sha1(image), "modified ${image.id}", imageResult, modifiedImageResult),
        )
    }

    private fun markNotForUpload(image: Image) =
        runBlocking {
            database.NotForUploadStatusDao().insert(NotForUploadStatus(sha1(image)))
        }

    private fun isHandled(folder: Folder) = runBlocking { checker.isFolderHandled(folder, Dispatchers.IO) }

    @Test
    fun `folder whose pictures were all uploaded is handled`() {
        val images = arrayOf(picture(), picture(), picture())
        images.forEach { markUploaded(it) }

        assertTrue(isHandled(folder(*images)))
    }

    @Test
    fun `folder with one picture that was not uploaded is not handled`() {
        val uploaded = picture()
        val notUploaded = picture()
        markUploaded(uploaded)

        assertFalse(isHandled(folder(uploaded, notUploaded)))
    }

    @Test
    fun `pictures marked as not for upload count as handled`() {
        val uploaded = picture()
        val notForUpload = picture()
        markUploaded(uploaded)
        markNotForUpload(notForUpload)

        assertTrue(isHandled(folder(uploaded, notForUpload)))
    }

    @Test
    fun `picture uploaded after its EXIF data was changed counts as uploaded`() {
        val image = picture()
        // The upload worker stores this when the uploaded file differs from the original
        markUploaded(image, imageResult = false, modifiedImageResult = true)

        assertTrue(isHandled(folder(image)))
    }

    @Test
    fun `picture that Commons does not have yet is not handled`() {
        val image = picture()
        // The picture grid stores this when Commons answered that the picture is not there
        markUploaded(image, imageResult = false, modifiedImageResult = false)

        assertFalse(isHandled(folder(image)))
    }

    @Test
    fun `new picture in a handled folder makes it not handled until it is uploaded`() {
        val first = picture()
        val second = picture()
        markUploaded(first)
        markUploaded(second)
        assertTrue(isHandled(folder(first, second)))

        val newPicture = picture()
        val folderWithNewPicture = folder(newPicture, first, second)
        assertFalse(isHandled(folderWithNewPicture))

        markUploaded(newPicture)
        assertTrue(isHandled(folderWithNewPicture))
    }

    @Test
    fun `picture that was unmarked as not for upload makes the folder not handled again`() {
        val image = picture()
        markNotForUpload(image)
        assertTrue(isHandled(folder(image)))

        runBlocking { database.NotForUploadStatusDao().deleteNotForUploadWithImageSHA1(sha1(image)) }

        assertFalse(isHandled(folder(image)))
    }

    @Test
    fun `copy of an uploaded picture in another folder is handled too`() {
        val original = picture(bucketId = 1, content = "same picture")
        val copy = picture(bucketId = 2, content = "same picture")
        markUploaded(original)

        assertTrue(isHandled(folder(copy)))
    }

    @Test
    fun `picture that cannot be read is not handled`() {
        val image = picture()
        markUploaded(image)
        File(image.path).delete()

        assertFalse(isHandled(folder(image)))
    }

    @Test
    fun `empty folder is not handled`() {
        assertFalse(isHandled(Folder(1, "Empty")))
    }

    @Test
    fun `only handled folders are returned`() {
        val done = picture(bucketId = 1)
        val notDone = picture(bucketId = 2)
        val notForUpload = picture(bucketId = 3)
        markUploaded(done)
        markNotForUpload(notForUpload)

        val handled =
            runBlocking {
                checker.findHandledFolders(listOf(folder(done), folder(notDone), folder(notForUpload)), Dispatchers.IO)
            }

        assertEquals(setOf(1L, 3L), handled)
    }

    @Test
    fun `check stops at the first picture that is not handled`() {
        val notUploaded = picture()
        val uploaded = picture()
        markUploaded(uploaded)

        assertFalse(isHandled(folder(notUploaded, uploaded, picture(), picture())))

        // Only the first picture had to be read
        verify(fileUtilsWrapper, times(1)).getSHA1(any())
    }

    @Test
    fun `pictures are only read once when the folders are checked again`() {
        val images = arrayOf(picture(), picture())
        images.forEach { markUploaded(it) }
        val folder = folder(*images)

        assertTrue(isHandled(folder))
        assertTrue(isHandled(folder))

        verify(fileUtilsWrapper, times(2)).getSHA1(any())
    }
}
