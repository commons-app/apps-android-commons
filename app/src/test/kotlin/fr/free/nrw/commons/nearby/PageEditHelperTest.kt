package fr.free.nrw.commons.nearby

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.utils.ViewUtilWrapper
import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks the reports that Nearby adds to Wikidata talk pages: each report is a new section that
 * keeps the rest of the page, and it is marked with a template that volunteers can search for.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class PageEditHelperTest {
    private lateinit var context: Context
    private lateinit var pageEditClient: PageEditClient
    private lateinit var helper: PageEditHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pageEditClient = mock()
        whenever(pageEditClient.createNewSection(any(), any(), any(), any())).thenReturn(Observable.just(true))
        helper = PageEditHelper(mock<NotificationHelper>(), pageEditClient, mock<ViewUtilWrapper>(), "Someone")
    }

    private fun report(
        description: String = "'Old mill' does not exist anymore",
        details: String = "It was demolished in 2020",
    ) = PageEditHelper.buildReportText(description, details, 48.635769, -1.510118)

    @Test
    fun `report starts with the template for open reports`() {
        assertTrue(report().startsWith("{{app-report-todo}}\n"))
    }

    @Test
    fun `report keeps the problem, details, location and signature`() {
        val text = report()

        assertTrue(text.contains("(48.635769,-1.510118)"))
        assertTrue(text.contains("* <i><nowiki>'Old mill' does not exist anymore</nowiki></i>"))
        assertTrue(text.contains("Details: <i><nowiki>It was demolished in 2020</nowiki></i>"))
        assertTrue(text.endsWith("\n\n~~~~"))
    }

    @Test
    fun `report uses each template only once outside nowiki`() {
        val text = report()
        val outsideNowiki = text.replace(Regex("<nowiki>.*?</nowiki>"), "")

        // The open template is used once, the done template is only named in the instructions
        assertEquals(1, Regex("\\{\\{app-report-todo}}").findAll(outsideNowiki).count())
        assertFalse(outsideNowiki.contains("app-report-done"))
        assertTrue(text.contains("<nowiki>{{app-report-todo}}</nowiki> to <nowiki>{{app-report-done}}</nowiki>"))
    }

    @Test
    fun `report is added as a new section without replacing the talk page`() {
        helper
            .makePageEdit(context, "Talk:Q22963302", "'Old mill' does not exist anymore", "details", 1.0, 2.0)
            .test()
            .assertValue(true)

        val text = argumentCaptor<String>()
        verify(pageEditClient).createNewSection(
            eq("Talk:Q22963302"),
            eq("Please fix this item"),
            text.capture(),
            eq("Please fix this item"),
        )
        val expected = PageEditHelper.buildReportText("'Old mill' does not exist anymore", "details", 1.0, 2.0)
        assertEquals(expected, text.firstValue)
        verify(pageEditClient, never()).postCreate(any(), any(), any())
        verify(pageEditClient, never()).edit(any(), any(), any())
    }

    @Test
    fun `second report on the same item gets its own section`() {
        helper.makePageEdit(context, "Talk:Q1", "First problem", "first", 1.0, 2.0).test().assertValue(true)
        helper.makePageEdit(context, "Talk:Q1", "Second problem", "second", 1.0, 2.0).test().assertValue(true)

        val text = argumentCaptor<String>()
        verify(pageEditClient, times(2)).createNewSection(eq("Talk:Q1"), any(), text.capture(), any())
        assertTrue(text.secondValue.contains("Second problem"))
        // Each section only holds its own report, the first one stays as it was
        assertFalse(text.secondValue.contains("First problem"))
    }

    @Test
    fun `failed edit is reported`() {
        whenever(pageEditClient.createNewSection(any(), any(), any(), any())).thenReturn(Observable.just(false))

        helper.makePageEdit(context, "Talk:Q1", "problem", "details", 1.0, 2.0).test().assertValue(false)
    }

    @Test
    fun `edit error is passed on`() {
        whenever(pageEditClient.createNewSection(any(), any(), any(), any()))
            .thenReturn(Observable.error(RuntimeException("Failed to create the new section")))

        helper
            .makePageEdit(context, "Talk:Q1", "problem", "details", 1.0, 2.0)
            .test()
            .assertError(RuntimeException::class.java)
    }
}
