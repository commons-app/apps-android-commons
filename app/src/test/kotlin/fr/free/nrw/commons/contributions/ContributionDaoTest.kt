package fr.free.nrw.commons.contributions

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.RemoteException
import com.nhaarman.mockito_kotlin.*
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.contributions.Contribution.*
import fr.free.nrw.commons.contributions.ContributionDao.Table
import fr.free.nrw.commons.contributions.ContributionsContentProvider.BASE_URI
import fr.free.nrw.commons.contributions.ContributionsContentProvider.uriForId
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = [21], application = TestCommonsApplication::class)
class ContributionDaoTest {
    private val localUri = "http://example.com/"
    private val client: ContentProviderClient = mock()
    private val database: SQLiteDatabase = mock()
    private val captor = argumentCaptor<ContentValues>()

    private lateinit var contentUri: Uri
    private lateinit var testObject: ContributionDao

    @Before
    fun setUp() {
        contentUri = uriForId(111)
        testObject = ContributionDao { client }
    }

    @Test
    fun createTable() {
        Table.onCreate(database)
        verify(database).execSQL(Table.CREATE_TABLE_STATEMENT)
    }

    @Test
    fun deleteTable() {
        Table.onDelete(database)

        inOrder(database) {
            verify(database).execSQL(Table.DROP_TABLE_STATEMENT)
            verify(database).execSQL(Table.CREATE_TABLE_STATEMENT)
        }
    }

    @Test
    fun upgradeDatabase_v1_to_v2() {
        Table.onUpdate(database, 1, 2)

        inOrder(database) {
            verify<SQLiteDatabase>(database).execSQL(Table.ADD_DESCRIPTION_FIELD)
            verify<SQLiteDatabase>(database).execSQL(Table.ADD_CREATOR_FIELD)
        }
    }

    @Test
    fun upgradeDatabase_v2_to_v3() {
        Table.onUpdate(database, 2, 3)

        inOrder(database) {
            verify<SQLiteDatabase>(database).execSQL(Table.ADD_MULTIPLE_FIELD)
            verify<SQLiteDatabase>(database).execSQL(Table.SET_DEFAULT_MULTIPLE)
        }
    }

    @Test
    fun upgradeDatabase_v3_to_v4() {
        Table.onUpdate(database, 3, 4)

        // No changes
        verifyZeroInteractions(database)
    }

    @Test
    fun upgradeDatabase_v4_to_v5() {
        Table.onUpdate(database, 4, 5)

        // No changes
        verifyZeroInteractions(database)
    }

    @Test
    fun upgradeDatabase_v5_to_v6() {
        Table.onUpdate(database, 5, 6)

        inOrder(database) {
            verify<SQLiteDatabase>(database).execSQL(Table.ADD_WIDTH_FIELD)
            verify<SQLiteDatabase>(database).execSQL(Table.SET_DEFAULT_WIDTH)
            verify<SQLiteDatabase>(database).execSQL(Table.ADD_HEIGHT_FIELD)
            verify<SQLiteDatabase>(database).execSQL(Table.SET_DEFAULT_HEIGHT)
            verify<SQLiteDatabase>(database).execSQL(Table.ADD_LICENSE_FIELD)
            verify<SQLiteDatabase>(database).execSQL(Table.SET_DEFAULT_LICENSE)
        }
    }

    @Test
    fun saveNewContribution_nonNullFields() {
        whenever(client.insert(isA(), isA())).thenReturn(contentUri)
        val contribution = createContribution(true, null, null, null, null)

        testObject.save(contribution)

        assertEquals(contentUri, contribution.contentUri)
        verify(client).insert(eq(BASE_URI), captor.capture())
        captor.firstValue.let {
            // Long fields
            assertEquals(222L, it.getAsLong(Table.COLUMN_LENGTH))
            assertEquals(321L, it.getAsLong(Table.COLUMN_TIMESTAMP))
            assertEquals(333L, it.getAsLong(Table.COLUMN_TRANSFERRED))

            // Integer fields
            assertEquals(STATE_COMPLETED, it.getAsInteger(Table.COLUMN_STATE))
            assertEquals(640, it.getAsInteger(Table.COLUMN_WIDTH))
            assertEquals(480, it.getAsInteger(Table.COLUMN_HEIGHT))

            // String fields
            assertEquals(SOURCE_CAMERA, it.getAsString(Table.COLUMN_SOURCE))
            assertEquals("desc", it.getAsString(Table.COLUMN_DESCRIPTION))
            assertEquals("create", it.getAsString(Table.COLUMN_CREATOR))
            assertEquals("007", it.getAsString(Table.COLUMN_LICENSE))
        }
    }

    @Test
    fun saveNewContribution_nullableFieldsAreNull() {
        whenever(client.insert(isA(), isA())).thenReturn(contentUri)
        val contribution = createContribution(true, null, null, null, null)

        testObject.save(contribution)

        assertEquals(contentUri, contribution.contentUri)
        verify(client).insert(eq(BASE_URI), captor.capture())
        captor.firstValue.let {
            // Nullable fields are absent if null
            assertFalse(it.containsKey(Table.COLUMN_LOCAL_URI))
            assertFalse(it.containsKey(Table.COLUMN_IMAGE_URL))
            assertFalse(it.containsKey(Table.COLUMN_UPLOADED))
        }
    }

    @Test
    fun saveNewContribution_nullableImageUrlUsesFileAsBackup() {
        whenever(client.insert(isA(), isA())).thenReturn(contentUri)
        val contribution = createContribution(true, null, null, null, "file")

        testObject.save(contribution)

        assertEquals(contentUri, contribution.contentUri)
        verify(client).insert(eq(BASE_URI), captor.capture())
        captor.firstValue.let {
            // Nullable fields are absent if null
            assertFalse(it.containsKey(Table.COLUMN_LOCAL_URI))
            assertFalse(it.containsKey(Table.COLUMN_UPLOADED))
            assertEquals(Utils.makeThumbBaseUrl("file"), it.getAsString(Table.COLUMN_IMAGE_URL))
        }
    }

    @Test
    fun saveNewContribution_nullableFieldsAreNonNull() {
        whenever(client.insert(isA(), isA())).thenReturn(contentUri)
        val contribution = createContribution(true, Uri.parse(localUri),
                "image", Date(456L), null)

        testObject.save(contribution)

        assertEquals(contentUri, contribution.contentUri)
        verify(client).insert(eq(BASE_URI), captor.capture())
        captor.firstValue.let {
            assertEquals(localUri, it.getAsString(Table.COLUMN_LOCAL_URI))
            assertEquals("image", it.getAsString(Table.COLUMN_IMAGE_URL))
            assertEquals(456L, it.getAsLong(Table.COLUMN_UPLOADED))
        }
    }

    @Test
    fun saveNewContribution_booleanEncodesTrue() {
        whenever(client.insert(isA(), isA())).thenReturn(contentUri)
        val contribution = createContribution(true, null, null, null, null)

        testObject.save(contribution)

        assertEquals(contentUri, contribution.contentUri)
        verify(client).insert(eq(BASE_URI), captor.capture())

        // Boolean true --> 1 for ths encoding scheme
        assertEquals("Boolean true should be encoded as 1", 1,
                captor.firstValue.getAsInteger(Table.COLUMN_MULTIPLE))
    }

    @Test
    fun saveNewContribution_booleanEncodesFalse() {
        whenever(client.insert(isA(), isA())).thenReturn(contentUri)
        val contribution = createContribution(false, null, null, null, null)

        testObject.save(contribution)

        assertEquals(contentUri, contribution.contentUri)
        verify(client).insert(eq(BASE_URI), captor.capture())

        // Boolean true --> 1 for ths encoding scheme
        assertEquals("Boolean false should be encoded as 0", 0,
                captor.firstValue.getAsInteger(Table.COLUMN_MULTIPLE))
    }

    @Test
    fun saveExistingContribution() {
        val contribution = createContribution(false, null, null, null, null)
        contribution.contentUri = contentUri

        testObject.save(contribution)

        verify(client).update(eq(contentUri), isA(), isNull(), isNull())
    }

    @Test(expected = RuntimeException::class)
    fun saveTranslatesExceptions() {
        whenever(client.insert(isA(), isA())).thenThrow(RemoteException(""))

        testObject.save(createContribution(false, null, null, null, null))
    }

    @Test(expected = RuntimeException::class)
    fun deleteTranslatesExceptions() {
        whenever(client.delete(anyOrNull(), anyOrNull(), anyOrNull())).thenThrow(RemoteException(""))

        val contribution = createContribution(false, null, null, null, null)
        contribution.contentUri = contentUri
        testObject.delete(contribution)
    }

    @Test(expected = RuntimeException::class)
    fun exceptionThrownWhenAttemptingToDeleteUnsavedContribution() {
        testObject.delete(createContribution(false, null, null, null, null))
    }

    @Test
    fun deleteExistingContribution() {
        val contribution = createContribution(false, null, null, null, null)
        contribution.contentUri = contentUri

        testObject.delete(contribution)

        verify(client).delete(eq(contentUri), isNull(), isNull())
    }

    @Test
    fun createFromCursor() {
        val created = 321L
        val uploaded = 456L
        createCursor(created, uploaded, false, localUri).let { mc ->
            testObject.fromCursor(mc).let {
                assertEquals(uriForId(111), it.contentUri)
                assertEquals("file", it.filename)
                assertEquals(localUri, it.localUri.toString())
                assertEquals("image", it.imageUrl)
                assertEquals(created, it.timestamp.time)
                assertEquals(created, it.dateCreated.time)
                assertEquals(STATE_QUEUED, it.state)
                assertEquals(222L, it.dataLength)
                assertEquals(uploaded, it.dateUploaded?.time)
                assertEquals(88L, it.transferred)
                assertEquals(SOURCE_GALLERY, it.source)
                assertEquals("desc", it.description)
                assertEquals("create", it.creator)
                assertEquals(640, it.width)
                assertEquals(480, it.height)
                assertEquals("007", it.license)
            }
        }
    }

    @Test
    fun createFromCursor_nullableTimestamps() {
        createCursor(0L, 0L, false, localUri).let { mc ->
            testObject.fromCursor(mc).let {
                assertNull(it.timestamp)
                assertNull(it.dateCreated)
                assertNull(it.dateUploaded)
            }
        }
    }

    @Test
    fun createFromCursor_nullableLocalUri() {
        createCursor(0L, 0L, false, "").let { mc ->
            testObject.fromCursor(mc).let {
                assertNull(it.localUri)
                assertNull(it.dateCreated)
                assertNull(it.dateUploaded)
            }
        }
    }

    @Test
    fun createFromCursor_booleanEncoding() {
        val mcFalse = createCursor(0L, 0L, false, localUri)
        assertFalse(testObject.fromCursor(mcFalse).multiple)

        val mcHammer = createCursor(0L, 0L, true, localUri)
        assertTrue(testObject.fromCursor(mcHammer).multiple)
    }

    private fun createCursor(created: Long, uploaded: Long, multiple: Boolean, localUri: String) =
            MatrixCursor(Table.ALL_FIELDS, 1).apply {
                addRow(listOf("111", "file", localUri, "image",
                        created, STATE_QUEUED, 222L, uploaded, 88L, SOURCE_GALLERY, "desc",
                        "create", if (multiple) 1 else 0, 640, 480, "007"))
                moveToFirst()
            }

    private fun createContribution(isMultiple: Boolean, localUri: Uri?, imageUrl: String?, dateUploaded: Date?, filename: String?) =
            Contribution(localUri, imageUrl, filename, "desc", 222L, Date(321L), dateUploaded,
                    "create", "edit", "coords").apply {
                state = STATE_COMPLETED
                transferred = 333L
                source = SOURCE_CAMERA
                license = "007"
                multiple = isMultiple
                timestamp = Date(321L)
                width = 640
                height = 480  // VGA should be enough for anyone, right?
            }
}