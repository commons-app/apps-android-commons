package fr.free.nrw.commons.mwapi

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.profile.achievements.FeaturedImages
import fr.free.nrw.commons.profile.achievements.FeedbackResponse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

/**
 * Usage and award counts for the files a user currently has.
 */
private data class CommonsFileUsage(
    val uniqueUsedImages: Int,
    val articlesUsingImages: Int,
    val featuredImages: Int,
    val qualityImages: Int,
)

/** Computes upload and achievement statistics when Toolforge feedback is unavailable. */
class CommonsUserStats
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
    ) {
        private companion object {
            // Each batch of titles costs another Commons API request, so heavy uploaders keep the error.
            private const val MAX_FALLBACK_UPLOADS = 1000
            private const val TITLES_PER_QUERY = 50
            private const val FEATURED_PICTURES_CATEGORY = "Featured_pictures_on_Wikimedia_Commons"
            private const val QUALITY_IMAGES_CATEGORY = "Quality_images"
        }

        /**
         * Current files for [userName], at most [MAX_FALLBACK_UPLOADS].
         * Contributions requests this on launch, so a prolific uploader must not page the whole list.
         */
        internal fun countUploadsFromCommonsApi(userName: String): Int {
            var uploads = 0
            queryCommonsApi(
                mapOf(
                    "list" to "allimages",
                    "aiuser" to userName,
                    "aisort" to "timestamp",
                    "aiprop" to "timestamp",
                    "ailimit" to "500",
                ),
                stopAfterBatch = { uploads >= MAX_FALLBACK_UPLOADS },
            ) { query ->
                uploads += query.getAsJsonArray("allimages")?.size() ?: 0
            }
            return minOf(uploads, MAX_FALLBACK_UPLOADS)
        }

        /**
         * Computes the same statistics as feedback.py from the public Commons API.
         * Throws [TooManyAchievementsException] above [MAX_FALLBACK_UPLOADS].
         */
        internal fun getAchievementsFromCommonsApi(userName: String): FeedbackResponse {
            val uploadedTitles = collectUploadedTitles(userName)
            val deletedUploads = countMissingUploads(uploadedTitles)
            val fileUsage = collectFileUsage(userName)
            return FeedbackResponse(
                uniqueUsedImages = fileUsage.uniqueUsedImages,
                articlesUsingImages = fileUsage.articlesUsingImages,
                deletedUploads = deletedUploads,
                featuredImages = FeaturedImages(fileUsage.qualityImages, fileUsage.featuredImages),
                thanksReceived = countThanks(userName),
                user = userName,
            )
        }

        /**
         * Titles from the user's upload log. Stops once [MAX_FALLBACK_UPLOADS] is passed.
         */
        private fun collectUploadedTitles(userName: String): Set<String> {
            val uploadedTitles = mutableSetOf<String>()
            queryCommonsApi(
                mapOf(
                    "list" to "logevents",
                    "letype" to "upload",
                    "leuser" to userName,
                    "leprop" to "title",
                    "lelimit" to "500",
                ),
            ) { query ->
                query.getAsJsonArray("logevents")?.forEach {
                    uploadedTitles.add(it.asJsonObject.get("title").asString)
                }
                if (uploadedTitles.size > MAX_FALLBACK_UPLOADS) {
                    throw TooManyAchievementsException()
                }
            }
            return uploadedTitles
        }

        /**
         * How many of [titles] no longer exist. MediaWiki accepts [TITLES_PER_QUERY] titles per request.
         */
        private fun countMissingUploads(titles: Set<String>): Int {
            var deletedUploads = 0
            titles.chunked(TITLES_PER_QUERY).forEach { batch ->
                queryCommonsApi(mapOf("titles" to batch.joinToString("|"))) { query ->
                    deletedUploads += query
                        .getAsJsonArray("pages")
                        ?.count { it.asJsonObject.get("missing")?.asBoolean == true } ?: 0
                }
            }
            return deletedUploads
        }

        /**
         * How often the user's current files are used, and how many are Featured or Quality images.
         */
        private fun collectFileUsage(userName: String): CommonsFileUsage {
            val usagesByPage = mutableMapOf<Int, Int>()
            val featuredPages = mutableSetOf<Int>()
            val qualityPages = mutableSetOf<Int>()
            queryCommonsApi(
                mapOf(
                    "generator" to "allimages",
                    "gaiuser" to userName,
                    "gaisort" to "timestamp",
                    "gailimit" to TITLES_PER_QUERY.toString(),
                    "prop" to "globalusage|categories",
                    "gulimit" to "500",
                    "cllimit" to "500",
                    "clcategories" to "Category:$FEATURED_PICTURES_CATEGORY|Category:$QUALITY_IMAGES_CATEGORY",
                ),
            ) { query ->
                query.getAsJsonArray("pages")?.forEach { element ->
                    val page = element.asJsonObject
                    val pageId = page.get("pageid").asInt
                    val usages = page.getAsJsonArray("globalusage")?.size() ?: 0
                    usagesByPage[pageId] = (usagesByPage[pageId] ?: 0) + usages
                    page.getAsJsonArray("categories")?.forEach {
                        when (
                            it.asJsonObject
                                .get("title")
                                .asString
                                .removePrefix("Category:")
                        ) {
                            FEATURED_PICTURES_CATEGORY -> featuredPages.add(pageId)
                            QUALITY_IMAGES_CATEGORY -> qualityPages.add(pageId)
                        }
                    }
                }
            }
            return CommonsFileUsage(
                uniqueUsedImages = usagesByPage.values.count { it > 0 },
                articlesUsingImages = usagesByPage.values.sum(),
                featuredImages = featuredPages.size,
                qualityImages = qualityPages.size,
            )
        }

        /**
         * Thanks logged against the user's user page.
         */
        private fun countThanks(userName: String): Int {
            var thanksReceived = 0
            queryCommonsApi(
                mapOf(
                    "list" to "logevents",
                    "letype" to "thanks",
                    "letitle" to "User:$userName",
                    "leprop" to "ids",
                    "lelimit" to "500",
                ),
            ) { query ->
                thanksReceived += query.getAsJsonArray("logevents")?.size() ?: 0
            }
            return thanksReceived
        }

        /**
         * Runs an action=query request against Commons, following continuation, and passes each
         * "query" object to [onBatch].
         */
        private fun queryCommonsApi(
            params: Map<String, String>,
            stopAfterBatch: () -> Boolean = { false },
            onBatch: (JsonObject) -> Unit,
        ) {
            var continuation = emptyMap<String, String>()
            do {
                val urlBuilder =
                    "${BuildConfig.COMMONS_URL}/w/api.php"
                        .toHttpUrlOrNull()!!
                        .newBuilder()
                        .addQueryParameter("action", "query")
                        .addQueryParameter("format", "json")
                        .addQueryParameter("formatversion", "2")
                (params + continuation).forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }
                val response = okHttpClient.newCall(Request.Builder().url(urlBuilder.build()).build()).execute()
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    throw IOException("Commons API request failed: ${response.message}")
                }
                val json = JsonParser.parseString(body.string()).asJsonObject
                json.getAsJsonObject("query")?.let(onBatch)
                if (stopAfterBatch()) {
                    return
                }
                continuation = json
                    .getAsJsonObject("continue")
                    ?.entrySet()
                    ?.associate { it.key to it.value.asString } ?: emptyMap()
            } while (continuation.isNotEmpty())
        }
    }
