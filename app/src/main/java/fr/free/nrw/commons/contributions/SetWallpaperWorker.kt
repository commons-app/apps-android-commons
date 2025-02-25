package fr.free.nrw.commons.contributions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequestBuilder
import fr.free.nrw.commons.R
import timber.log.Timber

class SetWallpaperWorker(context: Context, params: WorkerParameters) :
    Worker(context, params) {
    override fun doWork(): Result {
        val context = applicationContext
        createNotificationChannel(context)
        showProgressNotification(context)

        val imageUrl = inputData.getString("imageUrl") ?: return Result.failure()

        val imageRequest = ImageRequestBuilder
            .newBuilderWithSource(Uri.parse(imageUrl))
            .build()

        val imagePipeline = Fresco.getImagePipeline()
        val dataSource = imagePipeline.fetchDecodedImage(imageRequest, context)

        dataSource.subscribe(object : BaseBitmapDataSubscriber() {
            public override fun onNewResultImpl(bitmap: Bitmap?) {
                if (dataSource.isFinished && bitmap != null) {
                    Timber.d("Bitmap loaded from url %s", imageUrl.toString())
                    setWallpaper(context, Bitmap.createBitmap(bitmap))
                    dataSource.close()
                }
            }

            override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>?) {
                Timber.d("Error getting bitmap from image url %s", imageUrl.toString())
                showNotification(context, "Setting Wallpaper Failed", "Failed to download image.")
                dataSource?.close()
            }
        }, CallerThreadExecutor.getInstance())

        return Result.success()
    }

    private fun setWallpaper(context: Context, bitmap: Bitmap) {
        val wallpaperManager = WallpaperManager.getInstance(context)

        try {
            wallpaperManager.setBitmap(bitmap)
            showNotification(context, "Wallpaper Set", "Wallpaper has been updated successfully.")
        } catch (e: Exception) {
            Timber.e(e, "Error setting wallpaper")
            showNotification(context, "Setting Wallpaper Failed", " " + e.localizedMessage)
        }
    }

    private fun showProgressNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.commons_logo)
            .setContentTitle("Setting Wallpaper")
            .setContentText("Please wait...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setProgress(0, 0, true)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun showNotification(context: Context, title: String, content: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.commons_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(false)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Wallpaper Setting"
            val description = "Notifications for wallpaper setting progress"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = context.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "set_wallpaper_channel"
        private const val NOTIFICATION_ID = 1
    }
}
