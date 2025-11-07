package fr.free.nrw.commons.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequestBuilder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * Entry point for injecting dependencies into PicOfDayAppWidget
 * AppWidgets cannot use @AndroidEntryPoint, so we use @EntryPoint instead
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PicOfDayAppWidgetEntryPoint {
    fun mediaClient(): MediaClient
}

/**
 * Implementation of App Widget functionality.
 */
class PicOfDayAppWidget : AppWidgetProvider() {

    private val compositeDisposable = CompositeDisposable()


    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        mediaClient: MediaClient
    ) {
        val views = RemoteViews(context.packageName, R.layout.pic_of_day_app_widget)

        // Launch App on Button Click
        val viewIntent = Intent(context, MainActivity::class.java)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, viewIntent, flags)
        views.setOnClickPendingIntent(R.id.camera_button, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)

        loadPictureOfTheDay(context, views, appWidgetManager, appWidgetId, mediaClient)
    }

    /**
     * Loads the picture of the day using media wiki API
     * @param context The application context.
     * @param views The RemoteViews object used to update the App Widget UI.
     * @param appWidgetManager The AppWidgetManager instance for managing the widget.
     * @param appWidgetId The ID of the App Widget to update.
     * @param mediaClient The MediaClient for fetching picture of the day
     */
    private fun loadPictureOfTheDay(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        mediaClient: MediaClient
    ) {
        compositeDisposable.add(
            mediaClient.getPictureOfTheDay()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { response ->
                        if (response != null) {
                            views.setTextViewText(R.id.appwidget_title, response.displayTitle)

                            // View in browser
                            val viewIntent = Intent().apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse(response.pageTitle.mobileUri)
                            }

                            var flags = PendingIntent.FLAG_UPDATE_CURRENT
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                flags = flags or PendingIntent.FLAG_IMMUTABLE
                            }
                            val pendingIntent = PendingIntent.getActivity(
                                context,
                                0,
                                viewIntent,
                                flags
                            )

                            views.setOnClickPendingIntent(R.id.appwidget_image, pendingIntent)
                            loadImageFromUrl(
                                response.thumbUrl,
                                context,
                                views,
                                appWidgetManager,
                                appWidgetId
                            )
                        }
                    },
                    { t -> Timber.e(t, "Fetching picture of the day failed") }
                )
        )
    }

    /**
     * Uses Fresco to load an image from Url
     * @param imageUrl The URL of the image to load.
     * @param context The application context.
     * @param views The RemoteViews object used to update the App Widget UI.
     * @param appWidgetManager The AppWidgetManager instance for managing the widget.
     * @param appWidgetId The ID of the App Widget to update.
     */
    private fun loadImageFromUrl(
        imageUrl: String?,
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageUrl)).build()
        val imagePipeline = Fresco.getImagePipeline()
        val dataSource = imagePipeline.fetchDecodedImage(request, context)

        dataSource.subscribe(object : BaseBitmapDataSubscriber() {
            override fun onNewResultImpl(tempBitmap: Bitmap?) {
                val bitmap = tempBitmap?.let {
                    Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_8888).apply {
                        Canvas(this).drawBitmap(it, 0f, 0f, Paint())
                    }
                }
                views.setImageViewBitmap(R.id.appwidget_image, bitmap)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
                // Ignore failure for now.
            }
        }, CallerThreadExecutor.getInstance())
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Get mediaClient from EntryPoint since AppWidgets cannot use @AndroidEntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            PicOfDayAppWidgetEntryPoint::class.java
        )
        val mediaClient = entryPoint.mediaClient()

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, mediaClient)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}
