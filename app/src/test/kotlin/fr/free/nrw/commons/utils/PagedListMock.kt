package fr.free.nrw.commons.utils


import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.paging.LimitOffsetDataSource
import com.nhaarman.mockitokotlin2.whenever
import org.mockito.Mockito.mock

fun <T> List<T>.asPagedList(config: PagedList.Config? = null): LiveData<PagedList<T>> {
    val defaultConfig = PagedList.Config.Builder()
        .setEnablePlaceholders(false)
        .setPageSize(size)
        .setMaxSize(size + 2)
        .setPrefetchDistance(1)
        .build()
    return LivePagedListBuilder<Int, T>(
        createMockDataSourceFactory(this),
        config ?: defaultConfig
    ).build()
}

fun <T> createMockDataSourceFactory(itemList: List<T>): DataSource.Factory<Int, T> =
    object : DataSource.Factory<Int, T>() {
        override fun create(): DataSource<Int, T> = MockLimitDataSource(itemList)
    }

private fun mockQuery(): RoomSQLiteQuery? {
    val query = mock(RoomSQLiteQuery::class.java);
    whenever(query.sql).thenReturn("");
    return query;
}

private fun mockDb(): RoomDatabase? {
    val roomDatabase = mock(RoomDatabase::class.java);
    val invalidationTracker = mock(InvalidationTracker::class.java)
    whenever(roomDatabase.invalidationTracker).thenReturn(invalidationTracker);
    return roomDatabase;
}

class MockLimitDataSource<T>(private val itemList: List<T>) :
    LimitOffsetDataSource<T>(mockDb(), mockQuery(), false, null) {
    override fun convertRows(cursor: Cursor?): MutableList<T> = itemList.toMutableList()
    override fun countItems(): Int = itemList.count()
    override fun isInvalid(): Boolean = false
    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
    }

    override fun loadRange(startPosition: Int, loadCount: Int): MutableList<T> {
        return itemList.subList(startPosition, startPosition + loadCount).toMutableList()
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
        callback.onResult(itemList, 0)
    }
}