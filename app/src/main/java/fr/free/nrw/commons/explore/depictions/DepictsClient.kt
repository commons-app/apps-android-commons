package fr.free.nrw.commons.explore.depictions

import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.depictions.models.DepictionResponse
import fr.free.nrw.commons.depictions.subClass.models.Binding
import fr.free.nrw.commons.depictions.subClass.models.SparqlResponse
import fr.free.nrw.commons.media.MediaInterface
import fr.free.nrw.commons.upload.depicts.DepictsInterface
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.utils.CommonsDateUtil
import io.reactivex.Observable
import io.reactivex.Single
import org.wikipedia.wikidata.Entities
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.ParseException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

const val LARGE_IMAGE_SIZE="640px"
const val THUMB_IMAGE_SIZE="70px"
/**
 * Depicts Client to handle custom calls to Commons Wikibase APIs
 */
@Singleton
class DepictsClient @Inject constructor(
    private val depictsInterface: DepictsInterface,
    private val mediaInterface: MediaInterface
) {

    /**
     * Search for depictions using the search item
     * @return list of depicted items
     */
    fun searchForDepictions(query: String?, limit: Int, offset: Int): Single<List<DepictedItem>> {
        val language = Locale.getDefault().language
        return depictsInterface.searchForDepicts(query, "$limit", language, language, "$offset")
            .map { it.search.joinToString("|") { searchItem -> searchItem.id } }
            .flatMap(::getEntities)
            .map { it.entities().values.map(::DepictedItem) }
    }

    /**
     * @return list of images for a particular depict entity
     */
    fun fetchImagesForDepictedItem(query: String, sroffset: Int): Observable<List<Media>> {
        return mediaInterface.fetchImagesForDepictedItem(
            "haswbstatement:" + BuildConfig.DEPICTS_PROPERTY + "=" + query,
            sroffset.toString()
        )
            .map { mwQueryResponse: DepictionResponse ->
                mwQueryResponse.query
                    .search
                    .map {
                        Media(
                            null,
                            getUrl(it.title),
                            it.title,
                            "",
                            0,
                            safeParseDate(it.timestamp),
                            safeParseDate(it.timestamp),
                            ""
                        )
                    }
            }
    }


    private fun getUrl(title: String): String {
        return getImageUrl(title, LARGE_IMAGE_SIZE)
    }

    fun getEntities(ids: String): Single<Entities> {
        return depictsInterface.getEntities(ids, Locale.getDefault().language)
    }

    fun toDepictions(sparqlResponse: Observable<SparqlResponse>): Observable<List<DepictedItem>> {
        return sparqlResponse.map { it.results.bindings.joinToString("|", transform = Binding::id) }
            .flatMap { getEntities(it).toObservable() }
            .map { it.entities().values.map(::DepictedItem) }
    }

    companion object {

        /**
         * Get url for the image from media of depictions
         * Ex: Tiger_Woods
         * Value: https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Tiger_Woods.jpg/70px-Tiger_Woods.jpg
         */
        fun getImageUrl(title: String, size: String): String {
            return title.substringAfter(":")
                .replace(" ", "_")
                .let {
                    val MD5Hash = getMd5(it)
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/${MD5Hash[0]}/${MD5Hash[0]}${MD5Hash[1]}/$it/$size-$it"
                }
        }

        /**
         * Generates MD5 hash for the filename
         */
        fun getMd5(input: String): String {
            return try {

                // Static getInstance method is called with hashing MD5
                val md = MessageDigest.getInstance("MD5")

                // digest() method is called to calculate message digest
                //  of an input digest() return array of byte
                val messageDigest = md.digest(input.toByteArray())

                // Convert byte array into signum representation
                val no = BigInteger(1, messageDigest)

                // Convert message digest into hex value
                var hashtext = no.toString(16)
                while (hashtext.length < 32) {
                    hashtext = "0$hashtext"
                }
                hashtext
            } // For specifying wrong message digest algorithms
            catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
        }

        /**
         * Parse the date string into the required format
         * @param dateStr
         * @return date in the required format
         */
        private fun safeParseDate(dateStr: String): Date? {
            return try {
                CommonsDateUtil.getIso8601DateFormatShort().parse(dateStr)
            } catch (e: ParseException) {
                null
            }
        }
    }

}
