package fr.free.nrw.commons.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.R
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.CategoryApi
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.upload.structure.depictions.DepictModel
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Named

/**
 * Processing of the image filePath that is about to be uploaded via ShareActivity is done here
 */

private const val DEFAULT_SUGGESTION_RADIUS_IN_METRES = 100
private const val MAX_SUGGESTION_RADIUS_IN_METRES = 1000
private const val RADIUS_STEP_SIZE_IN_METRES = 100
private const val MIN_NEARBY_RESULTS = 5

class FileProcessor @Inject constructor(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val gpsCategoryModel: GpsCategoryModel,
    private val depictsModel: DepictModel,
    @param:Named("default_preferences") private val defaultKvStore: JsonKvStore,
    private val apiCall: CategoryApi,
    private val okHttpJsonApiClient: OkHttpJsonApiClient
) {
    private val compositeDisposable = CompositeDisposable()

    fun cleanup() {
        compositeDisposable.clear()
    }

    /**
     * Processes filePath coordinates, either from EXIF data or user location
     */
    fun processFileCoordinates(similarImageInterface: SimilarImageInterface,
                               filePath: String?, inAppPictureLocation: LatLng?)
            : ImageCoordinates {
        val exifInterface: ExifInterface? = try {
            ExifInterface(filePath!!)
        } catch (e: IOException) {
            Timber.e(e)
            null
        }
        // Redact EXIF data as indicated in preferences.
        redactExifTags(exifInterface, getExifTagsToRedact())
        Timber.d("Calling GPSExtractor")
        val originalImageCoordinates = ImageCoordinates(exifInterface, inAppPictureLocation)
        if (originalImageCoordinates.decimalCoords == null) {
            //Find other photos taken around the same time which has gps coordinates
            findOtherImages(
                File(filePath),
                similarImageInterface
            )
        } else {
            prePopulateCategoriesAndDepictionsBy(originalImageCoordinates)
        }
        return originalImageCoordinates
    }

    /**
     * Gets EXIF Tags from preferences to be redacted.
     *
     * @return tags to be redacted
     */
    fun getExifTagsToRedact(): Set<String> {
        val prefManageEXIFTags =
            defaultKvStore.getStringSet(Prefs.MANAGED_EXIF_TAGS) ?: emptySet()
        val redactTags: Set<String> =
            context.resources.getStringArray(R.array.pref_exifTag_values).toSet()
        return redactTags - prefManageEXIFTags
    }

    /**
     * Redacts EXIF metadata as indicated in preferences.
     *
     * @param exifInterface ExifInterface object
     * @param redactTags    tags to be redacted
     */
    fun redactExifTags(exifInterface: ExifInterface?, redactTags: Set<String>) {
        compositeDisposable.add(
            Observable.fromIterable(redactTags)
                .flatMap { Observable.fromArray(*FileMetadataUtils.getTagsFromPref(it)) }
                .subscribe(
                    { redactTag(exifInterface, it) },
                    { Timber.d(it) },
                    { save(exifInterface) }
                )
        )
    }

    private fun save(exifInterface: ExifInterface?) {
        try {
            exifInterface?.saveAttributes()
        } catch (e: IOException) {
            Timber.w("EXIF redaction failed: %s", e.toString())
        }
    }

    private fun redactTag(exifInterface: ExifInterface?, tag: String) {
        Timber.d("Checking for tag: %s", tag)
        exifInterface?.getAttribute(tag)
            ?.takeIf { it.isNotEmpty() }
            ?.let { attributeName ->
                exifInterface.setAttribute(tag, null).also {
                    Timber.d("Exif tag $tag with value $attributeName redacted.")
                }
            }
    }

    /**
     * Find other images around the same location that were taken within the last 20 sec
     *
     * @param originalImageCoordinates
     * @param fileBeingProcessed
     * @param similarImageInterface
     */
    private fun findOtherImages(
        fileBeingProcessed: File,
        similarImageInterface: SimilarImageInterface
    ) {
        val oneHundredAndTwentySeconds = 120 * 1000L
        //Time when the original image was created
        val timeOfCreation = fileBeingProcessed.lastModified()
        LongRange
        val timeOfCreationRange =
            timeOfCreation - oneHundredAndTwentySeconds..timeOfCreation + oneHundredAndTwentySeconds
        fileBeingProcessed.parentFile
            .listFiles()
            .asSequence()
            .filter { it.lastModified() in timeOfCreationRange }
            .map { Pair(it, readImageCoordinates(it)) }
            .firstOrNull { it.second?.decimalCoords != null }
            ?.let { fileCoordinatesPair ->
                similarImageInterface.showSimilarImageFragment(
                    fileBeingProcessed.path,
                    fileCoordinatesPair.first.absolutePath,
                    fileCoordinatesPair.second
                )
            }
    }

    private fun readImageCoordinates(file: File) =
        try {
            /* Used null location as location for similar images captured before is not available
               in case it is not present in the EXIF. */
            ImageCoordinates(contentResolver.openInputStream(Uri.fromFile(file))!!, null)
        } catch (e: IOException) {
            Timber.e(e)
            try {
                ImageCoordinates(file.absolutePath, null)
            } catch (ex: IOException) {
                Timber.e(ex)
                null
            }
        }

    /**
     * Initiates retrieval of image coordinates or user coordinates, and caching of coordinates. Then
     * initiates the calls to MediaWiki API through an instance of CategoryApi.
     *
     * @param imageCoordinates
     */
    fun prePopulateCategoriesAndDepictionsBy(imageCoordinates: ImageCoordinates) {
        requireNotNull(imageCoordinates.decimalCoords)
        compositeDisposable.add(
            apiCall.request(imageCoordinates.decimalCoords)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    gpsCategoryModel::setCategoriesFromLocation,
                    {
                        Timber.e(it)
                        gpsCategoryModel.clear()
                    }
                )
        )

        compositeDisposable.add(
            suggestNearbyDepictions(imageCoordinates)
        )
    }

    private val radiiProgressionInMetres =
        (DEFAULT_SUGGESTION_RADIUS_IN_METRES..MAX_SUGGESTION_RADIUS_IN_METRES step RADIUS_STEP_SIZE_IN_METRES)

    private fun suggestNearbyDepictions(imageCoordinates: ImageCoordinates): Disposable {
        return Observable.fromIterable(radiiProgressionInMetres.map { it / 1000.0 })
            .concatMap {
                Observable.fromCallable {
                    okHttpJsonApiClient.getNearbyPlaces(
                        imageCoordinates.latLng,
                        Locale.getDefault().language,
                        it,
                        false
                    )
                }
            }
            .subscribeOn(Schedulers.io())
            .filter { it.size >= MIN_NEARBY_RESULTS }
            .take(1)
            .subscribe(
                { depictsModel.nearbyPlaces.offer(it) },
                { Timber.e(it) }
            )
    }
}
