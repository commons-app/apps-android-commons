package fr.free.nrw.commons.mwapi

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.profile.achievements.FeedbackResponse
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.wikidata.model.GetWikidataEditCountResponse
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** Reads the contributor statistics used by Achievements, Contributions and the quiz. */
@Singleton
class UserStatsClient
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        @Named("tools_forge") private val wikiMediaToolforgeUrl: HttpUrl,
        private val gson: Gson,
        private val commonsUserStats: CommonsUserStats,
    ) {
        /** Counts uploads using Toolforge, with a bounded Commons fallback. */
        fun getUploadCount(userName: String?): Single<Int> {
            val urlBuilder: HttpUrl.Builder =
                wikiMediaToolforgeUrl
                    .newBuilder()
                    .addPathSegments("uploadsbyuser.py")
                    .addQueryParameter("user", userName)

            if (isBetaFlavour) {
                urlBuilder.addQueryParameter("labs", "commonswiki")
            }

            val request: Request =
                Request
                    .Builder()
                    .url(urlBuilder.build())
                    .build()

            return Single.fromCallable {
                val counted =
                    try {
                        uploadCountFromToolforge(request)
                    } catch (e: IOException) {
                        Timber.w(e, "uploadsbyuser.py failed, counting uploads from the Commons API")
                        null
                    }
                counted ?: userName?.let {
                    try {
                        commonsUserStats.countUploadsFromCommonsApi(it)
                    } catch (e: IOException) {
                        Timber.e(e, "Commons API upload count failed")
                        0
                    }
                } ?: 0
            }
        }

        /**
         * Returns the Toolforge count, or null when that response cannot be used.
         */
        private fun uploadCountFromToolforge(request: Request): Int? {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.w("uploadsbyuser.py failed with HTTP ${response.code}")
                return null
            }
            val count =
                response.body
                    ?.string()
                    ?.trim()
                    ?.toIntOrNull()
            if (count == null) {
                Timber.w("uploadsbyuser.py returned no upload count")
            }
            return count
        }

        /** Reads a user’s Wikidata edit count, falling back to the Wikidata user list. */
        fun getWikidataEdits(userName: String?): Single<Int> {
            val urlBuilder: HttpUrl.Builder =
                wikiMediaToolforgeUrl
                    .newBuilder()
                    .addPathSegments("wikidataedits.py")
                    .addQueryParameter("user", userName)

            if (isBetaFlavour) {
                urlBuilder.addQueryParameter("labs", "commonswiki")
            }

            val request: Request =
                Request
                    .Builder()
                    .url(urlBuilder.build())
                    .build()

            return Single.fromCallable {
                val counted =
                    try {
                        wikidataEditsFromToolforge(request)
                    } catch (e: IOException) {
                        Timber.w(e, "wikidataedits.py failed, reading the edit count from Wikidata")
                        null
                    }
                counted ?: userName?.let { countWikidataEditsFromApi(it) } ?: 0
            }
        }

        /**
         * Returns the Toolforge edit count, or null when that response cannot be used.
         */
        private fun wikidataEditsFromToolforge(request: Request): Int? {
            val response = okHttpClient.newCall(request).execute()
            val json = response.body?.takeIf { response.isSuccessful }?.string()
            val jsonStart = json?.indexOf('{') ?: -1
            if (json == null) {
                Timber.w("wikidataedits.py failed with HTTP ${response.code}")
            }
            return if (json == null || jsonStart < 0) {
                null
            } else {
                try {
                    gson
                        .fromJson(json.substring(jsonStart), GetWikidataEditCountResponse::class.java)
                        ?.wikidataEditCount
                } catch (e: JsonParseException) {
                    Timber.w(e, "wikidataedits.py returned invalid JSON")
                    null
                }
            }
        }

        /**
         * Edit count from Wikidata's public user list. This replaces wikidataedits.py.
         */
        private fun countWikidataEditsFromApi(userName: String): Int {
            val url =
                "${BuildConfig.WIKIDATA_URL}/w/api.php"
                    .toHttpUrlOrNull()!!
                    .newBuilder()
                    .addQueryParameter("action", "query")
                    .addQueryParameter("format", "json")
                    .addQueryParameter("formatversion", "2")
                    .addQueryParameter("list", "users")
                    .addQueryParameter("ususers", userName)
                    .addQueryParameter("usprop", "editcount")
                    .build()
            val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body
            if (!response.isSuccessful || body == null) {
                throw IOException("Wikidata edit count request failed: ${response.message}")
            }
            val users =
                JsonParser
                    .parseString(body.string())
                    .asJsonObject
                    .getAsJsonObject("query")
                    ?.getAsJsonArray("users")
            return users
                ?.firstOrNull()
                ?.asJsonObject
                ?.get("editcount")
                ?.asInt ?: 0
        }

        /** Loads Toolforge feedback or computes the equivalent Commons statistics. */
        fun getAchievements(userName: String?): Single<FeedbackResponse?> {
            val suffix = if (isBetaFlavour) "/feedback.py?labs=commonswiki" else "/feedback.py"
            val fetchAchievementUrlTemplate = wikiMediaToolforgeUrl.toString() + suffix
            return Single.fromCallable {
                val fromToolforge =
                    try {
                        feedbackFromToolforge(userName, fetchAchievementUrlTemplate)
                    } catch (e: IOException) {
                        Timber.w(e, "feedback.py failed, computing achievements from the Commons API")
                        null
                    }
                when {
                    fromToolforge != null -> fromToolforge
                    userName != null -> commonsUserStats.getAchievementsFromCommonsApi(userName)
                    else -> null
                }
            }
        }

        /**
         * Parsed feedback.py body, or null when the caller should use the Commons API instead.
         */
        private fun feedbackFromToolforge(
            userName: String?,
            fetchAchievementUrlTemplate: String,
        ): FeedbackResponse? {
            val url = String.format(Locale.ENGLISH, fetchAchievementUrlTemplate, userName)
            val request =
                Request
                    .Builder()
                    .url(
                        url
                            .toHttpUrlOrNull()!!
                            .newBuilder()
                            .addQueryParameter("user", userName)
                            .build(),
                    ).build()
            val response = okHttpClient.newCall(request).execute()
            val json = response.body?.takeIf { response.isSuccessful }?.string()
            val jsonStart = json?.indexOf('{') ?: -1
            if (json == null) {
                Timber.w("feedback.py failed with HTTP ${response.code}")
            } else if (jsonStart < 0) {
                Timber.w("feedback.py returned no JSON, computing achievements from the Commons API")
            }
            return if (json == null || jsonStart < 0) {
                null
            } else {
                try {
                    gson.fromJson(json.substring(jsonStart), FeedbackResponse::class.java)
                } catch (e: JsonParseException) {
                    Timber.w(e, "feedback.py returned invalid JSON, computing achievements from the Commons API")
                    null
                }
            }
        }
    }
