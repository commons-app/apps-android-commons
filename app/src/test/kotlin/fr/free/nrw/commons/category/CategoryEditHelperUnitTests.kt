package fr.free.nrw.commons.category

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
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class CategoryEditHelperUnitTests {

    private lateinit var context: Context
    private lateinit var helper: CategoryEditHelper

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
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        helper = CategoryEditHelper(notificationHelper, pageEditClient, viewUtilWrapper,
            "")
        Mockito.`when`(media.filename).thenReturn("File:Example.jpg")
        Mockito.`when`(pageEditClient.getCurrentWikiText(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(""))
        Mockito.`when`(
            pageEditClient.edit(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
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
    fun testMakeCategoryEdit() {
        helper.makeCategoryEdit(context, media, listOf("Test"), "[[Category:Test]]")
        Mockito.verify(viewUtilWrapper, Mockito.times(1)).showShortToast(
            context,
            context.getString(R.string.category_edit_helper_make_edit_toast)
        )
        Mockito.verify(pageEditClient, Mockito.times(1)).edit(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString()
        )
    }

    @Test
    @Throws(Exception::class)
    fun testMakeUnCategoryEdit() {
        helper.makeCategoryEdit(context, media, listOf("Test"), "== {{int:filedesc}} ==\n" +
                "{{Information\n" +
                "|description=\n" +
                "\n" +
                "|source={{own}}\n" +
                "|author=[[User:UserForthBeta|UserForthBeta]]\n" +
                "|date={{According to Exif data|2022-03-06}}\n" +
                "}}\n" +
                "{{Location|22.538830555555556|114.08847222222222}}\n" +
                "== {{int:license-header}} ==\n" +
                "{{self|cc-zero}}\n" +
                "\n" +
                "{{Uploaded from Mobile|platform=Android|version=3.1.1-debug-master~e7a9ba9ad}}\n" +
                "{{Uncategorized|year=2022|month=April|day=23}}")
        Mockito.verify(viewUtilWrapper, Mockito.times(1)).showShortToast(
            context,
            context.getString(R.string.category_edit_helper_make_edit_toast)
        )
        Mockito.verify(pageEditClient, Mockito.times(1)).edit(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString()
        )
    }
}