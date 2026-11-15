package fr.free.nrw.commons.mwapi

import com.google.gson.Gson
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.campaigns.CampaignResponseDTO
import fr.free.nrw.commons.fileusages.FileUsagesResponse
import fr.free.nrw.commons.fileusages.GlobalFileUsagesResponse
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.model.ItemsClass
import fr.free.nrw.commons.nearby.model.NearbyQueryParams
import fr.free.nrw.commons.nearby.model.NearbyResponse
import fr.free.nrw.commons.nearby.model.PlaceBindings
import fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants
import fr.free.nrw.commons.profile.leaderboard.LeaderboardResponse
import fr.free.nrw.commons.profile.leaderboard.UpdateAvatarResponse
import fr.free.nrw.commons.upload.FileUtils
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test methods in ok http api client
 */
@Singleton
class OkHttpJsonApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val wikiMediaToolforgeUrl: HttpUrl,
    private val sparqlQueryUrl: String,
    private val campaignsUrl: String,
    private val gson: Gson
) {
    fun getLeaderboard(
        userName: String?, duration: String?,
        category: String?, limit: String?, offset: String?
    ): Observable<LeaderboardResponse> {
        val fetchLeaderboardUrlTemplate =
            wikiMediaToolforgeUrl.toString() + LeaderboardConstants.LEADERBOARD_END_POINT
        val url = String.format(
            Locale.ENGLISH,
            fetchLeaderboardUrlTemplate, userName, duration, category, limit, offset
        )
        val urlBuilder: HttpUrl.Builder = url.toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("user", userName)
            .addQueryParameter("duration", duration)
            .addQueryParameter("category", category)
            .addQueryParameter("limit", limit)
            .addQueryParameter("offset", offset)
        Timber.i("Url %s", urlBuilder.toString())
        val request: Request = Request.Builder()
            .url(urlBuilder.toString())
            .build()
        return Observable.fromCallable({
            val response: Response = okHttpClient.newCall(request).execute()
            if (response.body != null && response.isSuccessful) {
                val json: String = response.body!!.string()
                Timber.d("Response for leaderboard is %s", json)
                try {
                    return@fromCallable gson.fromJson<LeaderboardResponse>(
                        json,
                        LeaderboardResponse::class.java
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse the leaderboard response")
                    return@fromCallable LeaderboardResponse()
                }
            }
            LeaderboardResponse()
        })
    }

    /**
     * Show where file is being used on Commons.
     */
    suspend fun getFileUsagesOnCommons(
        fileName: String?,
        pageSize: Int
    ): FileUsagesResponse? {
        return withContext(Dispatchers.IO) {

            return@withContext try {

                val urlBuilder = BuildConfig.FILE_USAGES_BASE_URL.toHttpUrlOrNull()!!.newBuilder()
                urlBuilder.addQueryParameter("prop", "fileusage")
                urlBuilder.addQueryParameter("titles", fileName)
                urlBuilder.addQueryParameter("fulimit", pageSize.toString())

                Timber.i("Url %s", urlBuilder.toString())
                val request: Request = Request.Builder()
                    .url(urlBuilder.toString())
                    .build()

                val response: Response = okHttpClient.newCall(request).execute()
                if (response.body != null && response.isSuccessful) {
                    val json: String = response.body!!.string()
                    gson.fromJson<FileUsagesResponse>(
                        json,
                        FileUsagesResponse::class.java
                    )
                } else null
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }
    }

    /**
     * Show where file is being used on non-Commons wikis, typically the Wikipedias in various languages.
     */
    suspend fun getGlobalFileUsages(
        fileName: String?,
        pageSize: Int
    ): GlobalFileUsagesResponse? {

        return withContext(Dispatchers.IO) {

            return@withContext try {

                val urlBuilder = BuildConfig.FILE_USAGES_BASE_URL.toHttpUrlOrNull()!!.newBuilder()
                urlBuilder.addQueryParameter("prop", "globalusage")
                urlBuilder.addQueryParameter("titles", fileName)
                urlBuilder.addQueryParameter("gulimit", pageSize.toString())

                Timber.i("Url %s", urlBuilder.toString())
                val request: Request = Request.Builder()
                    .url(urlBuilder.toString())
                    .build()

                val response: Response = okHttpClient.newCall(request).execute()
                if (response.body != null && response.isSuccessful) {
                    val json: String = response.body!!.string()

                    gson.fromJson<GlobalFileUsagesResponse>(
                        json,
                        GlobalFileUsagesResponse::class.java
                    )
                } else null
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }
    }

    fun setAvatar(username: String?, avatar: String?): Single<UpdateAvatarResponse?> {
        val urlTemplate = wikiMediaToolforgeUrl
            .toString() + LeaderboardConstants.UPDATE_AVATAR_END_POINT
        return Single.fromCallable<UpdateAvatarResponse?>({
            val url = String.format(Locale.ENGLISH, urlTemplate, username, avatar)
            val urlBuilder: HttpUrl.Builder = url.toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("user", username)
                .addQueryParameter("avatar", avatar)
            Timber.i("Url %s", urlBuilder.toString())
            val request: Request = Request.Builder()
                .url(urlBuilder.toString())
                .build()
            val response: Response = okHttpClient.newCall(request).execute()
            if (response.body != null && response.isSuccessful) {
                val json: String = response.body!!.string()
                try {
                    return@fromCallable gson.fromJson<UpdateAvatarResponse>(
                        json,
                        UpdateAvatarResponse::class.java
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse the avatar update response")
                    return@fromCallable UpdateAvatarResponse()
                }
            }
            null
        })
    }

    @JvmOverloads
    @Throws(Exception::class)
    fun getNearbyPlaces(
        cur: LatLng, language: String, radius: Double,
        customQuery: String? = null
    ): List<Place>? {
        Timber.d("Fetching nearby items at radius %s", radius)
        Timber.d("CUSTOM_SPARQL: %s", (customQuery != null).toString())
        val wikidataQuery: String = if (customQuery != null) {
            customQuery
        } else {
            FileUtils.readFromResource("/queries/radius_query_for_upload_wizard.rq")
        }
        val query = wikidataQuery
            .replace("\${RAD}", String.format(Locale.ROOT, "%.2f", radius))
            .replace("\${LAT}", String.format(Locale.ROOT, "%.4f", cur.latitude))
            .replace("\${LONG}", String.format(Locale.ROOT, "%.4f", cur.longitude))
            .replace("\${LANG}", language)
            .replace("\${FALLBACK_LANGUAGES}", FALLBACK_LANGUAGES)

        val urlBuilder: HttpUrl.Builder = sparqlQueryUrl.toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("format", "json")

        val request: Request = Request.Builder()
            .url(urlBuilder.build())
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (response.body != null && response.isSuccessful) {
            val json = response.body!!.string()
            val nearbyResponse = gson.fromJson(json, NearbyResponse::class.java)
            val bindings = nearbyResponse.results.bindings
            val places: MutableList<Place> = ArrayList()
            for (item in bindings) {
                val placeFromNearbyItem = Place.from(item)
                placeFromNearbyItem.isMonument = false
                places.add(placeFromNearbyItem)
            }
            return places
        }
        throw IOException(response.message)
    }

    @Throws(Exception::class)
    fun getNearbyPlaces(
        queryParams: NearbyQueryParams, language: String,
        shouldQueryForMonuments: Boolean, customQuery: String?
    ): List<Place>? {
        Timber.d("CUSTOM_SPARQL: %s", (customQuery != null).toString())

        val locale = Locale.ROOT;
        val wikidataQuery: String = if (customQuery != null) {
                when (queryParams) {
                    is NearbyQueryParams.Rectangular -> {
                        val westCornerLat = queryParams.screenTopRight.latitude
                        val westCornerLong = queryParams.screenTopRight.longitude
                        val eastCornerLat = queryParams.screenBottomLeft.latitude
                        val eastCornerLong = queryParams.screenBottomLeft.longitude
                        customQuery
                            .replace("\${LAT_WEST}", String.format(locale, "%.4f", westCornerLat))
                            .replace("\${LONG_WEST}", String.format(locale, "%.4f", westCornerLong))
                            .replace("\${LAT_EAST}", String.format(locale, "%.4f", eastCornerLat))
                            .replace("\${LONG_EAST}", String.format(locale, "%.4f", eastCornerLong))
                            .replace("\${LANG}", language)
                    }
                    is NearbyQueryParams.Radial -> {
                        Timber.e(
                            "%s%s",
                            "okHttpJsonApiClient.getNearbyPlaces invoked with custom query",
                            "and radial coordinates. This is currently not supported."
                        )
                        ""
                    }
                }
        } else when (queryParams) {
            is NearbyQueryParams.Radial -> {
                val placeHolderQuery: String = if (!shouldQueryForMonuments) {
                    FileUtils.readFromResource("/queries/radius_query_for_nearby.rq")
                } else {
                    FileUtils.readFromResource("/queries/radius_query_for_nearby_monuments.rq")
                }
                placeHolderQuery.replace(
                        "\${LAT}", String.format(locale, "%.4f", queryParams.center.latitude)
                    ).replace(
                        "\${LONG}", String.format(locale, "%.4f", queryParams.center.longitude)
                    )
                    .replace("\${RAD}", String.format(locale, "%.2f", queryParams.radiusInKm))
            }

            is NearbyQueryParams.Rectangular -> {
                val placeHolderQuery: String = if (!shouldQueryForMonuments) {
                    FileUtils.readFromResource("/queries/rectangle_query_for_nearby.rq")
                } else {
                    FileUtils.readFromResource("/queries/rectangle_query_for_nearby_monuments.rq")
                }
                val westCornerLat = queryParams.screenTopRight.latitude
                val westCornerLong = queryParams.screenTopRight.longitude
                val eastCornerLat = queryParams.screenBottomLeft.latitude
                val eastCornerLong = queryParams.screenBottomLeft.longitude
                placeHolderQuery
                    .replace("\${LAT_WEST}", String.format(locale, "%.4f", westCornerLat))
                    .replace("\${LONG_WEST}", String.format(locale, "%.4f", westCornerLong))
                    .replace("\${LAT_EAST}", String.format(locale, "%.4f", eastCornerLat))
                    .replace("\${LONG_EAST}", String.format(locale, "%.4f", eastCornerLong))
                    .replace("\${LANG}", language)
            }
        }

        val urlBuilder: HttpUrl.Builder = sparqlQueryUrl.toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("query", wikidataQuery)
            .addQueryParameter("format", "json")

        val request: Request = Request.Builder()
            .url(urlBuilder.build())
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (response.body != null && response.isSuccessful) {
            val json = response.body!!.string()
            val nearbyResponse = gson.fromJson(json, NearbyResponse::class.java)
            val bindings = nearbyResponse.results.bindings
            val places: MutableList<Place> = ArrayList()
            for (item in bindings) {
                val placeFromNearbyItem = Place.from(item)
                if (shouldQueryForMonuments && item.getMonument() != null) {
                    placeFromNearbyItem.isMonument = true
                } else {
                    placeFromNearbyItem.isMonument = false
                }
                places.add(placeFromNearbyItem)
            }
            return places
        }
        throw IOException(response.message)
    }

    @Throws(Exception::class)
    fun getPlacesAsKML(leftLatLng: LatLng, rightLatLng: LatLng): String? {
        var kmlString = """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!--Created by Wikimedia Commons Android app -->
<kml xmlns="http://www.opengis.net/kml/2.2">
    <Document>"""
        val placeBindings = runQuery(
            leftLatLng,
            rightLatLng
        )
        if (placeBindings != null) {
            for ((item1, label, location, clas) in placeBindings) {
                val input = location.value
                val pattern = Pattern.compile(
                    "Point\\(([-+]?[0-9]*\\.?[0-9]+) ([-+]?[0-9]*\\.?[0-9]+)\\)"
                )
                val matcher = pattern.matcher(input)

                if (matcher.find()) {
                    val longStr = matcher.group(1)
                    val latStr = matcher.group(2)
                    val itemUrl = item1.value
                    val itemName = label.value.replace("&", "&amp;")
                    val itemLatitude = latStr
                    val itemLongitude = longStr
                    val itemClass = clas.value

                    val formattedItemName =
                        if (!itemClass.isEmpty())
                            "$itemName ($itemClass)"
                        else
                            itemName

                    val kmlEntry = ("""
    <Placemark>
        <name>$formattedItemName</name>
        <description>$itemUrl</description>
        <Point>
            <coordinates>$itemLongitude,$itemLatitude</coordinates>
        </Point>
    </Placemark>""")
                    kmlString = kmlString + kmlEntry
                } else {
                    Timber.e("No match found")
                }
            }
        }
        kmlString = """$kmlString
    </Document>
</kml>
"""
        return kmlString
    }

    @Throws(Exception::class)
    fun getPlacesAsGPX(leftLatLng: LatLng, rightLatLng: LatLng): String? {
        var gpxString = ("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<gpx
 version="1.0"
 creator="Wikimedia Commons Android app"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xmlns="http://www.topografix.com/GPX/1/0"
 xsi:schemaLocation="http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd">
<bounds minlat="${"$"}MIN_LATITUDE" minlon="${"$"}MIN_LONGITUDE" """ +
                    """maxlat="${"$"}MAX_LATITUDE" maxlon="${"$"}MAX_LONGITUDE"/>""")

        val placeBindings = runQuery(leftLatLng, rightLatLng)
        if (placeBindings != null) {
            for ((item1, label, location, clas) in placeBindings) {
                val input = location.value
                val pattern = Pattern.compile(
                    "Point\\(([-+]?[0-9]*\\.?[0-9]+) ([-+]?[0-9]*\\.?[0-9]+)\\)"
                )
                val matcher = pattern.matcher(input)

                if (matcher.find()) {
                    val longStr = matcher.group(1)
                    val latStr = matcher.group(2)
                    val itemUrl = item1.value
                    val itemName = label.value.replace("&", "&amp;")
                    val itemLatitude = latStr
                    val itemLongitude = longStr
                    val itemClass = clas.value

                    val formattedItemName = if (!itemClass.isEmpty())
                        "$itemName ($itemClass)"
                    else
                        itemName

                    val gpxEntry =
                        ("""
<wpt lat="$itemLatitude" lon="$itemLongitude">
    <name>$itemName</name>
    <url>$itemUrl</url>
</wpt>""")
                    gpxString = gpxString + gpxEntry
                } else {
                    Timber.e("No match found")
                }
            }
        }
        gpxString = "$gpxString\n</gpx>"
        return gpxString
    }

    fun getCampaigns(): Single<CampaignResponseDTO> {
        return Single.fromCallable<CampaignResponseDTO?>({
            val request: Request = Request.Builder().url(campaignsUrl).build()
            val response: Response = okHttpClient.newCall(request).execute()
            if (response.body != null && response.isSuccessful) {
                val json: String = response.body!!.string()
                return@fromCallable gson.fromJson<CampaignResponseDTO>(
                    json,
                    CampaignResponseDTO::class.java
                )
            }
            null
        })
    }

    @Throws(IOException::class)
    private fun runQuery(currentLatLng: LatLng, nextLatLng: LatLng): List<PlaceBindings>? {
        val wikidataQuery = FileUtils.readFromResource("/queries/places_query.rq")
        val query = wikidataQuery
            .replace("\${LONGITUDE}", String.format(Locale.ROOT, "%.2f", currentLatLng.longitude))
            .replace("\${LATITUDE}", String.format(Locale.ROOT, "%.4f", currentLatLng.latitude))
            .replace("\${NEXT_LONGITUDE}", String.format(Locale.ROOT, "%.4f", nextLatLng.longitude))
            .replace("\${NEXT_LATITUDE}", String.format(Locale.ROOT, "%.4f", nextLatLng.latitude))

        val urlBuilder: HttpUrl.Builder = sparqlQueryUrl.toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("format", "json")

        val request: Request = Request.Builder().url(urlBuilder.build()).build()

        val response = okHttpClient.newCall(request).execute()
        if (response.body != null && response.isSuccessful) {
            val json = response.body!!.string()
            val item = gson.fromJson(json, ItemsClass::class.java)
            return item.results.bindings
        } else {
            return null
        }
    }
}
