package free.nrw.commons.campaigns;

import android.accounts.Account;
import android.content.*;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import free.nrw.commons.CommonsApplication;
import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;

import java.io.IOException;
import java.util.ArrayList;


public class CampaignsSyncAdapter extends AbstractThreadedSyncAdapter {
    private static int COMMIT_THRESHOLD = 10;
    public CampaignsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    private int getLimit() {
        return 500; // FIXME: Parameterize!
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        // This code is fraught with possibilities of race conditions, but lalalalala I can't hear you!
        String user = account.name;
        MWApi api = CommonsApplication.createMWApi();
        ApiResult result;
        Boolean done = false;
        String queryContinue = null;
        while(!done) {

            try {
                MWApi.RequestBuilder builder = api.action("query")
                        .param("list", "allcampaigns")
                        // Disabled, since we want to modify local state if the campaign was disabled
                        // FIXME: To be more effecient, delete the disabled campaigns locally
                        //.param("ucenabledonly", "true")
                        .param("uclimit", getLimit());
                if(!TextUtils.isEmpty(queryContinue)) {
                    builder.param("uccontinue", queryContinue);
                }
                result = builder.get();
            } catch (IOException e) {
                // There isn't really much we can do, eh?
                // FIXME: Perhaps add EventLogging?
                syncResult.stats.numIoExceptions += 1; // Not sure if this does anything. Shitty docs
                Log.d("Commons", "Syncing failed due to " + e.toString());
                return;
            }

            ArrayList<ApiResult> campaigns = result.getNodes("/api/query/allcampaigns/campaign");
            Log.d("Commons", campaigns.size() + " results!");
            ArrayList<ContentValues> campaignValues = new ArrayList<ContentValues>();
            for(ApiResult campaignItem: campaigns) {
                String name = campaignItem.getString("@name");
                String body = campaignItem.getString(".");
                Log.d("Commons", "Campaign body is " + body);
                String trackingCat = campaignItem.getString("@trackingCategory");
                Campaign campaign = Campaign.parse(name, body, trackingCat);
                campaignValues.add(campaign.toContentValues());

                if(campaignValues.size() % COMMIT_THRESHOLD == 0) {
                    try {
                        contentProviderClient.bulkInsert(CampaignsContentProvider.BASE_URI, campaignValues.toArray(new ContentValues[]{}));
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                    campaignValues.clear();
                }
            }

            if(campaignValues.size() != 0) {
                try {
                    contentProviderClient.bulkInsert(CampaignsContentProvider.BASE_URI, campaignValues.toArray(new ContentValues[]{}));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            queryContinue = result.getString("/api/query-continue/allcampaigns/@uccontinue");
            if(TextUtils.isEmpty(queryContinue)) {
                done = true;
            }
        }
    }
}
