package fr.free.nrw.commons.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.prof.rssparser.Article;
import com.prof.rssparser.Parser;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.util.ArrayList;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;

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
                try {
                    if (desc != null) {
                        JSONObject data = XML.toJSONObject(desc);
                        JSONArray tr = data.optJSONObject("div").optJSONObject("div").optJSONObject("table").optJSONArray("tr");
                        JSONObject jsonObject = new JSONObject(String.valueOf(tr.opt(1)));
                        String imageUrl = jsonObject.optJSONObject("td").optJSONObject("a").optJSONObject("img").optString("src");
                        if (imageUrl != null && imageUrl.length() > 0) {
                            Picasso.get().load(imageUrl).into(views, R.id.appwidget_image, new int[]{appWidgetId});
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
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