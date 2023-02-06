package fr.free.nrw.commons.coordinates

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.utils.ViewUtilWrapper
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class CoordinateEditHelperUnitTest {

    private lateinit var context: Context
    private lateinit var helper: CoordinateEditHelper

    @Mock
    private lateinit var notificationHelper: NotificationHelper

    @Mock
    private lateinit var pageEditClient: PageEditClient

    @Mock
    private lateinit var viewUtilWrapper: ViewUtilWrapper

    @Mock
    private lateinit var media: Media

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        helper = CoordinateEditHelper(notificationHelper, pageEditClient, viewUtilWrapper)
        `when`(media.filename).thenReturn("")
        `when`(pageEditClient.getCurrentWikiText(anyString())).thenReturn(Single.just(""))
        `when`(
            pageEditClient.edit(
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(Observable.just(true))
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        Assert.assertNotNull(helper)
    }

    @Test
    @Throws(Exception::class)
    fun testMakeCoordinatesEdit() {
        helper.makeCoordinatesEdit(context, media, "0.0", "0.0", "0.0F")
        verify(viewUtilWrapper, times(1)).showShortToast(
            context,
            context.getString(R.string.coordinates_edit_helper_make_edit_toast)
        )
        verify(pageEditClient, times(1)).getCurrentWikiText(anyString())
        verify(pageEditClient, times(1)).edit(anyString(), anyString(), anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testGetFormattedWikiTextCaseNewLineContainsLocation() {
        val method: Method = CoordinateEditHelper::class.java.getDeclaredMethod(
            "getFormattedWikiText", String::class.java, String::class.java
        )
        method.isAccessible = true
        assertEquals(
            method.invoke(
                helper,
                "== {{int:filedesc}} == {{user|Test}} == \n{{Location|0.0|0.0|0.0F}}",
                "{{Location|0.1|0.1|0.1F}}"
            ), "== {{int:filedesc}} == {{user|Test}} == \n" +
                    "{Location|0.1|0.1|0.1F}}\n"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetFormattedWikiTextCaseContainsLocation() {
        val method: Method = CoordinateEditHelper::class.java.getDeclaredMethod(
            "getFormattedWikiText", String::class.java, String::class.java
        )
        method.isAccessible = true
        assertEquals(
            method.invoke(
                helper,
                "== {{int:filedesc}} == {{Location|0.0|0.0|0.0F}}",
                "{{Location|0.1|0.1|0.1F}}"
            ), "== {{int:filedesc}} == {{Location|0.1|0.1|0.1F}}"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetFormattedWikiTextCaseDoesContainsLocationHasSubString() {
        val method: Method = CoordinateEditHelper::class.java.getDeclaredMethod(
            "getFormattedWikiText", String::class.java, String::class.java
        )
        method.isAccessible = true
        assertEquals(
            method.invoke(
                helper,
                "== {{int:filedesc}} == {{user|Test}} == {{int:license-header}} ==",
                "{{Location|0.1|0.1|0.1F}}"
            ),
            "== {{int:filedesc}} == {{user|Test}} {Location|0.1|0.1|0.1F}}\n" +
                    "== {{int:license-header}} =="
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetFormattedWikiTextCaseDoesContainsLocationDoesNotHaveSubString() {
        val method: Method = CoordinateEditHelper::class.java.getDeclaredMethod(
            "getFormattedWikiText", String::class.java, String::class.java
        )
        method.isAccessible = true
        assertEquals(
            method.invoke(
                helper,
                "== {{int:filedesc}} {{int:license-header}} ==",
                "{{Location|0.1|0.1|0.1F}}"
            ),
            "== {{int:filedesc}} {{int:license-header}} =={{Location|0.1|0.1|0.1F}}"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetFormattedWikiTextCaseDoesNotContainFileDesc() {
        val method: Method = CoordinateEditHelper::class.java.getDeclaredMethod(
            "getFormattedWikiText", String::class.java, String::class.java
        )
        method.isAccessible = true
        assertEquals(
            method.invoke(
                helper,
                "{{Location|0.0|0.0|0.0F}}",
                "{{Location|0.1|0.1|0.1F}}"
            ), "== {{int:filedesc}} =={{Location|0.1|0.1|0.1F}}{{Location|0.0|0.0|0.0F}}"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testShowCoordinatesEditNotificationCaseTrue() {
        val method: Method = CoordinateEditHelper::class.java.getDeclaredMethod(
            "showCoordinatesEditNotification", Context::class.java, Media::class.java,
            String::class.java, String::class.java, String::class.java, Boolean::class.java
        )
        method.isAccessible = true
        assertEquals(method.invoke(helper, context, media, "0.0", "0.0", "0.0F", true), true)
    }

    @Test
    @Throws(Exception::class)
    fun testShowCoordinatesEditNotificationCaseFalse() {
        val method: Method = CoordinateEditHelper::class.java.getDeclaredMethod(
            "showCoordinatesEditNotification", Context::class.java, Media::class.java,
            String::class.java, String::class.java, String::class.java, Boolean::class.java
        )
        method.isAccessible = true
        assertEquals(method.invoke(helper, context, media, "0.0", "0.0", "0.0F", false), false)
    }

}