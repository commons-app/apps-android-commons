package fr.free.nrw.commons.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import javax.inject.Inject;

import fr.free.nrw.commons.FrescoImageLoader;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Implementation of App Widget functionality.
 */
public class PicOfDayAppWidget extends AppWidgetProvider {

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject OkHttpJsonApiClient okHttpJsonApiClient;

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pic_of_day_app_widget);
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
                                Bitmap bitmap = FrescoImageLoader.loadImageFromUrl(response.getImageUrl(), context);
                                views.setImageViewBitmap(R.id.appwidget_image, bitmap);
                                appWidgetManager.updateAppWidget(appWidgetId, views);
                            }
                        },
                        t -> Timber.e(t, "Fetching picture of the day failed")
                ));
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
