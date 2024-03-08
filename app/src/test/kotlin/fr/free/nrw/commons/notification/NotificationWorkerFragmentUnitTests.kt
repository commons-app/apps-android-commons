package fr.free.nrw.commons.notification

import org.junit.Before
import org.junit.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.notNullValue

class NotificationWorkerFragmentUnitTests {

    private lateinit var fragment: NotificationWorkerFragment

    @Before
    fun setUp() {
        fragment = NotificationWorkerFragment()
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        assertThat(fragment, notNullValue())
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