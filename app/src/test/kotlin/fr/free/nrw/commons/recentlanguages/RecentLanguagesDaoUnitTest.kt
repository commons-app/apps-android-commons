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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoInteractions
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
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v2_to_v3() {
        onUpdate(database, 2, 3)
        // Table didnt exist before v7
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v3_to_v4() {
        onUpdate(database, 3, 4)
        // Table didnt exist before v7
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v4_to_v5() {
        onUpdate(database, 4, 5)
        // Table didnt exist before v7
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v5_to_v6() {
        onUpdate(database, 5, 6)
        // Table didnt exist in version 6
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v6_to_v7() {
        onUpdate(database, 6, 7)
        // Table didnt exist in version 7
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v7_to_v8() {
        onUpdate(database, 7, 8)
        // Table didnt exist in version 8
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v8_to_v9() {
        onUpdate(database, 8, 9)
        // Table didnt exist in version 9
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v9_to_v10() {
        onUpdate(database, 9, 10)
        // Table didnt exist in version 10
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v10_to_v11() {
        onUpdate(database, 10, 11)
        // Table didnt exist in version 11
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v11_to_v12() {
        onUpdate(database, 11, 12)
        // Table didnt exist in version 12
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v12_to_v13() {
        onUpdate(database, 12, 13)
        // Table didnt exist in version 13
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v13_to_v14() {
        onUpdate(database, 13, 14)
        // Table didnt exist in version 14
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v14_to_v15() {
        onUpdate(database, 14, 15)
        // Table didnt exist in version 15
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v15_to_v16() {
        onUpdate(database, 15, 16)
        // Table didnt exist in version 16
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v16_to_v17() {
        onUpdate(database, 16, 17)
        // Table didnt exist in version 17
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v18_to_v19() {
        onUpdate(database, 18, 19)
        // Table didnt exist in version 18
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v19_to_v20() {
        onUpdate(database, 19, 20)
        verify(database).execSQL(CREATE_TABLE_STATEMENT)
    }

    @Test
    fun migrateTableVersionFrom_v20_to_v20() {
        onUpdate(database, 20, 20)
        verifyNoInteractions(database)
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