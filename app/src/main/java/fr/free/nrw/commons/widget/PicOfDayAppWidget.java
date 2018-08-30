package fr.free.nrw.commons.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.Nullable;
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
import com.prof.rssparser.Article;
import com.prof.rssparser.Parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;

/**
 * Implementation of App Widget functionality.
 */
public class PicOfDayAppWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
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
                }
            }

            @Override
            public void onError() {
            }
        });
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