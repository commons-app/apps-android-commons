package org.wikimedia.commons.campaigns;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;
import org.wikimedia.commons.CommonsApplication;
import org.wikimedia.commons.Utils;

import java.io.IOException;
import java.util.ArrayList;

public class FetchCampaignsTask extends AsyncTask<Void, Void, ArrayList<Campaign>>{

    private Context context;
    private MWApi api;

    public FetchCampaignsTask(Context context) {
        this.context = context;
        this.api = ((CommonsApplication)context.getApplicationContext()).getApi();
    }

    @Override
    protected ArrayList<Campaign> doInBackground(Void... voids) {
        ArrayList<Campaign> campaigns = new ArrayList<Campaign>();
        ApiResult result;
        try {
            result = api.action("query").param("prop", "revisions")
                    .param("rvprop", "content").param("generator", "allpages")
                    .param("gapnamespace", 460).param("gaplimit", 500).get(); //FIXME: Actually paginate!
        } catch (IOException e) {
            throw new RuntimeException(e); // FIXME: Do something else ;)
        }
        ArrayList<ApiResult> pageNodes = result.getNodes("/api/query/pages/page");
        for(ApiResult pageNode : pageNodes) {
            Log.d("Commons", Utils.getStringFromDOM(pageNode.getDocument()));
            String configString = pageNode.getString("revisions/rev");
            String pageName = pageNode.getString("@title").split(":")[1];
            JSONObject config;
            try {
                config = new JSONObject(configString);
            } catch (JSONException e) {
                throw new RuntimeException(e); // NEVER HAPPENS!
            }
            campaigns.add(new Campaign(pageName, config));
        }
        return campaigns;
    }
}
