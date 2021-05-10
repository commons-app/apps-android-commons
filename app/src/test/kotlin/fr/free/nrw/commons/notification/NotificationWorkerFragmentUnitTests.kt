package fr.free.nrw.commons.notification

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class NotificationWorkerFragmentUnitTests {

    private lateinit var fragment: NotificationWorkerFragment

    @Before
    fun setUp() {
        fragment = NotificationWorkerFragment()
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreate() {
        fragment.onCreate(null)
    }

    @Test
    @Throws(Exception::class)
    fun testSetNotificationList() {
        fragment.notificationList = null
    }

    @Test
    @Throws(Exception::class)
    fun testGetNotificationList() {
        fragment.notificationList
    }

}