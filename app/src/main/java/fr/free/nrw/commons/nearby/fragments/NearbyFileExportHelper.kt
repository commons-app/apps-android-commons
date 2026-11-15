package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.core.content.FileProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DATE_FORMAT_PATTERN = "yyyyMMdd_HHmmss"
private const val KML_PREFIX = "KML_"
private const val KML_EXTENSION = ".kml"
private const val GPX_PREFIX = "GPX_"
private const val GPX_EXTENSION = ".gpx"
private const val MIME_TYPE_KML = "application/kml"
private const val MIME_TYPE_GPX = "application/gpx"
private const val PROVIDER_SUFFIX = ".provider"

/**
 * Handles exporting places in KML and GPX formats and managing file storage operations.
 */
class NearbyFileExportHelper(
    private var activityProvider: (() -> Activity?)? = null,
    private var nearbyControllerProvider: (() -> NearbyController?)? = null,
    private var compositeDisposableProvider: (() -> CompositeDisposable?)? = null,
    private var mapFocusProvider: (() -> LatLng?)? = null,
    private var viewCallback: ExportViewCallback? = null,
) : NearbyParentFragmentContract.FileExport {
    /**
     * Interface defining callbacks for file export UI state changes.
     */
    interface ExportViewCallback {
        fun showProgressDialog()

        fun hideProgressDialog()

        fun showErrorMessage(message: String)

        fun showToast(messageResId: Int)

        fun setProgressBarVisibility(isVisible: Boolean)

        fun lockUnlockNearby(isLocked: Boolean)

        fun setFilterState()
    }

    @VisibleForTesting
    internal var isStorageWritableProvider: () -> Boolean = {
        Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    @VisibleForTesting
    internal var downloadsDirProvider: () -> File = {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }

    private val isExternalStorageWritable: Boolean
        get() = isStorageWritableProvider()

    /**
     * Initializes helper dependencies and references.
     *
     * @param activityProvider Provider for the active activity
     * @param nearbyControllerProvider Provider for NearbyController
     * @param compositeDisposableProvider Provider for CompositeDisposable
     * @param mapFocusProvider Provider for map center coordinates
     * @param viewCallback Callback for notifying UI changes
     */
    fun init(
        activityProvider: () -> Activity?,
        nearbyControllerProvider: () -> NearbyController?,
        compositeDisposableProvider: () -> CompositeDisposable?,
        mapFocusProvider: () -> LatLng?,
        viewCallback: ExportViewCallback,
    ) {
        this.activityProvider = activityProvider
        this.nearbyControllerProvider = nearbyControllerProvider
        this.compositeDisposableProvider = compositeDisposableProvider
        this.mapFocusProvider = mapFocusProvider
        this.viewCallback = viewCallback
    }

    /**
     * Exports nearby places as a KML file using the current map focus.
     */
    override fun savePlacesAsKML() {
        savePlacesAsKML(mapFocusProvider?.invoke())
    }

    /**
     * Exports nearby places as a KML file.
     *
     * @param mapFocus Target LatLng location coordinates to center place queries around
     */
    fun savePlacesAsKML(mapFocus: LatLng?) {
        val nearbyController = nearbyControllerProvider?.invoke()
        val compositeDisposable = compositeDisposableProvider?.invoke()
        if (nearbyController == null || compositeDisposable == null) {
            return
        }
        val savePlacesObservable =
            Observable.fromCallable {
                nearbyController.getPlacesAsKML(mapFocus)
            }
        compositeDisposable.add(
            savePlacesObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { kmlString: String? ->
                        handleExportResponse(kmlString, false)
                    },
                    { throwable: Throwable ->
                        handleExportError(throwable)
                    },
                ),
        )
    }

    /**
     * Exports nearby places as a GPX file using the current map focus.
     */
    override fun savePlacesAsGPX() {
        savePlacesAsGPX(mapFocusProvider?.invoke())
    }

    /**
     * Exports nearby places as a GPX file.
     *
     * @param mapFocus Target LatLng location coordinates to center place queries around
     */
    fun savePlacesAsGPX(mapFocus: LatLng?) {
        val nearbyController = nearbyControllerProvider?.invoke()
        val compositeDisposable = compositeDisposableProvider?.invoke()
        if (nearbyController == null || compositeDisposable == null) {
            return
        }
        val savePlacesObservable =
            Observable.fromCallable {
                nearbyController.getPlacesAsGPX(mapFocus)
            }
        compositeDisposable.add(
            savePlacesObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { gpxString: String? ->
                        handleExportResponse(gpxString, true)
                    },
                    { throwable: Throwable ->
                        handleExportError(throwable)
                    },
                ),
        )
    }

    private fun handleExportResponse(
        content: String?,
        isGPX: Boolean,
    ) {
        if (content == null) {
            return
        }
        val context = activityProvider?.invoke() ?: return
        val prefix = if (isGPX) GPX_PREFIX else KML_PREFIX
        val ext = if (isGPX) GPX_EXTENSION else KML_EXTENSION
        val errorResId =
            if (isGPX) {
                R.string.failed_to_save_gpx_file
            } else {
                R.string.failed_to_save_kml_file
            }

        val timeStamp = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault()).format(Date())
        val fileName = prefix + timeStamp + "_" + System.currentTimeMillis() + ext
        val saved = saveFile(content, fileName)
        viewCallback?.hideProgressDialog()
        if (saved) {
            showOpenFileDialog(context, fileName, isGPX)
        } else {
            viewCallback?.showToast(errorResId)
        }
    }

    private fun handleExportError(throwable: Throwable) {
        Timber.d(throwable)
        val context = activityProvider?.invoke()
        val prefix = context?.getString(R.string.error_fetching_nearby_places) ?: ""
        viewCallback?.showErrorMessage(prefix + throwable.localizedMessage)
        viewCallback?.setProgressBarVisibility(false)
        viewCallback?.lockUnlockNearby(false)
        viewCallback?.setFilterState()
    }

    /**
     * Saves text content to a file in the public downloads directory.
     *
     * @param string The file content to write
     * @param fileName Target file name
     * @return True if successfully written, false otherwise
     */
    fun saveFile(
        string: String,
        fileName: String?,
    ): Boolean {
        if (!isExternalStorageWritable || fileName == null) {
            return false
        }

        val downloadsDir = downloadsDirProvider()
        val targetFile = File(downloadsDir, fileName)

        return try {
            val fos = FileOutputStream(targetFile)
            fos.write(string.toByteArray())
            fos.close()
            true
        } catch (e: IOException) {
            Timber.e(e, "Could not save file")
            false
        }
    }

    /**
     * Displays a dialog asking whether the user wants to open the exported file.
     *
     * @param context Active context
     * @param fileName Name of the exported file
     * @param isGPX True if GPX format, false for KML
     */
    fun showOpenFileDialog(
        context: Context,
        fileName: String,
        isGPX: Boolean,
    ) {
        val title = context.getString(R.string.file_saved_successfully)
        val message =
            if (isGPX) {
                context.getString(R.string.do_you_want_to_open_gpx_file)
            } else {
                context.getString(R.string.do_you_want_to_open_kml_file)
            }
        val runnable = Runnable { openFile(context, fileName, isGPX) }
        val activity = activityProvider?.invoke() ?: (context as? Activity)
        if (activity != null) {
            showAlertDialog(activity, title, message, runnable) {
                // No action needed when the dialog is dismissed
            }
        }
    }

    /**
     * Launches an external application to view the exported file via ACTION_VIEW intent.
     *
     * @param context Active context
     * @param fileName Name of the file to open
     * @param isGPX True if GPX format, false for KML
     */
    fun openFile(
        context: Context,
        fileName: String,
        isGPX: Boolean,
    ) {
        val file = File(downloadsDirProvider(), fileName)
        val uri =
            FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + PROVIDER_SUFFIX,
                file,
            )
        val intent = Intent(Intent.ACTION_VIEW)

        if (isGPX) {
            intent.setDataAndType(uri, MIME_TYPE_GPX)
        } else {
            intent.setDataAndType(uri, MIME_TYPE_KML)
        }

        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "No application available to open the GPX/KML file")
            Toast
                .makeText(
                    context,
                    R.string.no_application_available_to_open_gpx_files,
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }
}
