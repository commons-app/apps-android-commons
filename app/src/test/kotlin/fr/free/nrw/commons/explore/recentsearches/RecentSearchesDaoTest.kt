package fr.free.nrw.commons.explore.recentsearches

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.os.RemoteException
import com.nhaarman.mockito_kotlin.*
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesContentProvider.BASE_URI
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesContentProvider.uriForId
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao.Table.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = [21], application = TestCommonsApplication::class)
class RecentSearchesDaoTest {

    private val columns = arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_LAST_USED)
    private val client: ContentProviderClient = mock()
    private val database: SQLiteDatabase = mock()
    private val captor = argumentCaptor<ContentValues>()
    private val queryCaptor = argumentCaptor<Array<String>>()

    private lateinit var testObject: RecentSearchesDao

    @Before
    fun setUp() {
        testObject = RecentSearchesDao { client }
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
            verify(database).execSQL(CREATE_TABLE_STATEMENT)
        }
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
    fun createFromCursor() {
        createCursor(1).let { cursor ->
            cursor.moveToFirst()
            testObject.fromCursor(cursor).let {
                assertEquals(uriForId(1), it.contentUri)
                assertEquals("butterfly", it.query)
                assertEquals(123, it.lastSearched.time)
            }
        }
    }

    @Test
    fun saveExistingQuery() {
        createCursor(1).let {
            val recentSearch = testObject.fromCursor(it.apply { moveToFirst() })

            testObject.save(recentSearch)

            verify(client).update(eq(recentSearch.contentUri), captor.capture(), isNull(), isNull())
            captor.firstValue.let { cv ->
                assertEquals(2, cv.size())
                assertEquals(recentSearch.query, cv.getAsString(COLUMN_NAME))
                assertEquals(recentSearch.lastSearched.time, cv.getAsLong(COLUMN_LAST_USED))
            }
        }
    }

    @Test
    fun saveNewQuery() {
        val contentUri = RecentSearchesContentProvider.uriForId(111)
        whenever(client.insert(isA(), isA())).thenReturn(contentUri)
        val recentSearch = RecentSearch(null, "butterfly", Date(234L))

        testObject.save(recentSearch)

        verify(client).insert(eq(BASE_URI), captor.capture())
        captor.firstValue.let { cv ->
            assertEquals(2, cv.size())
            assertEquals(recentSearch.query, cv.getAsString(COLUMN_NAME))
            assertEquals(recentSearch.lastSearched.time, cv.getAsLong(COLUMN_LAST_USED))
            assertEquals(contentUri, recentSearch.contentUri)
        }
    }

    @Test(expected = RuntimeException::class)
    fun findRecentSearchTranslatesExceptions() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenThrow(RemoteException(""))
        testObject.find("butterfly")
    }

    @Test
    fun whenTheresNoDataFindReturnsNull_nullCursor() {
        whenever(client.query(any(), any(), any(), any(), any())).thenReturn(null)
        assertNull(testObject.find("butterfly"))
    }

    @Test
    fun whenTheresNoDataFindReturnsNull_emptyCursor() {
        whenever(client.query(any(), any(), any(), any(), any())).thenReturn(createCursor(0))
        assertNull(testObject.find("butterfly"))
    }

    @Test
    fun cursorsAreClosedAfterUse() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.find("butterfly")

        verify(mockCursor).close()
    }


    @Test
    fun findRecentSearchQuery() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(1))

        val recentSearch = testObject.find("butterfly")
        assertNotNull(recentSearch)

        assertEquals(uriForId(1), recentSearch?.contentUri)
        assertEquals("butterfly", recentSearch?.query)
        assertEquals(123L, recentSearch?.lastSearched?.time)

        verify(client).query(
                eq(BASE_URI),
                eq(ALL_FIELDS),
                eq("$COLUMN_NAME=?"),
                queryCaptor.capture(),
                isNull()
        )
        assertEquals("butterfly", queryCaptor.firstValue[0])
    }

    @Test
    fun cursorsAreClosedAfterRecentSearchQuery() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), anyOrNull(), any(), any())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.recentSearches(1)

        verify(mockCursor).close()
    }

    @Test
    fun recentSearchesReturnsLessThanLimit() {
        whenever(client.query(any(), any(), anyOrNull(), any(), any())).thenReturn(createCursor(1))

        val result = testObject.recentSearches(10)

        assertEquals(1, result.size)
        assertEquals("butterfly", result[0])

        verify(client).query(
                eq(BASE_URI),
                eq(ALL_FIELDS),
                isNull(),
                queryCaptor.capture(),
                eq("$COLUMN_LAST_USED DESC")
        )
        assertEquals(0, queryCaptor.firstValue.size)
    }

    @Test
    fun recentSearchesHonorsLimit() {
        whenever(client.query(any(), any(), anyOrNull(), any(), any())).thenReturn(createCursor(10))

        val result = testObject.recentSearches(5)

        assertEquals(5, result.size)
    }

    private fun createCursor(rowCount: Int) = MatrixCursor(columns, rowCount).apply {
        for (i in 0 until rowCount) {
            addRow(listOf("1", "butterfly", "123"))
        }
    }

}