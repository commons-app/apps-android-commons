package fr.free.nrw.commons.explore.depictions

import android.content.Context
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.wikidata.WikipediaArticleInterface
import fr.free.nrw.commons.wikidata.model.WikipediaArticleResponse
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.net.URLEncoder
import java.util.Locale

class WikipediaArticleClientTest {
    @Mock
    private lateinit var wikipediaArticleInterface: WikipediaArticleInterface

    @Mock
    private lateinit var context: Context

    private lateinit var client: WikipediaArticleClient

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        client = WikipediaArticleClient(wikipediaArticleInterface, context)
        whenever(wikipediaArticleInterface.getPageProps(anyString())).thenReturn(Single.error(Exception("not found")))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `findUnlinkedArticle returns exact title without a Wikidata item`() {
        stubPage("en", "Test Article", wikidataId = null)

        client.findUnlinkedArticle("Test Article").test().assertValue(
            WikipediaArticleClient.Article("en", "Test Article"),
        )
    }

    @Test
    fun `findUnlinkedArticle prefers the device language`() {
        stubPage("en", "Test Article")
        stubPage("ja", "Test Article")

        client.findUnlinkedArticle("Test Article").test().assertValue(
            WikipediaArticleClient.Article("en", "Test Article"),
        )
    }

    @Test
    fun `findUnlinkedArticle ignores articles that already have a Wikidata item`() {
        stubPage("en", "Test Article", wikidataId = "Q42")

        client
            .findUnlinkedArticle("Test Article")
            .test()
            .assertComplete()
            .assertNoValues()
    }

    @Test
    fun `findUnlinkedArticle ignores missing pages and other namespaces`() {
        stubPage("en", "Test Article", missing = "")
        stubPage("ja", "Test Article", ns = 1)

        client
            .findUnlinkedArticle("Test Article")
            .test()
            .assertComplete()
            .assertNoValues()
    }

    @Test
    fun `pending depiction id round trip keeps language and title`() {
        val article = WikipediaArticleClient.Article("ja", "現代革命勞衝者党: party")

        val id = client.pendingDepictionId(article)

        assert(client.isPendingDepictionId(id))
        assert(client.articleFromPendingDepictionId(id) == article)
        assert(client.articleFromPendingDepictionId("Q42") == null)
    }

    @Test
    fun `descriptionFor uses localized language name`() {
        whenever(context.getString(R.string.depicts_unlinked_article_description, "Japanese"))
            .thenReturn("description")

        assert(client.descriptionFor("ja") == "description")
    }

    private fun stubPage(
        languageCode: String,
        title: String,
        ns: Int = 0,
        missing: String? = null,
        wikidataId: String? = null,
    ) {
        val encodedTitle = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
        val url =
            "https://$languageCode.wikipedia.org/w/api.php?action=query&format=json&prop=pageprops&titles=$encodedTitle"
        val page =
            WikipediaArticleResponse.Page(
                title = title,
                ns = ns,
                missing = missing,
                pageprops = wikidataId?.let { WikipediaArticleResponse.PageProps(it) },
            )
        val response = WikipediaArticleResponse(WikipediaArticleResponse.Query(mapOf("123" to page)))
        whenever(wikipediaArticleInterface.getPageProps(url)).thenReturn(Single.just(response))
    }
}
