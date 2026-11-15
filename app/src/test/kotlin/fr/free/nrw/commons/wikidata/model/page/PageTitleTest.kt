package fr.free.nrw.commons.wikidata.model.page

import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.wikidata.model.WikiSite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class PageTitleTest {
    private val commons = WikiSite("commons.wikimedia.org")

    @Test
    fun fileTitleIsSplitIntoNamespaceAndText() {
        val title = PageTitle("File:Example.jpg", commons)
        assertEquals("File:Example.jpg", title.prefixedText)
        assertEquals("Example.jpg", title.displayTextWithoutNamespace)
        assertNull(title.fragment)
    }

    @Test
    fun onlyTheFirstColonSeparatesTheNamespace() {
        val title = PageTitle("Category:Foo: Bar", commons)
        assertEquals("Category:Foo:_Bar", title.prefixedText)
        assertEquals("Foo: Bar", title.displayTextWithoutNamespace)
    }

    @Test
    fun spacesBecomeUnderscoresOnlyInThePrefixedText() {
        val title = PageTitle("File:My example file.jpg", commons)
        assertEquals("File:My_example_file.jpg", title.prefixedText)
        assertEquals("File:My example file.jpg", title.displayText)
    }

    @Test
    fun fragmentIsSeparatedFromTheTitle() {
        val title = PageTitle("File:Example.jpg#Summary section", commons)
        assertEquals("File:Example.jpg", title.prefixedText)
        assertEquals("Summary_section", title.fragment)
    }

    @Test
    fun titleWithoutColonHasNoNamespace() {
        val title = PageTitle("Example", commons)
        assertEquals("Example", title.prefixedText)
        assertEquals("Example", title.displayTextWithoutNamespace)
    }

    @Test
    fun languageCodePrefixSelectsTheLanguageInsteadOfANamespace() {
        val title = PageTitle("de:Berlin", commons)
        assertEquals("Berlin", title.prefixedText)
        assertEquals("de", title.wikiSite.languageCode())
    }

    @Test
    fun separateFragmentIsAddedToTheTitle() {
        val title = PageTitle.withSeparateFragment("Category:Example", "Some heading", commons)
        assertEquals("Category:Example", title.prefixedText)
        assertEquals("Some_heading", title.fragment)
    }
}
