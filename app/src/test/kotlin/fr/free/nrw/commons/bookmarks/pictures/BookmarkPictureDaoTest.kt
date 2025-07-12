package fr.free.nrw.commons.bookmarks.pictures

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.RemoteException
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.models.Bookmark
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesContentProvider.Companion.BASE_URI
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.COLUMN_CREATOR
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.COLUMN_MEDIA_NAME
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.CREATE_TABLE_STATEMENT
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.DROP_TABLE_STATEMENT
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.onCreate
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.onDelete
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.onUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verifyNoInteractions
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class BookmarkPictureDaoTest {
    private val columns = arrayOf(COLUMN_MEDIA_NAME, COLUMN_CREATOR)
    private val client: ContentProviderClient = mock()
    private val database: SQLiteDatabase = mock()
    private val captor = argumentCaptor<ContentValues>()

    private lateinit var testObject: BookmarkPicturesDao
    private lateinit var exampleBookmark: Bookmark

    @Before
    fun setUp() {
        exampleBookmark = Bookmark("mediaName", "creatorName", Uri.EMPTY)
        testObject = BookmarkPicturesDao { client }
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
    fun createFromCursor() {
        createCursor(1).let { cursor ->
            cursor.moveToFirst()
            testObject.fromCursor(cursor).let {
                assertEquals("mediaName", it.mediaName)
                assertEquals("creatorName", it.mediaCreator)
            }
        }
    }

    @Test
    fun getAllBookmarks() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(createCursor(14))

        var result = testObject.getAllBookmarks()

        assertEquals(14, (result.size))
    }

    @Test(expected = RuntimeException::class)
    fun getAllBookmarksTranslatesExceptions() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenThrow(RemoteException(""))
        testObject.getAllBookmarks()
    }

    @Test
    fun getAllBookmarksReturnsEmptyList_emptyCursor() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(createCursor(0))
        assertTrue(testObject.getAllBookmarks().isEmpty())
    }

    @Test
    fun getAllBookmarksReturnsEmptyList_nullCursor() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(null)
        assertTrue(testObject.getAllBookmarks().isEmpty())
    }

    @Test
    fun cursorsAreClosedAfterGetAllBookmarksQuery() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.getAllBookmarks()

        verify(mockCursor).close()
    }

    @Test
    fun updateNewBookmark() {
        whenever(client.insert(any(), any())).thenReturn(exampleBookmark.contentUri)
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(null)

        assertTrue(testObject.updateBookmark(exampleBookmark))
        verify(client).insert(eq(BASE_URI), captor.capture())
        captor.firstValue.let { cv ->
            assertEquals(2, cv.size())
            assertEquals(exampleBookmark.mediaName, cv.getAsString(COLUMN_MEDIA_NAME))
            assertEquals(exampleBookmark.mediaCreator, cv.getAsString(COLUMN_CREATOR))
        }
    }

    @Test
    fun updateExistingBookmark() {
        whenever(client.delete(isA(), isNull(), isNull())).thenReturn(1)
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(1))

        assertFalse(testObject.updateBookmark(exampleBookmark))
        verify(client).delete(eq(exampleBookmark.contentUri!!), isNull(), isNull())
    }

    @Test
    fun findExistingBookmark() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(1))
        assertTrue(testObject.findBookmark(exampleBookmark))
    }

    @Test(expected = RuntimeException::class)
    fun findBookmarkTranslatesExceptions() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenThrow(RemoteException(""))
        testObject.findBookmark(exampleBookmark)
    }

    @Test
    fun findNotExistingBookmarkReturnsNull_emptyCursor() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(0))
        assertFalse(testObject.findBookmark(exampleBookmark))
    }

    @Test
    fun findNotExistingBookmarkReturnsNull_nullCursor() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(null)
        assertFalse(testObject.findBookmark(exampleBookmark))
    }

    @Test
    fun cursorsAreClosedAfterFindBookmarkQuery() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.findBookmark(exampleBookmark)

        verify(mockCursor).close()
    }

    @Test
    fun migrateTableVersionFrom_v1_to_v2() {
        onUpdate(database, 1, 2)
        // Table didn't exist before v5
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v2_to_v3() {
        onUpdate(database, 2, 3)
        // Table didn't exist before v5
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v3_to_v4() {
        onUpdate(database, 3, 4)
        // Table didn't exist before v5
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v4_to_v5() {
        onUpdate(database, 4, 5)
        // Table didn't change in version 5
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v5_to_v6() {
        onUpdate(database, 5, 6)
        // Table didn't change in version 6
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v6_to_v7() {
        onUpdate(database, 6, 7)
        // Table didn't change in version 7
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v7_to_v8() {
        onUpdate(database, 7, 8)
        verify(database).execSQL(CREATE_TABLE_STATEMENT)
    }

    private fun createCursor(rowCount: Int) =
        MatrixCursor(columns, rowCount).apply {
            for (i in 0 until rowCount) {
                addRow(listOf("mediaName", "creatorName"))
            }
        }
}
