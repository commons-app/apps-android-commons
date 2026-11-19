package fr.free.nrw.commons.settings.sync

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.bookmarks.category.BookmarkCategoriesDao
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class UserSettingsSyncManagerTest {
    @Mock
    private lateinit var sessionManager: SessionManager

    @Mock
    private lateinit var pageEditClient: PageEditClient

    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var bookmarkLocationsDao: BookmarkLocationsDao

    @Mock
    private lateinit var bookmarkCategoriesDao: BookmarkCategoriesDao

    @Mock
    private lateinit var bookmarkItemsDao: BookmarkItemsDao

    @Mock
    private lateinit var bookmarkPicturesDao: BookmarkPicturesDao

    private val gson = Gson()

    private lateinit var userSettingsSyncManager: UserSettingsSyncManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }

        userSettingsSyncManager =
            UserSettingsSyncManager(
                sessionManager,
                pageEditClient,
                defaultKvStore,
                bookmarkLocationsDao,
                bookmarkCategoriesDao,
                bookmarkItemsDao,
                bookmarkPicturesDao,
                gson,
            )
    }

    @Test
    fun backupToUserPage_userNotLoggedIn_returnsError() {
        whenever(sessionManager.isUserLoggedIn).thenReturn(false)
        whenever(sessionManager.userName).thenReturn(null)

        userSettingsSyncManager
            .backupToUserPage()
            .test()
            .assertError(IllegalStateException::class.java)
    }

    @Test
    fun backupToUserPage_success() {
        whenever(sessionManager.isUserLoggedIn).thenReturn(true)
        whenever(sessionManager.userName).thenReturn("TestUser")
        whenever(defaultKvStore.all).thenReturn(mapOf("appThemePref" to "1"))
        kotlinx.coroutines.runBlocking {
            whenever(bookmarkLocationsDao.getAllBookmarksLocations()).thenReturn(emptyList())
        }
        whenever(bookmarkCategoriesDao.getAllCategories()).thenReturn(flowOf(emptyList()))
        whenever(bookmarkItemsDao.getAllBookmarksItems()).thenReturn(emptyList())
        whenever(bookmarkPicturesDao.getAllBookmarks()).thenReturn(emptyList())
        whenever(pageEditClient.edit(eq("User:TestUser/commons-android-app.json"), any(), any()))
            .thenReturn(Observable.just(true))

        userSettingsSyncManager
            .backupToUserPage()
            .test()
            .assertValue(true)
    }

    @Test
    fun restoreFromUserPage_noBackupFound_returnsError() {
        whenever(sessionManager.isUserLoggedIn).thenReturn(true)
        whenever(sessionManager.userName).thenReturn("TestUser")
        whenever(pageEditClient.getCurrentWikiText("User:TestUser/commons-android-app.json"))
            .thenReturn(Single.just(""))

        userSettingsSyncManager
            .restoreFromUserPage()
            .test()
            .assertError(IllegalStateException::class.java)
    }

    @Test
    fun restoreFromUserPage_success() {
        whenever(sessionManager.isUserLoggedIn).thenReturn(true)
        whenever(sessionManager.userName).thenReturn("TestUser")

        val sampleData =
            UserSettingsData(
                preferences = mapOf("appThemePref" to PreferenceValueDto(PrefType.STRING, "1")),
                bookmarks = BookmarksData(),
            )
        val jsonString = gson.toJson(sampleData)

        whenever(pageEditClient.getCurrentWikiText("User:TestUser/commons-android-app.json"))
            .thenReturn(Single.just(jsonString))

        userSettingsSyncManager
            .restoreFromUserPage()
            .test()
            .assertValue(true)
    }
}
