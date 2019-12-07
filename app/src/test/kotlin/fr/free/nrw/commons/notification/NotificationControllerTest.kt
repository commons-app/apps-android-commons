package fr.free.nrw.commons.notification

import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class NotificationControllerTest {

    @Mock
    private lateinit var notificationClient: NotificationClient
    @Mock
    private lateinit var notification: Notification
    private lateinit var notificationController: NotificationController

    /**
     * initial setup, test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        notificationController = NotificationController(notificationClient)
    }

    /**
     * Test get notifications
     */
    @Test
    fun testGetNotifications() {
        notificationController.getNotifications(ArgumentMatchers.anyBoolean())
        verify(notificationClient).getNotifications(ArgumentMatchers.anyBoolean())
    }

    /**
     * Test mark notifications as read
     */
    @Test
    fun testMarkNotificationsAsRead() {
        `when`(notification.notificationId).thenReturn("test")
        notificationController.markAsRead(notification)
        verify(notificationClient).markNotificationAsRead(eq("test"))
    }
}