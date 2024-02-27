package fr.free.nrw.commons.notification

import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.notification.models.NotificationType
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.model.notifications.Notification
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo

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
        Mockito.`when`(service.getAllNotifications(anyString(), anyString(), any()))
            .thenReturn(Observable.just(mQueryResponse))
        Mockito.`when`(mQueryResponse.query()).thenReturn(mQueryResult)
        Mockito.`when`(mQueryResult.notifications()).thenReturn(mQueryResultNotificationsList)
        Mockito.`when`(mQueryResultNotificationsList.list()).thenReturn(
            listOf(
                createWikimediaNotification(
                    primaryUrl = "foo",
                    compactHeader = "header",
                    timestamp = "2024-01-22T10:12:00Z",
                    notificationId = 1234L
                )
            )
        )

        val result = notificationClient.getNotifications(true).test().values()

        verify(service).getAllNotifications(
            eq("wikidatawiki|commonswiki|enwiki"),
            eq("read"),
            eq(null)
        )

        val notificationList = result.first()
        assertThat(1, equalTo( notificationList.size))

        with(notificationList.first()) {
            assertThat(NotificationType.UNKNOWN, equalTo( notificationType))
            assertThat("header", equalTo( notificationText))
            assertThat("January 22", equalTo( date))
            assertThat("foo", equalTo( link))
            assertThat("", equalTo( iconUrl))
            assertThat("1234", equalTo( notificationId))
        }
    }

    /**
     * Test mark Notifications As Read
     */
    @Test
    fun markNotificationAsReadTest() {
        Mockito.`when`(csrfTokenClient.getTokenBlocking()).thenReturn("test")
        Mockito.`when`(service.markRead(anyString(), anyString(), anyString()))
            .thenReturn(Observable.just(mQueryResponse))
        Mockito.`when`(mQueryResponse.success()).thenReturn(true)
        notificationClient.markNotificationAsRead("test")
        verify(service).markRead(anyString(), anyString(), anyString())
    }

    @Suppress("SameParameterValue")
    private fun createWikimediaNotification(
        primaryUrl: String, compactHeader: String, timestamp: String, notificationId: Long
    ) = Notification().apply {
        setId(notificationId)

        setTimestamp(Notification.Timestamp().apply {
            setUtciso8601(timestamp)
        })

        contents = Notification.Contents().apply {
            setCompactHeader(compactHeader)

            links = Notification.Links().apply {
                setPrimary(
                    GsonUtil.getDefaultGson().toJsonTree(Notification.Link().apply {
                        setUrl(primaryUrl)
                    })
                )
            }
        }
    }
}