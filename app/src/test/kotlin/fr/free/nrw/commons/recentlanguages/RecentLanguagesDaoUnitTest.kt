package fr.free.nrw.commons.recentlanguages

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.os.RemoteException
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao.Table.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class RecentLanguagesDaoUnitTest {

    private val columns = arrayOf(
        COLUMN_NAME,
        COLUMN_CODE
    )

    private val client: ContentProviderClient = mock()
    private val database: SQLiteDatabase = mock()
    private val captor = argumentCaptor<ContentValues>()

    private lateinit var testObject: RecentLanguagesDao
    private lateinit var exampleLanguage: Language

    /**
     * Set up Test Language and RecentLanguagesDao
     */
    @Before
    fun setUp() {
        exampleLanguage = Language("English", "en")
        testObject = RecentLanguagesDao { client }
    }

    @Test
    fun createTable() {
        onCreate(database)
        verify(database).execSQL(CREATE_TABLE_STATEMENT)
    }

    @Test
    fun deleteTable() {
        onDelete(database)
        inOrder(database) {
            verify(database).execSQL(DROP_TABLE_STATEMENT)
        }
    }

    @Test
    fun createFromCursor() {
        createCursor(1).let { cursor ->
            cursor.moveToFirst()
            testObject.fromCursor(cursor).let {
                Assert.assertEquals("languageName", it.languageName)
                Assert.assertEquals("languageCode", it.languageCode)
            }
        }
    }

    @Test
    fun testGetRecentLanguages() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull()))
            .thenReturn(createCursor(14))

        val result = testObject.recentLanguages

        Assert.assertEquals(14, (result.size))

    }

    @Test(expected = RuntimeException::class)
    fun getGetRecentLanguagesTranslatesExceptions() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenThrow(
            RemoteException("")
        )
        testObject.recentLanguages
    }

    @Test
    fun getGetRecentLanguagesReturnsEmptyList_emptyCursor() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull()))
            .thenReturn(createCursor(0))
        Assert.assertTrue(testObject.recentLanguages.isEmpty())
    }

    @Test
    fun getGetRecentLanguagesReturnsEmptyList_nullCursor() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(null)
        Assert.assertTrue(testObject.recentLanguages.isEmpty())
    }

    @Test
    fun cursorsAreClosedAfterGetRecentLanguages() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.recentLanguages

        verify(mockCursor).close()
    }

    @Test
    fun findExistingLanguage() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(1))
        Assert.assertTrue(testObject.findRecentLanguage(exampleLanguage.languageCode))
    }

    @Test(expected = RuntimeException::class)
    fun findLanguageTranslatesExceptions() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenThrow(
            RemoteException("")
        )
        testObject.findRecentLanguage(exampleLanguage.languageCode)
    }

    @Test
    fun findNotExistingLanguageReturnsNull_emptyCursor() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(0))
        Assert.assertFalse(testObject.findRecentLanguage(exampleLanguage.languageCode))
    }

    @Test
    fun findNotExistingLanguageReturnsNull_nullCursor() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(null)
        Assert.assertFalse(testObject.findRecentLanguage(exampleLanguage.languageCode))
    }

    @Test
    fun cursorsAreClosedAfterFindLanguageQuery() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.findRecentLanguage(exampleLanguage.languageCode)

        verify(mockCursor).close()
    }

    @Test
    fun migrateTableVersionFrom_v1_to_v2() {
        onUpdate(database, 1, 2)
        // Table didnt exist before v7
        verifyZeroInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v2_to_v3() {
        onUpdate(database, 2, 3)
        // Table didnt exist before v7
        verifyZeroInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v3_to_v4() {
        onUpdate(database, 3, 4)
        // Table didnt exist before v7
        verifyZeroInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v4_to_v5() {
        onUpdate(database, 4, 5)
        // Table didnt exist before v7
        verifyZeroInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v5_to_v6() {
        onUpdate(database, 5, 6)
        // Table didnt exist before v7
        verifyZeroInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v6_to_v7() {
        onUpdate(database, 6, 7)
        verify(database).execSQL(CREATE_TABLE_STATEMENT)
    }

    @Test
    fun migrateTableVersionFrom_v7_to_v8() {
        onUpdate(database, 7, 8)
        // Table didnt change in version 8
        verifyZeroInteractions(database)
    }

    @Test
    fun testAddNewLanguage() {
        testObject.addRecentLanguage(exampleLanguage)

        verify(client).insert(eq(RecentLanguagesContentProvider.BASE_URI), captor.capture())
        captor.firstValue.let { cv ->
            Assert.assertEquals(2, cv.size())
            Assert.assertEquals(
                exampleLanguage.languageName,
                cv.getAsString(COLUMN_NAME)
            )
            Assert.assertEquals(
                exampleLanguage.languageCode,
                cv.getAsString(COLUMN_CODE)
            )
        }
    }

    @Test
    fun testDeleteLanguage() {
        testObject.addRecentLanguage(exampleLanguage)
        testObject.deleteRecentLanguage(exampleLanguage.languageCode)
    }

    private fun createCursor(rowCount: Int) = MatrixCursor(columns, rowCount).apply {

        for (i in 0 until rowCount) {
            addRow(listOf("languageName", "languageCode"))
        }
    }
}