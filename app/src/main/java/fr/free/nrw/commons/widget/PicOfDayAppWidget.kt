package fr.free.nrw.commons.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import fr.free.nrw.commons.media.MediaClient
import javax.inject.Inject
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.di.ApplicationlessInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * Implementation of App Widget functionality.
 */
class PicOfDayAppWidget : AppWidgetProvider() {

    private val compositeDisposable = CompositeDisposable()

    @Inject
    lateinit var mediaClient: MediaClient

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
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

        loadPictureOfTheDay(context, views, appWidgetManager, appWidgetId)
    }

    /**
     * Loads the picture of the day using media wiki API
     * @param context The application context.
     * @param views The RemoteViews object used to update the App Widget UI.
     * @param appWidgetManager The AppWidgetManager instance for managing the widget.
     * @param appWidgetId The ID of the App Widget to update.
     */
    private fun loadPictureOfTheDay(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
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
     * Uses Coil to load an image from Url
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
        if (imageUrl == null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.image.toBitmap()
                    views.setImageViewBitmap(R.id.appwidget_image, bitmap)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading image for widget")
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ApplicationlessInjection
            .getInstance(context.applicationContext)
            .commonsApplicationComponent
            .inject(this)

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}
