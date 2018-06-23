package fr.free.nrw.commons.modifications

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.RemoteException
import com.nhaarman.mockito_kotlin.*
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.modifications.ModificationsContentProvider.BASE_URI
import fr.free.nrw.commons.modifications.ModifierSequenceDao.Table.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = [21], application = TestCommonsApplication::class)
class ModifierSequenceDaoTest {

    private val mediaUrl = "http://example.com/"
    private val columns = arrayOf(COLUMN_ID, COLUMN_MEDIA_URI, COLUMN_DATA)
    private val client: ContentProviderClient = mock()
    private val database: SQLiteDatabase = mock()
    private val contentValuesCaptor = argumentCaptor<ContentValues>()

    private lateinit var testObject: ModifierSequenceDao

    @Before
    fun setUp() {
        testObject = ModifierSequenceDao { client }
    }

    @Test
    fun createFromCursorWithEmptyModifiers() {
        testObject.fromCursor(createCursor("")).let {
            assertEquals(mediaUrl, it.mediaUri.toString())
            assertEquals(BASE_URI.buildUpon().appendPath("1").toString(), it.contentUri.toString())
            assertTrue(it.modifiers.isEmpty())
        }
    }

    @Test
    fun createFromCursorWtihCategoryModifier() {
        val cursor = createCursor("{\"name\": \"CategoriesModifier\", \"data\": {}}")

        val seq = testObject.fromCursor(cursor)

        assertEquals(1, seq.modifiers.size)
        assertTrue(seq.modifiers[0] is CategoryModifier)
    }

    @Test
    fun createFromCursorWithRemoveModifier() {
        val cursor = createCursor("{\"name\": \"TemplateRemoverModifier\", \"data\": {}}")

        val seq = testObject.fromCursor(cursor)

        assertEquals(1, seq.modifiers.size)
        assertTrue(seq.modifiers[0] is TemplateRemoveModifier)
    }

    @Test
    fun deleteSequence() {
        whenever(client.delete(isA(), isNull(), isNull())).thenReturn(1)
        val seq = testObject.fromCursor(createCursor(""))

        testObject.delete(seq)

        verify(client).delete(eq(seq.contentUri), isNull(), isNull())
    }

    @Test(expected = RuntimeException::class)
    fun deleteTranslatesRemoteExceptions() {
        whenever(client.delete(isA(), isNull(), isNull())).thenThrow(RemoteException(""))
        val seq = testObject.fromCursor(createCursor(""))

        testObject.delete(seq)
    }

    @Test
    fun saveExistingSequence() {
        val modifierJson = "{\"name\":\"CategoriesModifier\",\"data\":{}}"
        val expectedData = "{\"modifiers\":[$modifierJson]}"
        val cursor = createCursor(modifierJson)
        val seq = testObject.fromCursor(cursor)

        testObject.save(seq)

        verify(client).update(eq(seq.contentUri), contentValuesCaptor.capture(), isNull(), isNull())
        contentValuesCaptor.firstValue.let {
            assertEquals(2, it.size())
            assertEquals(mediaUrl, it.get(COLUMN_MEDIA_URI))
            assertEquals(expectedData, it.get(COLUMN_DATA))
        }
    }

    @Test
    fun saveNewSequence() {
        val expectedContentUri = BASE_URI.buildUpon().appendPath("1").build()
        whenever(client.insert(isA(), isA())).thenReturn(expectedContentUri)
        val seq = ModifierSequence(Uri.parse(mediaUrl))

        testObject.save(seq)

        assertEquals(expectedContentUri.toString(), seq.contentUri.toString())
        verify(client).insert(eq(ModificationsContentProvider.BASE_URI), contentValuesCaptor.capture())
        contentValuesCaptor.firstValue.let {
            assertEquals(2, it.size())
            assertEquals(mediaUrl, it.get(COLUMN_MEDIA_URI))
            assertEquals("{\"modifiers\":[]}", it.get(COLUMN_DATA))
        }
    }

    @Test(expected = RuntimeException::class)
    fun saveTranslatesRemoteExceptions() {
        whenever(client.insert(isA(), isA())).thenThrow(RemoteException(""))
        testObject.save(ModifierSequence(Uri.parse(mediaUrl)))
    }

    @Test
    fun createTable() {
        onCreate(database)
        verify(database).execSQL(CREATE_TABLE_STATEMENT)
    }

    @Test
    fun updateTable() {
        onUpdate(database, 1, 2)

        inOrder(database) {
            verify<SQLiteDatabase>(database).execSQL(DROP_TABLE_STATEMENT)
            verify<SQLiteDatabase>(database).execSQL(CREATE_TABLE_STATEMENT)
        }
    }

    @Test
    fun deleteTable() {
        onDelete(database)

        inOrder(database) {
            verify<SQLiteDatabase>(database).execSQL(DROP_TABLE_STATEMENT)
            verify<SQLiteDatabase>(database).execSQL(CREATE_TABLE_STATEMENT)
        }
    }

    private fun createCursor(modifierJson: String) = MatrixCursor(columns, 1).apply {
        addRow(listOf("1", mediaUrl, "{\"modifiers\": [$modifierJson]}"))
        moveToFirst()
    }
}