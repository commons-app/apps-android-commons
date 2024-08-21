package fr.free.nrw.commons.bookmarks.items

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.RemoteException
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao.Table.*
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verifyNoInteractions
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class BookmarkItemsDaoTest {
    private val columns = arrayOf(
        COLUMN_NAME,
        COLUMN_DESCRIPTION,
        COLUMN_IMAGE,
        COLUMN_INSTANCE_LIST,
        COLUMN_CATEGORIES_NAME_LIST,
        COLUMN_CATEGORIES_DESCRIPTION_LIST,
        COLUMN_CATEGORIES_THUMBNAIL_LIST,
        COLUMN_IS_SELECTED,
        COLUMN_ID,
    )
    private val client: ContentProviderClient = mock()
    private val database: SQLiteDatabase = mock()
    private val captor = argumentCaptor<ContentValues>()

    private lateinit var testObject: BookmarkItemsDao
    private lateinit var exampleItemBookmark: DepictedItem

    /**
     * Set up Test DepictedItem and BookmarkItemsDao
     */
    @Before
    fun setUp() {
        exampleItemBookmark = DepictedItem("itemName", "itemDescription",
            "itemImageUrl", listOf("instance"), listOf(
                CategoryItem("category name", "category description",
                "category thumbnail", false)
            ), false,
            "itemID")
        testObject = BookmarkItemsDao { client }
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
                Assert.assertEquals("itemName", it.name)
                Assert.assertEquals("itemDescription", it.description)
                Assert.assertEquals("itemImageUrl", it.imageUrl)
                Assert.assertEquals(listOf("instance"), it.instanceOfs)
                Assert.assertEquals(listOf(CategoryItem("category name",
                    "category description",
                    "category thumbnail", false)), it.commonsCategories)
                Assert.assertEquals(false, it.isSelected)
                Assert.assertEquals("itemID", it.id)
            }
        }
    }

    @Test
    fun getAllItemsBookmarks() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull()))
            .thenReturn(createCursor(14))

        val result = testObject.allBookmarksItems

        Assert.assertEquals(14, (result.size))

    }

    @Test(expected = RuntimeException::class)
    fun getAllItemsBookmarksTranslatesExceptions() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenThrow(
            RemoteException("")
        )
        testObject.allBookmarksItems
    }

    @Test
    fun getAllItemsBookmarksReturnsEmptyList_emptyCursor() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull()))
            .thenReturn(createCursor(0))
        Assert.assertTrue(testObject.allBookmarksItems.isEmpty())
    }

    @Test
    fun getAllItemsBookmarksReturnsEmptyList_nullCursor() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(null)
        Assert.assertTrue(testObject.allBookmarksItems.isEmpty())
    }

    @Test
    fun cursorsAreClosedAfterGetAllItemsBookmarksQuery() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.allBookmarksItems

        verify(mockCursor).close()
    }


    @Test
    fun updateNewItemBookmark() {
        whenever(client.insert(any(), any())).thenReturn(Uri.EMPTY)
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(null)

        Assert.assertTrue(testObject.updateBookmarkItem(exampleItemBookmark))
        verify(client).insert(eq(BookmarkItemsContentProvider.BASE_URI), captor.capture())
        captor.firstValue.let { cv ->
            Assert.assertEquals(9, cv.size())
            Assert.assertEquals(
                exampleItemBookmark.name,
                cv.getAsString(COLUMN_NAME)
            )
            Assert.assertEquals(
                exampleItemBookmark.description,
                cv.getAsString(COLUMN_DESCRIPTION)
            )
            Assert.assertEquals(
                exampleItemBookmark.imageUrl,
                cv.getAsString(COLUMN_IMAGE)
            )
            Assert.assertEquals(
                exampleItemBookmark.instanceOfs[0],
                cv.getAsString(COLUMN_INSTANCE_LIST)
            )
            Assert.assertEquals(
                exampleItemBookmark.commonsCategories[0].name,
                cv.getAsString(COLUMN_CATEGORIES_NAME_LIST)
            )
            Assert.assertEquals(
                exampleItemBookmark.commonsCategories[0].description,
                cv.getAsString(COLUMN_CATEGORIES_DESCRIPTION_LIST)
            )
            Assert.assertEquals(
                exampleItemBookmark.commonsCategories[0].thumbnail,
                cv.getAsString(COLUMN_CATEGORIES_THUMBNAIL_LIST)
            )
            Assert.assertEquals(
                exampleItemBookmark.isSelected,
                cv.getAsBoolean(COLUMN_IS_SELECTED)
            )
            Assert.assertEquals(
                exampleItemBookmark.id,
                cv.getAsString(COLUMN_ID)
            )
        }
    }

    @Test
    fun updateExistingItemBookmark() {
        whenever(client.delete(isA(), isNull(), isNull())).thenReturn(1)
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(1))

        Assert.assertFalse(testObject.updateBookmarkItem(exampleItemBookmark))
        verify(client).delete(eq(BookmarkItemsContentProvider.uriForName(exampleItemBookmark.id)),
            isNull(), isNull())
    }

    @Test
    fun findExistingItemBookmark() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(1))
        Assert.assertTrue(testObject.findBookmarkItem(exampleItemBookmark.id))
    }

    @Test(expected = RuntimeException::class)
    fun findItemBookmarkTranslatesExceptions() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenThrow(
            RemoteException("")
        )
        testObject.findBookmarkItem(exampleItemBookmark.id)
    }

    @Test
    fun findNotExistingItemBookmarkReturnsNull_emptyCursor() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(0))
        Assert.assertFalse(testObject.findBookmarkItem(exampleItemBookmark.id))
    }

    @Test
    fun findNotExistingItemBookmarkReturnsNull_nullCursor() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(null)
        Assert.assertFalse(testObject.findBookmarkItem(exampleItemBookmark.id))
    }

    @Test
    fun cursorsAreClosedAfterFindItemBookmarkQuery() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.findBookmarkItem(exampleItemBookmark.id)

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
        // Table didn't change in version 8
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v8_to_v9() {
        onUpdate(database, 8, 9)
        // Table didn't change in version 9
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v9_to_v10() {
        onUpdate(database, 9, 10)
        // Table didn't change in version 10
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v10_to_v11() {
        onUpdate(database, 10, 11)
        // Table didn't change in version 11
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v11_to_v12() {
        onUpdate(database, 11, 12)
        // Table didn't change in version 12
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v12_to_v13() {
        onUpdate(database, 12, 13)
        // Table didn't change in version 13
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v13_to_v14() {
        onUpdate(database, 13, 14)
        // Table didn't change in version 14
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v14_to_v15() {
        onUpdate(database, 14, 15)
        // Table didn't change in version 15
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v15_to_v16() {
        onUpdate(database, 15, 16)
        // Table didn't change in version 16
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v16_to_v17() {
        onUpdate(database, 16, 17)
        // Table didn't change in version 17
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v18_to_v19() {
        onUpdate(database, 18, 19)
        verify(database).execSQL(CREATE_TABLE_STATEMENT)
    }

    @Test
    fun migrateTableVersionFrom_v19_to_v19() {
        onUpdate(database, 19, 19)
        verifyNoInteractions(database)
    }

    private fun createCursor(rowCount: Int) = MatrixCursor(columns, rowCount).apply {

        for (i in 0 until rowCount) {
            addRow(listOf("itemName", "itemDescription",
                "itemImageUrl", "instance", "category name", "category description",
                "category thumbnail", false, "itemID"))
        }
    }
}