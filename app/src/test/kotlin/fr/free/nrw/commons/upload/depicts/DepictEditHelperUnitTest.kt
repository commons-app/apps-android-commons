package fr.free.nrw.commons.upload.depicts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.utils.ViewUtilWrapper
import fr.free.nrw.commons.wikidata.WikidataEditService
import io.reactivex.Observable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DepictEditHelperUnitTest {

    private lateinit var context: Context
    private lateinit var helper: DepictEditHelper

    @Mock
    private lateinit var notificationHelper: NotificationHelper

    @Mock
    private lateinit var wikidataEditService: WikidataEditService

    @Mock
    private lateinit var viewUtilWrapper: ViewUtilWrapper

    @Mock
    private lateinit var media: Media

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        helper = DepictEditHelper(notificationHelper, wikidataEditService, viewUtilWrapper)
        Whitebox.setInternalState(helper, "viewUtilWrapper", viewUtilWrapper)
        Whitebox.setInternalState(helper, "notificationHelper", notificationHelper)
        Whitebox.setInternalState(helper, "wikidataEditService", wikidataEditService)
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        Assert.assertNotNull(helper)
    }

    @Test
    @Throws(Exception::class)
    fun testMakeDepictEdit() {
        whenever(wikidataEditService.updateDepictsProperty(media.filename, listOf("Q12")))
            .thenReturn(Observable.just(true))
        helper.makeDepictionEdit(context, media, listOf("Q12"))
        Mockito.verify(viewUtilWrapper, Mockito.times(1)).showShortToast(
            context,
            context.getString(R.string.depictions_edit_helper_make_edit_toast)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testShowCoordinatesEditNotificationCaseTrue() {
        whenever(media.depictionIds).thenReturn(listOf("id", "id2"))
        val method: Method = DepictEditHelper::class.java.getDeclaredMethod(
            "showDepictionEditNotification",
            Context::class.java,
            Media::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        Assertions.assertEquals(
            method.invoke(helper, context, media, true),
            true
        )
    }

    @Test
    @Throws(Exception::class)
    fun testShowCoordinatesEditNotificationCaseFalse() {
        val method: Method = DepictEditHelper::class.java.getDeclaredMethod(
            "showDepictionEditNotification",
            Context::class.java,
            Media::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        Assertions.assertEquals(
            method.invoke(helper, context, media, false),
            false
        )
    }


}