package fr.free.nrw.commons.category

import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

/**
 * Replaces categories that only redirect to another category by the category they redirect to,
 * so that an upload is not saved in a redirect category.
 *
 * Commons mostly uses "soft" category redirects: the redirect category holds a
 * {{Category redirect}} template and is listed in "Category:Category redirects", and the
 * category it points to is the only category it links to. This is the rule HotCat uses.
 * Hard redirects and title normalization are resolved by MediaWiki itself.
 */
class CategoryRedirectResolver
    @Inject
    constructor(
        private val categoryInterface: CategoryInterface,
    ) {
        /**
         * Resolves the given category names, without the "Category:" prefix.
         *
         * Names that are not redirects are kept as they are, in the same order. When two names
         * end up as the same category, it is kept only once. If the redirects cannot be checked,
         * for example without a network connection, the names are returned unchanged.
         *
         * @param categories category names, such as "Aerial photos of California"
         * @return the resolved category names, such as "Aerial photographs of California"
         */
        fun resolve(categories: List<String>): Single<List<String>> {
            if (categories.isEmpty()) {
                return Single.just(categories)
            }
            return Single
                .fromCallable { resolveNow(categories) }
                .onErrorReturn { throwable ->
                    Timber.w(throwable, "Could not check category redirects, keeping the categories as they are")
                    categories
                }
        }

        private fun resolveNow(categories: List<String>): List<String> {
            val titles = categories.map(::toTitle).distinct()
            val resolvedTitles = mutableMapOf<String, String>()
            val softRedirects = mutableSetOf<String>()

            titles.chunked(MAX_TITLES_PER_REQUEST).forEach { chunk ->
                val query =
                    categoryInterface
                        .getCategoryRedirectInfo(chunk.joinToString("|"), CATEGORY_REDIRECTS)
                        .blockingGet()
                        .query ?: return@forEach
                val normalized = query.normalized.toTitleMap()
                val hardRedirects = query.redirects.toTitleMap()
                chunk.forEach { title ->
                    val normalizedTitle = normalized[title] ?: title
                    resolvedTitles[title] = hardRedirects[normalizedTitle] ?: normalizedTitle
                }
                query.pages
                    .orEmpty()
                    .filter { page -> page.categories.orEmpty().any { it.title == CATEGORY_REDIRECTS } }
                    .mapNotNullTo(softRedirects) { it.title }
            }

            val softRedirectTargets = findSoftRedirectTargets(softRedirects)

            return categories
                .map { category ->
                    val title = resolvedTitles[toTitle(category)] ?: toTitle(category)
                    (softRedirectTargets[title] ?: title).removePrefix(CATEGORY_PREFIX)
                }.distinct()
        }

        /**
         * Finds the category each soft redirect points to. A redirect that links to more than one
         * category is ambiguous and is left out, so the category is kept as it is.
         */
        private fun findSoftRedirectTargets(softRedirects: Set<String>): Map<String, String> {
            val targets = mutableMapOf<String, String>()
            softRedirects.toList().chunked(MAX_TITLES_PER_REQUEST).forEach { chunk ->
                categoryInterface
                    .getCategoryLinks(chunk.joinToString("|"))
                    .blockingGet()
                    .query
                    ?.pages
                    .orEmpty()
                    .forEach { page ->
                        val links =
                            page.links
                                .orEmpty()
                                .mapNotNull { it.title }
                                .distinct()
                        if (page.title != null && links.size == 1) {
                            targets[page.title] = links.single()
                        }
                    }
            }
            return targets
        }

        private fun List<CategoryRedirectResponse.TitleMapping>?.toTitleMap(): Map<String, String> =
            orEmpty()
                .mapNotNull { mapping ->
                    val from = mapping.from ?: return@mapNotNull null
                    val to = mapping.to ?: return@mapNotNull null
                    from to to
                }.toMap()

        private fun toTitle(category: String): String =
            if (category.startsWith(CATEGORY_PREFIX)) category else CATEGORY_PREFIX + category

        companion object {
            /** The category that lists every soft category redirect on Commons */
            const val CATEGORY_REDIRECTS = "Category:Category redirects"

            private const val CATEGORY_PREFIX = "Category:"

            /** MediaWiki accepts at most 50 titles per query for normal accounts */
            private const val MAX_TITLES_PER_REQUEST = 50
        }
    }
