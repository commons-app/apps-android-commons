package fr.free.nrw.commons.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;
import androidx.annotation.Nullable;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import fr.free.nrw.commons.media.MediaClient;
import javax.inject.Inject;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.Intent.ACTION_VIEW;

/**
 * Implementation of App Widget functionality.
 */
public class PicOfDayAppWidget extends AppWidgetProvider {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    MediaClient mediaClient;

    void updateAppWidget(
        final Context context,
        final AppWidgetManager appWidgetManager,
        final int appWidgetId
    ) {
        final RemoteViews views = new RemoteViews(
            context.getPackageName(), R.layout.pic_of_day_app_widget);

        // Launch App on Button Click
        final Intent viewIntent = new Intent(context, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        final PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, viewIntent, flags);

        views.setOnClickPendingIntent(R.id.camera_button, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);

        loadPictureOfTheDay(context, views, appWidgetManager, appWidgetId);
    }

    /**
     * Loads the picture of the day using media wiki API
     * @param context The application context.
     * @param views The RemoteViews object used to update the App Widget UI.
     * @param appWidgetManager The AppWidgetManager instance for managing the widget.
     * @param appWidgetId he ID of the App Widget to update.
     */
    private void loadPictureOfTheDay(
        final Context context,
        final RemoteViews views,
        final AppWidgetManager appWidgetManager,
        final int appWidgetId
    ) {
        compositeDisposable.add(mediaClient.getPictureOfTheDay()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    response -> {
                        if (response != null) {
                            views.setTextViewText(R.id.appwidget_title, response.getDisplayTitle());

                            // View in browser
                            final Intent viewIntent = new Intent();
                            viewIntent.setAction(ACTION_VIEW);
                            viewIntent.setData(Uri.parse(response.getPageTitle().getMobileUri()));

                            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
                                flags |= PendingIntent.FLAG_IMMUTABLE;
                            }
                            final PendingIntent pendingIntent = PendingIntent.getActivity(
                                context, 0, viewIntent, flags);

                            views.setOnClickPendingIntent(R.id.appwidget_image, pendingIntent);
                            loadImageFromUrl(response.getThumbUrl(),
                                context, views, appWidgetManager, appWidgetId);
                        }
                    },
                    t -> Timber.e(t, "Fetching picture of the day failed")
                ));
    }

    /**
     * Uses Fresco to load an image from Url
     * @param imageUrl The URL of the image to load.
     * @param context The application context.
     * @param views The RemoteViews object used to update the App Widget UI.
     * @param appWidgetManager The AppWidgetManager instance for managing the widget.
     * @param appWidgetId he ID of the App Widget to update.
     */
    private void loadImageFromUrl(
        final String imageUrl,
        final Context context,
        final RemoteViews views,
        final AppWidgetManager appWidgetManager,
        final int appWidgetId
    ) {
        final ImageRequest request = ImageRequestBuilder
            .newBuilderWithSource(Uri.parse(imageUrl)).build();
        final ImagePipeline imagePipeline = Fresco.getImagePipeline();
        final DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline
            .fetchDecodedImage(request, context);

        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(@Nullable final Bitmap tempBitmap) {
                Bitmap bitmap = null;
                if (tempBitmap != null) {
                    bitmap = Bitmap.createBitmap(
                        tempBitmap.getWidth(), tempBitmap.getHeight(), Bitmap.Config.ARGB_8888
                    );
                    final Canvas canvas = new Canvas(bitmap);
                    canvas.drawBitmap(tempBitmap, 0f, 0f, new Paint());
                }
                views.setImageViewBitmap(R.id.appwidget_image, bitmap);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }

            @Override
            protected void onFailureImpl(
                final DataSource<CloseableReference<CloseableImage>> dataSource
            ) {
                // Ignore failure for now.
            }
        }, CallerThreadExecutor.getInstance());
    }

    @Override
    public void onUpdate(
        final Context context,
        final AppWidgetManager appWidgetManager,
        final int[] appWidgetIds
    ) {
        ApplicationlessInjection
                .getInstance(context.getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);
        // There may be multiple widgets active, so update all of them
        for (final int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(final Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(final Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}
