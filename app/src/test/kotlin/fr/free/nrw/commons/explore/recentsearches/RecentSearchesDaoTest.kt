package fr.free.nrw.commons.explore.recentsearches

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.os.RemoteException
import com.nhaarman.mockitokotlin2.*
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
@Config(sdk = [21], application = TestCommonsApplication::class)
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

    /**
     * Unit Test for creating a table for recent Searches
     */
    @Test
    fun createTable() {
        onCreate(database)
        verify(database).execSQL(CREATE_TABLE_STATEMENT)
    }

    /**
     * Unit Test for deleting table for recent Searches
     */
    @Test
    fun deleteTable() {
        onDelete(database)
        inOrder(database) {
            verify(database).execSQL(DROP_TABLE_STATEMENT)
            verify(database).execSQL(CREATE_TABLE_STATEMENT)
        }
    }

    /**
     * Unit Test for migrating from database version 1 to 2 for recent Searches Table
     */
    @Test
    fun migrateTableVersionFrom_v1_to_v2() {
        onUpdate(database, 1, 2)
        // Table didnt exist before v7
        verifyZeroInteractions(database)
    }

    /**
     * Unit Test for migrating from database version 2 to 3 for recent Searches Table
     */
    @Test
    fun migrateTableVersionFrom_v2_to_v3() {
        onUpdate(database, 2, 3)
        // Table didnt exist before v7
        verifyZeroInteractions(database)
    }

    /**
     * Unit Test for migrating from database version 3 to 4 for recent Searches Table
     */
    @Test
    fun migrateTableVersionFrom_v3_to_v4() {
        onUpdate(database, 3, 4)
        // Table didnt exist before v7
        verifyZeroInteractions(database)
    }

    /**
     * Unit Test for migrating from database version 4 to 5 for recent Searches Table
     */
    @Test
    fun migrateTableVersionFrom_v4_to_v5() {
        onUpdate(database, 4, 5)
        // Table didnt exist before v7
        verifyZeroInteractions(database)
    }

    /**
     * Unit Test for migrating from database version 5 to 6 for recent Searches Table
     */
    @Test
    fun migrateTableVersionFrom_v5_to_v6() {
        onUpdate(database, 5, 6)
        // Table didnt exist before v7
        verifyZeroInteractions(database)
    }

    /**
     * Unit Test for migrating from database version 6 to 7 for recent Searches Table
     */
    @Test
    fun migrateTableVersionFrom_v6_to_v7() {
        onUpdate(database, 6, 7)
        verify(database).execSQL(CREATE_TABLE_STATEMENT)
    }

    /**
     * Unit Test for migrating from database version 7 to 8 for recent Searches Table
     */
    @Test
    fun migrateTableVersionFrom_v7_to_v8() {
        onUpdate(database, 7, 8)
        // Table didnt change in version 8
        verifyZeroInteractions(database)
    }

    /**
     * Unit Test for migrating from creating a row without using ID in recent Searches Table
     */
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

    /**
     * Unit Test for migrating from updating a row using contentUri in recent Searches Table
     */
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

    /**
     * Unit Test for migrating from creating a row using ID in recent Searches Table
     */
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

    /**
     * Unit Test for checking translation exceptions in searching a row from DB using recent search query
     */
    @Test(expected = RuntimeException::class)
    fun findRecentSearchTranslatesExceptions() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenThrow(RemoteException(""))
        testObject.find("butterfly")
    }

    /**
     * Unit Test for checking data if it's not present in searching a row from DB using recent search query
     */
    @Test
    fun whenTheresNoDataFindReturnsNull_nullCursor() {
        whenever(client.query(any(), any(), any(), any(), any())).thenReturn(null)
        assertNull(testObject.find("butterfly"))
    }

    /**
     * Unit Test for checking data if it's not present in searching a row from DB using recent search query
     */
    @Test
    fun whenTheresNoDataFindReturnsNull_emptyCursor() {
        whenever(client.query(any(), any(), any(), any(), any())).thenReturn(createCursor(0))
        assertNull(testObject.find("butterfly"))
    }

    /**
     * Unit Test for checking if cursor's are closed after use or not
     */
    @Test
    fun cursorsAreClosedAfterUse() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.find("butterfly")

        verify(mockCursor).close()
    }

    /**
     * Unit Test for checking search results after searching a row from DB using recent search query
     */
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

    /**
     * Unit Test for checking if cursor's are closed after recent search query or not
     */
    @Test
    fun cursorsAreClosedAfterRecentSearchQuery() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), anyOrNull(), any(), any())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.recentSearches(1)

        verify(mockCursor).close()
    }

    /**
     * Unit Test for checking when recent searches returns less than the limit
     */
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

    /**
     * Unit Test for checking size or list recieved from recent searches
     */
    @Test
    fun recentSearchesHonorsLimit() {
        whenever(client.query(any(), any(), anyOrNull(), any(), any())).thenReturn(createCursor(10))

        val result = testObject.recentSearches(5)

        assertEquals(5, result.size)
    }

    /**
     * Unit Test for creating entries in recent searches database.
     * @param rowCount No of rows
     */
    private fun createCursor(rowCount: Int) = MatrixCursor(columns, rowCount).apply {
        for (i in 0 until rowCount) {
            addRow(listOf("1", "butterfly", "123"))
        }
    }

}