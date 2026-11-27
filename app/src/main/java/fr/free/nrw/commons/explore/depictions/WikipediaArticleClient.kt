package fr.free.nrw.commons.explore.depictions

import android.content.Context
import fr.free.nrw.commons.R
import fr.free.nrw.commons.wikidata.WikipediaArticleInterface
import io.reactivex.Flowable
import io.reactivex.Maybe
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
        /** A Wikipedia article identified by its language and exact title. */
        data class Article(
            val languageCode: String,
            val title: String,
        )

        /**
         * Finds an existing article with exactly this title that has no Wikidata item. The device
         * language is checked first so that the most relevant article is returned.
         */
        fun findUnlinkedArticle(title: String): Maybe<Article> {
            if (title.isBlank()) {
                return Maybe.empty()
            }
            val languages = (listOf(Locale.getDefault().language) + WIKIPEDIA_LANGUAGES).distinct()
            return Flowable
                .fromIterable(languages)
                .concatMapMaybe { language -> findOnWikipedia(language, title) }
                .firstElement()
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

            private val WIKIPEDIA_LANGUAGES =
                listOf(
                    "en",
                    "ja",
                    "de",
                    "fr",
                    "es",
                    "ru",
                    "it",
                    "pt",
                    "zh",
                    "nl",
                    "pl",
                    "ar",
                    "sv",
                    "uk",
                    "ko",
                    "hi",
                    "tr",
                    "fa",
                    "cs",
                    "vi",
                    "id",
                    "no",
                    "fi",
                    "da",
                    "el",
                    "he",
                    "hu",
                    "ro",
                    "th",
                    "bg",
                )
        }
    }
