package fr.free.nrw.commons.explore.depictions

import android.content.Context
import com.google.gson.JsonObject
import fr.free.nrw.commons.R
import fr.free.nrw.commons.wikidata.WikipediaArticleInterface
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Looks up Wikipedia articles that do not have a Wikidata item yet.
 */
@Singleton
class WikipediaArticleClient
    @Inject
    constructor(
        private val wikipediaArticleInterface: WikipediaArticleInterface,
        private val context: Context,
    ) {
        @Volatile
        private var cachedLanguages: List<String>? = null

        /** A Wikipedia article identified by its language and exact title. */
        data class Article(
            val languageCode: String,
            val title: String,
        )

        /**
         * Finds an existing article with exactly this title that has no Wikidata item. The device
         * language is checked first, then every other Wikipedia language edition.
         */
        fun findUnlinkedArticle(title: String): Maybe<Article> {
            if (title.isBlank()) {
                return Maybe.empty()
            }
            return wikipediaLanguages()
                .flatMapMaybe { languages -> lookupArticle(title, languages).firstElement() }
                .onErrorResumeNext { _: Throwable -> Maybe.empty() }
        }

        /** Builds the temporary depiction ID used before the Wikidata item is created. */
        fun pendingDepictionId(article: Article): String = "$PENDING_PREFIX${article.languageCode}:${article.title}"

        /** Returns true if this ID belongs to a Wikipedia article whose item is not created yet. */
        fun isPendingDepictionId(id: String): Boolean = id.startsWith(PENDING_PREFIX)

        /** Reads the language and title back from a temporary depiction ID. */
        fun articleFromPendingDepictionId(id: String): Article? =
            if (!isPendingDepictionId(id)) {
                null
            } else {
                val withoutPrefix = id.removePrefix(PENDING_PREFIX)
                val separator = withoutPrefix.indexOf(':')
                if (separator <= 0 || separator == withoutPrefix.lastIndex) {
                    null
                } else {
                    Article(withoutPrefix.substring(0, separator), withoutPrefix.substring(separator + 1))
                }
            }

        /** Describes an unlinked article in the user's language. */
        fun descriptionFor(languageCode: String): String =
            context.getString(
                R.string.depicts_unlinked_article_description,
                Locale.forLanguageTag(languageCode).displayLanguage,
            )

        private fun wikipediaLanguages(): Single<List<String>> =
            cachedLanguages
                ?.let { Single.just(it) }
                ?: wikipediaArticleInterface
                    .getSiteMatrix(SITE_MATRIX_URL)
                    .map(::languageCodes)
                    .doOnSuccess { cachedLanguages = it }
                    .onErrorReturnItem(emptyList())

        private fun languageCodes(matrix: JsonObject): List<String> {
            val siteMatrix = matrix.getAsJsonObject("sitematrix") ?: return emptyList()
            return siteMatrix.entrySet().mapNotNull { (key, value) ->
                if (key == "count" || key == "total" || !value.isJsonObject) {
                    return@mapNotNull null
                }
                val language = value.asJsonObject
                val codeElement = language.get("code") ?: return@mapNotNull null
                if (!codeElement.isJsonPrimitive) {
                    return@mapNotNull null
                }
                val sites = language.getAsJsonArray("site") ?: return@mapNotNull null
                val hasWikipedia =
                    sites.any { site ->
                        site.isJsonObject &&
                            site
                                .asJsonObject
                                .get("url")
                                ?.asString
                                .orEmpty()
                                .contains("wikipedia.org")
                    }
                codeElement.asString.takeIf { hasWikipedia }
            }
        }

        private fun lookupArticle(
            title: String,
            languages: List<String>,
        ): Flowable<Article> {
            val deviceLanguage = Locale.getDefault().language
            val primary = languages.filter { it == deviceLanguage }
            val remaining = languages.filterNot { it == deviceLanguage }
            return Flowable.concat(
                Flowable.fromIterable(primary).concatMapMaybe { findOnWikipedia(it, title) },
                Flowable
                    .fromIterable(remaining)
                    .flatMap({ findOnWikipedia(it, title).toFlowable() }, false, MAX_CONCURRENT_LOOKUPS),
            )
        }

        private fun findOnWikipedia(
            language: String,
            title: String,
        ): Maybe<Article> =
            wikipediaArticleInterface
                .getPageProps(pagePropsUrl(language, title))
                .flatMapMaybe { response ->
                    val page =
                        response.query
                            ?.pages
                            ?.values
                            ?.firstOrNull {
                                it.isExistingArticle() &&
                                    it.wikidataId() == null &&
                                    it.title.equals(title, ignoreCase = true)
                            }
                    if (page == null) {
                        Maybe.empty()
                    } else {
                        Maybe.just(Article(language, page.title!!))
                    }
                }.onErrorResumeNext { _: Throwable -> Maybe.empty<Article>() }

        private fun pagePropsUrl(
            language: String,
            title: String,
        ): String {
            val encodedTitle = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
            return "https://$language.wikipedia.org/w/api.php?action=query&format=json" +
                "&prop=pageprops&titles=$encodedTitle"
        }

        companion object {
            private const val PENDING_PREFIX = "pending-wikipedia:"
            private const val MAX_CONCURRENT_LOOKUPS = 16
            private const val SITE_MATRIX_URL =
                "https://www.wikidata.org/w/api.php?action=sitematrix&smtype=language&format=json"
        }
    }
