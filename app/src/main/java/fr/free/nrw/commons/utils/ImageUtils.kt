package fr.free.nrw.commons.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.SetWallpaperWorker
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * Created by blueSir9 on 3/10/17.
 */


object ImageUtils {

    /**
     * Set 0th bit as 1 for dark image ie. 0001
     */
    const val IMAGE_DARK = 1 shl 0 // 1

    /**
     * Set 1st bit as 1 for blurry image ie. 0010
     */
    const val IMAGE_BLURRY = 1 shl 1 // 2

    /**
     * Set 2nd bit as 1 for duplicate image ie. 0100
     */
    const val IMAGE_DUPLICATE = 1 shl 2 // 4

    /**
     * Set 3rd bit as 1 for image with different geo location ie. 1000
     */
    const val IMAGE_GEOLOCATION_DIFFERENT = 1 shl 3 // 8

    /**
     * The parameter FILE_FBMD is returned from the class ReadFBMD if the uploaded image contains
     * FBMD data else returns IMAGE_OK
     * ie. 10000
     */
    const val FILE_FBMD = 1 shl 4 // 16

    /**
     * The parameter FILE_NO_EXIF is returned from the class EXIFReader if the uploaded image does
     * not contains EXIF data else returns IMAGE_OK
     * ie. 100000
     */
    const val FILE_NO_EXIF = 1 shl 5 // 32

    const val IMAGE_OK = 0
    const val IMAGE_KEEP = -1
    const val IMAGE_WAIT = -2
    const val EMPTY_CAPTION = -3
    const val FILE_NAME_EXISTS = 1 shl 6 // 64
    const val NO_CATEGORY_SELECTED = -5

    private var progressDialogWallpaper: ProgressDialog? = null

    private var progressDialogAvatar: ProgressDialog? = null

    @IntDef(
        flag = true,
        value = [
            IMAGE_DARK,
            IMAGE_BLURRY,
            IMAGE_DUPLICATE,
            IMAGE_OK,
            IMAGE_KEEP,
            IMAGE_WAIT,
            EMPTY_CAPTION,
            FILE_NAME_EXISTS,
            NO_CATEGORY_SELECTED,
            IMAGE_GEOLOCATION_DIFFERENT
        ]
    )
    @Retention
    annotation class Result

    /**
     * @return IMAGE_OK if image is not too dark
     * IMAGE_DARK if image is too dark
     */
    @JvmStatic
    fun checkIfImageIsTooDark(imagePath: String): Int {
        val millis = System.currentTimeMillis()
        return try {
            var bmp = ExifInterface(imagePath).thumbnailBitmap
            if (bmp == null) {
                bmp = BitmapFactory.decodeFile(imagePath)
            }

            if (checkIfImageIsDark(bmp)) {
                IMAGE_DARK
            } else {
                IMAGE_OK
            }
        } catch (e: Exception) {
            Timber.d(e, "Error while checking image darkness.")
            IMAGE_OK
        } finally {
            Timber.d("Checking image darkness took ${System.currentTimeMillis() - millis} ms.")
        }
    }

    /**
     * @param geolocationOfFileString Geolocation of image. If geotag doesn't exists, then this will
     * be an empty string
     * @param latLng Location of wikidata item will be edited after upload
     * @return false if image is neither dark nor blurry or if the input bitmapRegionDecoder provide
     * d is null true if geolocation of the image and wikidata item are different
     */
    @JvmStatic
    fun checkImageGeolocationIsDifferent(geolocationOfFileString: String, latLng: LatLng?): Boolean {
        Timber.d("Comparing geolocation of file with nearby place location")
        if (latLng == null) { // Means that geolocation for this image is not given
            return false // Since we don't know geolocation of file, we choose letting upload
        }

        val geolocationOfFile = geolocationOfFileString.split("|")
        val distance = LengthUtils.computeDistanceBetween(
            LatLng(geolocationOfFile[0].toDouble(), geolocationOfFile[1].toDouble(), 0.0F),
            latLng
        )
        // Distance is more than 1 km, means that geolocation is wrong
        return distance >= 1000
    }

    @JvmStatic
    private fun checkIfImageIsDark(bitmap: Bitmap?): Boolean {
        if (bitmap == null) {
            Timber.e("Expected bitmap was null")
            return true
        }

        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        val allPixelsCount = bitmapWidth * bitmapHeight
        var numberOfBrightPixels = 0
        var numberOfMediumBrightnessPixels = 0
        val brightPixelThreshold = 0.025 * allPixelsCount
        val mediumBrightPixelThreshold = 0.3 * allPixelsCount

        for (x in 0 until bitmapWidth) {
            for (y in 0 until bitmapHeight) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val max = maxOf(r, g, b) / 255.0
                val min = minOf(r, g, b) / 255.0

                val luminance = ((max + min) / 2.0) * 100

                val highBrightnessLuminance = 40
                val mediumBrightnessLuminance = 26

                if (luminance < highBrightnessLuminance) {
                    if (luminance > mediumBrightnessLuminance) {
                        numberOfMediumBrightnessPixels++
                    }
                } else {
                    numberOfBrightPixels++
                }

                if (numberOfBrightPixels >= brightPixelThreshold || numberOfMediumBrightnessPixels >= mediumBrightPixelThreshold) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Downloads the image from the URL and sets it as the phone's wallpaper
     * Fails silently if download or setting wallpaper fails.
     *
     * @param context context
     * @param imageUrl Url of the image
     */
    @JvmStatic
    fun setWallpaperFromImageUrl(context: Context, imageUrl: Uri) {
        enqueueSetWallpaperWork(context, imageUrl)
    }

    @JvmStatic
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Wallpaper Setting"
            val description = "Notifications for wallpaper setting progress"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("set_wallpaper_channel", name, importance).apply {
                this.description = description
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Calls the set avatar api to set the image url as user's avatar
     * @param context
     * @param url
     * @param username
     * @param okHttpJsonApiClient
     * @param compositeDisposable
     */
    @JvmStatic
    fun setAvatarFromImageUrl(
        context: Context,
        url: String,
        username: String,
        okHttpJsonApiClient: OkHttpJsonApiClient,
        compositeDisposable: CompositeDisposable
    ) {
        showSettingAvatarProgressBar(context)

        try {
            compositeDisposable.add(
                okHttpJsonApiClient
                    .setAvatar(username, url)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { response ->
                            if (response?.status == "200") {
                                ViewUtil.showLongToast(context, context.getString(R.string.avatar_set_successfully))
                                progressDialogAvatar?.dismiss()
                            }
                        },
                        { t ->
                            Timber.e(t, "Setting Avatar Failed")
                            ViewUtil.showLongToast(context, context.getString(R.string.avatar_set_unsuccessfully))
                            progressDialogAvatar?.cancel()
                        }
                    )
            )
        } catch (e: Exception) {
            Timber.d("$e success")
            ViewUtil.showLongToast(context, context.getString(R.string.avatar_set_unsuccessfully))
            progressDialogAvatar?.cancel()
        }
    }

    @JvmStatic
    fun enqueueSetWallpaperWork(context: Context, imageUrl: Uri) {
        createNotificationChannel(context) // Ensure the notification channel is created

        val inputData = Data.Builder()
            .putString("imageUrl", imageUrl.toString())
            .build()

        val setWallpaperWork = OneTimeWorkRequest.Builder(SetWallpaperWorker::class.java)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(setWallpaperWork)
    }

    @JvmStatic
    private fun showSettingWallpaperProgressBar(context: Context) {
        progressDialogWallpaper = ProgressDialog.show(
            context,
            context.getString(R.string.setting_wallpaper_dialog_title),
            context.getString(R.string.setting_wallpaper_dialog_message),
            true,
            false
        )
    }

    @JvmStatic
    private fun showSettingAvatarProgressBar(context: Context) {
        progressDialogAvatar = ProgressDialog.show(
            context,
            context.getString(R.string.setting_avatar_dialog_title),
            context.getString(R.string.setting_avatar_dialog_message),
            true,
            false
        )
    }

    /**
     * Adds red border to bitmap with specified border size
     * * @param bitmap
     * * @param borderSize
     * * @param context
     * * @return
     */
    @JvmStatic
    fun addRedBorder(bitmap: Bitmap, borderSize: Int, context: Context): Bitmap {
        val bmpWithBorder = Bitmap.createBitmap(
            bitmap.width + borderSize * 2,
            bitmap.height + borderSize * 2,
            bitmap.config
        )
        val canvas = Canvas(bmpWithBorder)
        canvas.drawColor(ContextCompat.getColor(context, R.color.deleteRed))
        canvas.drawBitmap(bitmap, borderSize.toFloat(), borderSize.toFloat(), null)
        return bmpWithBorder
    }

    /**
     * Result variable is a result of an or operation of all possible problems. Ie. if result
     * is 0001 means IMAGE_DARK
     * if result is 1100 IMAGE_DUPLICATE and IMAGE_GEOLOCATION_DIFFERENT
     */
    @JvmStatic
    fun getErrorMessageForResult(context: Context, @Result result: Int): String {
        val errorMessage = StringBuilder()
        if (result <= 0) {
            Timber.d("No issues to warn user are found")
        } else {
            Timber.d("Issues found to warn user")
            errorMessage.append(context.getString(R.string.upload_problem_exist))

            if (result and IMAGE_DARK != 0) {
                errorMessage.append("\n - ")
                    .append(context.getString(R.string.upload_problem_image_dark))
            }
            if (result and IMAGE_BLURRY != 0) {
                errorMessage.append("\n - ")
                    .append(context.getString(R.string.upload_problem_image_blurry))
            }
            if (result and IMAGE_DUPLICATE != 0) {
                errorMessage.append("\n - ").
                append(context.getString(R.string.upload_problem_image_duplicate))
            }
            if (result and IMAGE_GEOLOCATION_DIFFERENT != 0) {
                errorMessage.append("\n - ")
                    .append(context.getString(R.string.upload_problem_different_geolocation))
            }
            if (result and FILE_FBMD != 0) {
                errorMessage.append("\n - ")
                    .append(context.getString(R.string.upload_problem_fbmd))
            }
            if (result and FILE_NO_EXIF != 0) {
                errorMessage.append("\n - ")
                    .append(context.getString(R.string.internet_downloaded))
            }
            errorMessage.append("\n\n")
                .append(context.getString(R.string.upload_problem_do_you_continue))
        }
        return errorMessage.toString()
    }
}