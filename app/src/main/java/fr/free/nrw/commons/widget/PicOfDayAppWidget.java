package fr.free.nrw.commons.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.bumptech.glide.request.target.AppWidgetTarget;
import com.bumptech.glide.request.transition.Transition;
import com.prof.rssparser.Article;
import com.prof.rssparser.Parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.notification.GlideApp;

/**
 * Implementation of App Widget functionality.
 */
public class PicOfDayAppWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pic_of_day_app_widget);

        String urlString = BuildConfig.WIKIMEDIA_API_POTD;
        Parser parser = new Parser();
        parser.execute(urlString);
        parser.onFinish(new Parser.OnTaskCompleted() {
            @Override
            public void onTaskCompleted(ArrayList<Article> list) {
                String desc = list.get(list.size() - 1).getDescription();
                if (desc != null) {
                    Document document = Jsoup.parse(desc);
                    Elements elements = document.select("img");
                    String imageUrl = elements.get(0).attr("src");
                    if (imageUrl != null && imageUrl.length() > 0) {
                        AppWidgetTarget appWidgetTarget = new AppWidgetTarget(context, R.id.appwidget_image, views, appWidgetId) {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                super.onResourceReady(resource, transition);
                            }
                        };
                        GlideApp.with(context).asBitmap().load(imageUrl).into(appWidgetTarget);
                    }
                }

            }

            @Override
            public void onError() {
            }
        });

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
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