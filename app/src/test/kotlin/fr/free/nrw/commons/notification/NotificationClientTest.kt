package fr.free.nrw.commons.notification

import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.notification.models.NotificationType
import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.model.notifications.Notification
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import io.reactivex.Observable
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NotificationClientTest {
    @Mock
    private lateinit var service: NotificationInterface

    @Mock
    private lateinit var csrfTokenClient: CsrfTokenClient

    @Mock
    private lateinit var mQueryResponse: MwQueryResponse

    @Mock
    private lateinit var mQueryResult: MwQueryResult

    @Mock
    private lateinit var mQueryResultNotificationsList: MwQueryResult.NotificationList

    private lateinit var notificationClient: NotificationClient

    /**
     * initial setup, test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        notificationClient = NotificationClient(csrfTokenClient, service)
    }

    /**
     * Test getNotifications
     */

    @Test
    fun getNotificationTest() {
        Mockito
            .`when`(service.getAllNotifications(anyString(), anyString(), any()))
            .thenReturn(Observable.just(mQueryResponse))
        Mockito.`when`(mQueryResponse.query()).thenReturn(mQueryResult)
        Mockito.`when`(mQueryResult.notifications()).thenReturn(mQueryResultNotificationsList)
        Mockito.`when`(mQueryResultNotificationsList.list()).thenReturn(
            listOf(
                createWikimediaNotification(
                    primaryUrl = "foo",
                    compactHeader = "header",
                    timestamp = "2024-01-22T10:12:00Z",
                    notificationId = 1234L,
                ),
            ),
        )

        val result = notificationClient.getNotifications(true).test().values()

        verify(service).getAllNotifications(
            eq("wikidatawiki|commonswiki|enwiki"),
            eq("read"),
            eq(null),
        )

        val notificationList = result.first()
        assertEquals(1, notificationList.size)

        with(notificationList.first()) {
            assertEquals(NotificationType.UNKNOWN, notificationType)
            assertEquals("header", notificationText)
            assertEquals("January 22", date)
            assertEquals("foo", link)
            assertEquals("", iconUrl)
            assertEquals("1234", notificationId)
        }
    }

    /**
     * Test mark Notifications As Read
     */
    @Test
    fun markNotificationAsReadTest() {
        Mockito.`when`(csrfTokenClient.getTokenBlocking()).thenReturn("test")
        Mockito
            .`when`(service.markRead(anyString(), anyString(), anyString()))
            .thenReturn(Observable.just(mQueryResponse))
        Mockito.`when`(mQueryResponse.success()).thenReturn(true)
        notificationClient.markNotificationAsRead("test")
        verify(service).markRead(anyString(), anyString(), anyString())
    }

    @Test
    fun getUnreadCountFollowsContinueToken() {
        val firstPage = Mockito.mock(MwQueryResponse::class.java)
        val firstResult = Mockito.mock(MwQueryResult::class.java)
        val firstNotifications = Mockito.mock(MwQueryResult.NotificationList::class.java)
        val secondPage = Mockito.mock(MwQueryResponse::class.java)
        val secondResult = Mockito.mock(MwQueryResult::class.java)
        val secondNotifications = Mockito.mock(MwQueryResult.NotificationList::class.java)

        Mockito.`when`(firstPage.query()).thenReturn(firstResult)
        Mockito.`when`(firstResult.notifications()).thenReturn(firstNotifications)
        Mockito.`when`(firstNotifications.list()).thenReturn(List(50) { Notification() })
        Mockito.`when`(firstNotifications.continueToken()).thenReturn("page-2")

        Mockito.`when`(secondPage.query()).thenReturn(secondResult)
        Mockito.`when`(secondResult.notifications()).thenReturn(secondNotifications)
        Mockito.`when`(secondNotifications.list()).thenReturn(List(7) { Notification() })
        Mockito.`when`(secondNotifications.continueToken()).thenReturn(null)

        Mockito
            .`when`(service.getAllNotifications(anyString(), eq("!read"), isNull()))
            .thenReturn(Observable.just(firstPage))
        Mockito
            .`when`(service.getAllNotifications(anyString(), eq("!read"), eq("page-2")))
            .thenReturn(Observable.just(secondPage))

        assertEquals(57, notificationClient.getUnreadCount().blockingGet())
    }

    @Suppress("SameParameterValue")
    private fun createWikimediaNotification(
        primaryUrl: String,
        compactHeader: String,
        timestamp: String,
        notificationId: Long,
    ) = Notification().apply {
        setId(notificationId)

        setTimestamp(Notification.Timestamp().apply { setUtciso8601(timestamp) })

        contents =
            Notification.Contents().apply {
                this.compactHeader = compactHeader

                links =
                    Notification.Links().apply {
                        setPrimary(
                            GsonUtil.defaultGson.toJsonTree(
                                Notification.Link().apply { url = primaryUrl },
                            ),
                        )
                    }
            }
    }
}
