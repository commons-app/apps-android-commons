package fr.free.nrw.commons.nearby

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.utils.ViewUtilWrapper
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.mockito.Mockito.mock

class PageEditHelperTest {

    private val notificationHelper: NotificationHelper = mock()
    private val pageEditClient: PageEditClient = mock()
    private val viewUtilWrapper: ViewUtilWrapper = mock()
    val testObject = PageEditHelper(notificationHelper, pageEditClient, viewUtilWrapper)
    private val titleCaptor = argumentCaptor<String>()
    private val textCaptor = argumentCaptor<String>()
    private val summaryCaptor = argumentCaptor<String>()

    @Test
    fun editPage_noMarkerPresent() {
        whenever(pageEditClient.postCreate(titleCaptor.capture(), textCaptor.capture(), summaryCaptor.capture())).thenReturn(mock())

        val observable = testObject.editPage("title", "pre-text", "desc", "details", 1.0, 2.0)
        assertNotNull(observable)

        verify(pageEditClient).postCreate(anyOrNull(), anyOrNull(), anyOrNull())

        assertEquals("title", titleCaptor.firstValue)
        assertEquals("==Please fix this item==\n" +
                "Someone using the [[Commons:Mobile_app|Commons Android app]] went to this item's geographical location (1.0,2.0) and noted the following problem(s):\n" +
                "* <i><nowiki>desc</nowiki></i>\n" +
                "\n" +
                "Details: <i><nowiki>details</nowiki></i>\n" +
                "\n" +
                "Please anyone fix the item accordingly, then reply to mark this section as fixed. Thanks a lot for your cooperation!\n" +
                "\n" +
                "~~~~", textCaptor.firstValue)
        assertEquals("Please fix this item", summaryCaptor.firstValue)
    }

    @Test
    fun editPage_withMarkerPresent() {
        whenever(pageEditClient.postCreate(titleCaptor.capture(), textCaptor.capture(), summaryCaptor.capture())).thenReturn(mock())

        val preText = "pre-text\n" +
                "Please anyone fix the item accordingly, then reply to mark this section as fixed. Thanks a lot for your cooperation!"
        val observable = testObject.editPage("title", preText, "desc", "details", 1.0, 2.0)
        assertNotNull(observable)

        verify(pageEditClient).postCreate(anyOrNull(), anyOrNull(), anyOrNull())

        assertEquals("title", titleCaptor.firstValue)
        assertEquals("pre-text\n" +
                "* <i><nowiki>desc</nowiki></i>\n" +
                "\n" +
                "Details: <i><nowiki>details</nowiki></i>\n" +
                "\n" +
                "Please anyone fix the item accordingly, then reply to mark this section as fixed. Thanks a lot for your cooperation!\n" +
                "\n" +
                "~~~~", textCaptor.firstValue)
        assertEquals("Please fix this item", summaryCaptor.firstValue)
    }

}