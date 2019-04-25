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
import android.widget.RemoteViews;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.Intent.ACTION_VIEW;

/**
 * Implementation of App Widget functionality.
 */
public class PicOfDayAppWidget extends AppWidgetProvider {

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject OkHttpJsonApiClient okHttpJsonApiClient;

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pic_of_day_app_widget);

        // Launch App on Button Click
        Intent viewIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);
        views.setOnClickPendingIntent(R.id.camera_button, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);

        loadPictureOfTheDay(context, views, appWidgetManager, appWidgetId);
    }

    /**
     * Loads the picture of the day using media wiki API
     * @param context
     * @param views
     * @param appWidgetManager
     * @param appWidgetId
     */
    private void loadPictureOfTheDay(Context context,
                                     RemoteViews views,
                                     AppWidgetManager appWidgetManager,
                                     int appWidgetId) {
        compositeDisposable.add(okHttpJsonApiClient.getPictureOfTheDay()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response != null) {
                                views.setTextViewText(R.id.appwidget_title, response.getDisplayTitle());

                                // View in browser
                                Intent viewIntent = new Intent();
                                viewIntent.setAction(ACTION_VIEW);
                                viewIntent.setData(Uri.parse(response.getPageTitle().getMobileUri()));
                                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);
                                views.setOnClickPendingIntent(R.id.appwidget_image, pendingIntent);

                                loadImageFromUrl(response.getThumbUrl(), context, views, appWidgetManager, appWidgetId);
                            }
                        },
                        t -> Timber.e(t, "Fetching picture of the day failed")
                ));
    }

    /**
     * Uses Fresco to load an image from Url
     * @param imageUrl
     * @param context
     * @param views
     * @param appWidgetManager
     * @param appWidgetId
     */
    private void loadImageFromUrl(String imageUrl,
                                  Context context,
                                  RemoteViews views,
                                  AppWidgetManager appWidgetManager,
                                  int appWidgetId) {
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageUrl)).build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> dataSource
                = imagePipeline.fetchDecodedImage(request, context);
        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(@Nullable Bitmap tempBitmap) {
                Bitmap bitmap = null;
                if (tempBitmap != null) {
                    bitmap = Bitmap.createBitmap(tempBitmap.getWidth(), tempBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawBitmap(tempBitmap, 0f, 0f, new Paint());
                }
                views.setImageViewBitmap(R.id.appwidget_image, bitmap);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }

            @Override
            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                // Ignore failure for now.
            }
        }, CallerThreadExecutor.getInstance());
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ApplicationlessInjection
                .getInstance(context
                        .getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}
