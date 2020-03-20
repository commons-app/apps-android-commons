package fr.free.nrw.commons.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.R
import fr.free.nrw.commons.caching.CacheController
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.mwapi.CategoryApi
import fr.free.nrw.commons.settings.Prefs
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

/**
 * Processing of the image filePath that is about to be uploaded via ShareActivity is done here
 */
class FileProcessor @Inject constructor(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val cacheController: CacheController,
    private val gpsCategoryModel: GpsCategoryModel,
    @param:Named("default_preferences") private val defaultKvStore: JsonKvStore,
    private val apiCall: CategoryApi
) {
    private val compositeDisposable = CompositeDisposable()

    fun cleanup() {
        compositeDisposable.clear()
    }

    /**
     * Processes filePath coordinates, either from EXIF data or user location
     */
    fun processFileCoordinates(similarImageInterface: SimilarImageInterface, filePath: String?)
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
        val originalImageCoordinates = ImageCoordinates(exifInterface)
        if (originalImageCoordinates.decimalCoords == null) {
            //Find other photos taken around the same time which has gps coordinates
            findOtherImages(
                File(filePath),
                similarImageInterface
            )
        } else {
            useImageCoords(originalImageCoordinates)
        }
        return originalImageCoordinates
    }

    /**
     * Gets EXIF Tags from preferences to be redacted.
     *
     * @return tags to be redacted
     */
    private fun getExifTagsToRedact(): Set<String> {
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
    private fun redactExifTags(exifInterface: ExifInterface?, redactTags: Set<String>) {
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
            ?.let {
                exifInterface.setAttribute(tag, null).also {
                    Timber.d("Exif tag $tag with value $it redacted.")
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
            ImageCoordinates(contentResolver.openInputStream(Uri.fromFile(file)))
        } catch (e: IOException) {
            Timber.e(e)
            try {
                ImageCoordinates(file.absolutePath)
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
    fun useImageCoords(imageCoordinates: ImageCoordinates) {
        requireNotNull(imageCoordinates.decimalCoords)
        cacheController.setQtPoint(imageCoordinates.decLongitude, imageCoordinates.decLatitude)
        val displayCatList = cacheController.findCategory()

        // If no categories found in cache, call MediaWiki API to match image coords with nearby Commons categories
        if (displayCatList.isEmpty()) {
            compositeDisposable.add(
                apiCall.request(imageCoordinates.decimalCoords)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(
                        { gpsCategoryModel.categoryList = it },
                        {
                            Timber.e(it)
                            gpsCategoryModel.clear()
                        }
                    )
            )
            Timber.d("displayCatList size 0, calling MWAPI %s", displayCatList)
        } else {
            Timber.d("Cache found, setting categoryList in model to %s", displayCatList)
            gpsCategoryModel.categoryList = displayCatList
        }
    }
}
