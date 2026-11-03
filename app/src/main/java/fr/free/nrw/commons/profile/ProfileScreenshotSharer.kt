package fr.free.nrw.commons.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.utils.DialogUtil
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/** Captures, previews, and shares screenshots of a profile. */
class ProfileScreenshotSharer(
    private val activity: Activity,
) {
    /** Shows a preview and asks whether to share the screenshot. */
    fun showAlert(screenshot: Bitmap) {
        val view = activity.layoutInflater.inflate(R.layout.image_alert_layout, null)
        val screenShotImage = view.findViewById<ImageView>(R.id.alert_image)
        val shareMessage = view.findViewById<TextView>(R.id.alert_text)

        screenShotImage.setImageBitmap(screenshot)
        shareMessage.setText(R.string.achievements_share_message)

        DialogUtil.showAlertDialog(
            activity,
            null,
            null,
            activity.getString(R.string.about_translate_proceed),
            activity.getString(R.string.cancel),
            { shareScreen(screenshot) },
            {},
            view,
        )
    }

    /** Writes and shares the screenshot through the Android chooser. */
    private fun shareScreen(bitmap: Bitmap) {
        runCatching {
            val file = File(activity.externalCacheDir, "screen.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            file.setReadable(true, false)

            val fileUri =
                FileProvider.getUriForFile(
                    activity.applicationContext,
                    "${activity.packageName}.provider",
                    file,
                )

            activity.grantUriPermission(activity.packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    type = "image/png"
                }
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_image_via)))
        }.onFailure { e ->
            if (e !is Exception) {
                throw e
            }
            Timber.e(e, "Could not share screenshot")
        }
    }

    /**
     * To take screenshot of the screen and return it in Bitmap format
     *
     * @param view
     * @return
     */
    fun getScreenShot(view: View): Bitmap? {
        val screenView = view.rootView
        screenView.isDrawingCacheEnabled = true
        val drawingCache = screenView.drawingCache
        if (drawingCache != null) {
            val bitmap = Bitmap.createBitmap(drawingCache)
            screenView.isDrawingCacheEnabled = false
            return bitmap
        }
        return null
    }
}
