package fr.free.nrw.commons.description

import android.content.Context
import fr.free.nrw.commons.models.Media
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.notification.NotificationHelper
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.lang.reflect.Method

class DescriptionEditHelperUnitTest {

    private lateinit var helper: DescriptionEditHelper

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var notificationHelper: NotificationHelper

    @Mock
    private lateinit var pageEditClient: PageEditClient

    @Mock
    private lateinit var media: Media

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        helper = DescriptionEditHelper(notificationHelper, pageEditClient)
    }

    @Test
    fun testAddDescription() {
        `when`(media.filename).thenReturn("")
        `when`(
            pageEditClient.edit(
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(Observable.just(true))
        helper.addDescription(context, media, "test")
        verify(pageEditClient, times(1)).edit(anyString(), anyString(), anyString())
    }

    @Test
    fun testAddCaption() {
        `when`(media.filename).thenReturn("")
        `when`(
            pageEditClient.setCaptions(
                anyString(),
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(Observable.just(0))
        helper.addCaption(context, media, "test", "test")
        verify(pageEditClient, times(1)).setCaptions(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )
    }

    @Test
    fun testShowCaptionEditNotificationCaseFalse() {
        val method: Method = DescriptionEditHelper::class.java.getDeclaredMethod(
            "showCaptionEditNotification", Context::class.java, Media::class.java,
            Int::class.java
        )
        method.isAccessible = true
        assertEquals(method.invoke(helper, context, media, 0), false)
    }

    @Test
    fun testShowCaptionEditNotificationCaseTrue() {
        val method: Method = DescriptionEditHelper::class.java.getDeclaredMethod(
            "showCaptionEditNotification", Context::class.java, Media::class.java,
            Int::class.java
        )
        method.isAccessible = true
        assertEquals(method.invoke(helper, context, media, 1), true)
    }

    @Test
    fun testShowDescriptionEditNotificationCaseFalse() {
        val method: Method = DescriptionEditHelper::class.java.getDeclaredMethod(
            "showDescriptionEditNotification", Context::class.java, Media::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        assertEquals(method.invoke(helper, context, media, false), false)
    }

    @Test
    fun testShowDescriptionEditNotificationCaseTrue() {
        val method: Method = DescriptionEditHelper::class.java.getDeclaredMethod(
            "showDescriptionEditNotification", Context::class.java, Media::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        assertEquals(method.invoke(helper, context, media, true), true)
    }

}