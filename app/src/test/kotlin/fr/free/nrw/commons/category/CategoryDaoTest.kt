package fr.free.nrw.commons.category

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.os.RemoteException
import com.nhaarman.mockito_kotlin.*
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.category.CategoryContentProvider.BASE_URI
import fr.free.nrw.commons.category.CategoryContentProvider.uriForId
import fr.free.nrw.commons.category.CategoryDao.Table.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = [21], application = TestCommonsApplication::class)
class CategoryDaoTest {

    private val columns = arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_LAST_USED, COLUMN_TIMES_USED)
    private val client: ContentProviderClient = mock()
    private val database: SQLiteDatabase = mock()
    private val captor = argumentCaptor<ContentValues>()
    private val queryCaptor = argumentCaptor<Array<String>>()

    private lateinit var testObject: CategoryDao

    @Before
    fun setUp() {
        testObject = CategoryDao { client }
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
        // Table didnt exist before v5
        verifyZeroInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v2_to_v3() {
        onUpdate(database, 2, 3)
        // Table didnt exist before v5
        verifyZeroInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v3_to_v4() {
        onUpdate(database, 3, 4)
        // Table didnt exist before v5
        verifyZeroInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v4_to_v5() {
        onUpdate(database, 4, 5)
        verify(database).execSQL(CREATE_TABLE_STATEMENT)
    }

    @Test
    fun migrateTableVersionFrom_v5_to_v6() {
        onUpdate(database, 5, 6)
        // Table didnt change in version 6
        verifyZeroInteractions(database)
    }

    @Test
    fun createFromCursor() {
        createCursor(1).let { cursor ->
            cursor.moveToFirst()
            testObject.fromCursor(cursor).let {
                assertEquals(uriForId(1), it.contentUri)
                assertEquals("foo", it.name)
                assertEquals(123, it.lastUsed.time)
                assertEquals(2, it.timesUsed)
            }
        }
    }

    @Test
    fun saveExistingCategory() {
        createCursor(1).let {
            val category = testObject.fromCursor(it.apply { moveToFirst() })

            testObject.save(category)

            verify(client).update(eq(category.contentUri), captor.capture(), isNull(), isNull())
            captor.firstValue.let { cv ->
                assertEquals(3, cv.size())
                assertEquals(category.name, cv.getAsString(COLUMN_NAME))
                assertEquals(category.lastUsed.time, cv.getAsLong(COLUMN_LAST_USED))
                assertEquals(category.timesUsed, cv.getAsInteger(COLUMN_TIMES_USED))
            }
        }
    }

    @Test
    fun saveNewCategory() {
        val contentUri = CategoryContentProvider.uriForId(111)
        whenever(client.insert(isA(), isA())).thenReturn(contentUri)
        val category = Category(null, "foo", Date(234L), 1)

        testObject.save(category)

        verify(client).insert(eq(BASE_URI), captor.capture())
        captor.firstValue.let { cv ->
            assertEquals(3, cv.size())
            assertEquals(category.name, cv.getAsString(COLUMN_NAME))
            assertEquals(category.lastUsed.time, cv.getAsLong(COLUMN_LAST_USED))
            assertEquals(category.timesUsed, cv.getAsInteger(COLUMN_TIMES_USED))
            assertEquals(contentUri, category.contentUri)
        }
    }

    @Test(expected = RuntimeException::class)
    fun testSaveTranslatesRemoteExceptions() {
        whenever(client.insert(isA(), isA())).thenThrow(RemoteException(""))
        testObject.save(Category())
    }

    @Test
    fun whenTheresNoDataFindReturnsNull_nullCursor() {
        whenever(client.query(any(), any(), any(), any(), any())).thenReturn(null)
        assertNull(testObject.find("foo"))
    }

    @Test
    fun whenTheresNoDataFindReturnsNull_emptyCursor() {
        whenever(client.query(any(), any(), any(), any(), any())).thenReturn(createCursor(0))
        assertNull(testObject.find("foo"))
    }

    @Test
    fun cursorsAreClosedAfterUse() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.find("foo")

        verify(mockCursor).close()
    }

    @Test
    fun findCategory() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(1))

        val category = testObject.find("foo")
        assertNotNull(category)

        assertEquals(uriForId(1), category?.contentUri)
        assertEquals("foo", category?.name)
        assertEquals(123L, category?.lastUsed?.time)
        assertEquals(2, category?.timesUsed)

        verify(client).query(
                eq(BASE_URI),
                eq(ALL_FIELDS),
                eq("$COLUMN_NAME=?"),
                queryCaptor.capture(),
                isNull()
        )
        assertEquals("foo", queryCaptor.firstValue[0])
    }

    @Test(expected = RuntimeException::class)
    fun findCategoryTranslatesExceptions() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenThrow(RemoteException(""))
        testObject.find("foo")
    }

    @Test(expected = RuntimeException::class)
    fun recentCategoriesTranslatesExceptions() {
        whenever(client.query(any(), any(), anyOrNull(), any(), any())).thenThrow(RemoteException(""))
        testObject.recentCategories(1)
    }

    @Test
    fun recentCategoriesReturnsEmptyList_nullCursor() {
        whenever(client.query(any(), any(), anyOrNull(), any(), any())).thenReturn(null)
        assertTrue(testObject.recentCategories(1).isEmpty())
    }

    @Test
    fun recentCategoriesReturnsEmptyList_emptyCursor() {
        whenever(client.query(any(), any(), any(), any(), any())).thenReturn(createCursor(0))
        assertTrue(testObject.recentCategories(1).isEmpty())
    }

    @Test
    fun cursorsAreClosedAfterRecentCategoriesQuery() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), anyOrNull(), any(), any())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.recentCategories(1)

        verify(mockCursor).close()
    }

    @Test
    fun recentCategoriesReturnsLessThanLimit() {
        whenever(client.query(any(), any(), anyOrNull(), any(), any())).thenReturn(createCursor(1))

        val result = testObject.recentCategories(10)

        assertEquals(1, result.size)
        assertEquals("foo", result[0])

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
    fun recentCategoriesHonorsLimit() {
        whenever(client.query(any(), any(), anyOrNull(), any(), any())).thenReturn(createCursor(10))

        val result = testObject.recentCategories(5)

        assertEquals(5, result.size)
    }

    private fun createCursor(rowCount: Int) = MatrixCursor(columns, rowCount).apply {
        for (i in 0 until rowCount) {
            addRow(listOf("1", "foo", "123", "2"))
        }
    }

}