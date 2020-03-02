package fr.free.nrw.commons.notification

import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.notifications.Notification

class NotificationClientTest {

    @Mock
    private lateinit var service: Service
    @Mock
    private lateinit var csrfTokenClient: CsrfTokenClient

    @Mock
    private lateinit var mQueryResponse: MwQueryResponse
    @Mock
    private lateinit var mQueryResult: MwQueryResult
    @Mock
    private lateinit var mQueryResultNotificationsList: MwQueryResult.NotificationList
    @Mock
    private lateinit var notificationsList: List<Notification>

    private lateinit var notificationClient: NotificationClient

    /**
     * initial setup, test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        notificationClient = NotificationClient(service, csrfTokenClient)
    }

    /**
     * Test getNotifications
     */

    @Test
    fun getNotificationTest() {
        Mockito.`when`(service.getAllNotifications(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(Observable.just(mQueryResponse))
        Mockito.`when`(mQueryResponse.query()).thenReturn(mQueryResult)
        Mockito.`when`(mQueryResult.notifications()).thenReturn(mQueryResultNotificationsList)
        Mockito.`when`(mQueryResultNotificationsList.list()).thenReturn(notificationsList)
        notificationClient.getNotifications(true)
        verify(service).getAllNotifications(eq("wikidatawiki|commonswiki|enwiki"), eq("read"), eq(null))
    }

    /**
     * Test mark Notifications As Read
     */
    @Test
    fun markNotificationAsReadTest() {
        Mockito.`when`(csrfTokenClient.tokenBlocking).thenReturn("test")
        Mockito.`when`(service.markRead(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(Observable.just(mQueryResponse))
        Mockito.`when`(mQueryResponse.success()).thenReturn(true)
        notificationClient.markNotificationAsRead("test")
        verify(service).markRead(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
    }

}