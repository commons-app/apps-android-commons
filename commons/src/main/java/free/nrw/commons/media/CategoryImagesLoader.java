package free.nrw.commons.media;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import free.nrw.commons.CommonsApplication;
import free.nrw.commons.Media;
import org.mediawiki.api.ApiResult;
import free.nrw.commons.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CategoryImagesLoader extends AsyncTaskLoader<List<Media>>{
    private final CommonsApplication app;
    private final String category;

    public CategoryImagesLoader(Context context, String category) {
        super(context);
        this.app = (CommonsApplication) context.getApplicationContext();
        this.category = category;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        super.forceLoad();
    }

    @Override
    public List<Media> loadInBackground() {
        ArrayList<Media> mediaList = new ArrayList<Media>();
        ApiResult result;
        try {
            result = app.getApi().action("query")
                    .param("list", "categorymembers")
                    .param("cmtitle", "Category:" + category)
                    .param("cmprop", "title|timestamp")
                    .param("cmtype", "file")
                    .param("cmsort", "timestamp")
                    .param("cmdir", "descending")
                    .param("cmlimit", 50)
                    .get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Log.d("Commons", Utils.getStringFromDOM(result.getDocument()));

        List<ApiResult> members = result.getNodes("/api/query/categorymembers/cm");
        for(ApiResult member : members) {
            mediaList.add(new Media(member.getString("@title")));
        }
        return mediaList;
    }
}
