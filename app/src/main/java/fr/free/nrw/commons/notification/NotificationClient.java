package fr.free.nrw.commons.notification;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.google.gson.JsonParseException;

import java.util.List;

import fr.free.nrw.commons.mwapi.MwQueryResponse;
import fr.free.nrw.commons.network.RetrofitFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import timber.log.Timber;

public final class NotificationClient {
    @NonNull
    private final Service service;

    public interface Callback {
        void success(@NonNull List<Notification> notifications);

        void failure(Throwable t);
    }

    public NotificationClient(@NonNull String endpoint) {
        service = RetrofitFactory.newInstance(endpoint).create(Service.class);
    }

    @VisibleForTesting
    static class CallbackAdapter implements retrofit2.Callback<MwQueryResponse<NotificationObject.QueryNotifications>> {
        @NonNull
        private final Callback callback;

        CallbackAdapter(@NonNull Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onResponse(Call<MwQueryResponse<NotificationObject.QueryNotifications>> call,
                               Response<MwQueryResponse<NotificationObject.QueryNotifications>> response) {
            Timber.d("Resonse is %s", response);
            if (response.body() != null && response.body().query() != null) {
                callback.success(response.body().query().get());
            } else {
                callback.failure(new JsonParseException("Notification response is malformed."));
            }
        }

        @Override
        public void onFailure(Call<MwQueryResponse<NotificationObject.QueryNotifications>> call, Throwable caught) {
            Timber.e(caught, "Error occurred while fetching notifications");
            callback.failure(caught);
        }
    }

    /**
     * Obrain a list of unread notifications for the user who is currently logged in.
     *
     * @param callback Callback that will receive the list of notifications.
     * @param wikis    List of wiki names for which notifications should be received. These must be
     *                 in the "DB name" format, as in "enwiki", "zhwiki", "wikidatawiki", etc.
     */
    public void getNotifications(@NonNull final Callback callback, @NonNull String... wikis) {
        String wikiList = TextUtils.join("|", wikis);
        requestNotifications(service, wikiList).enqueue(new CallbackAdapter(callback));
    }

    @VisibleForTesting
    @NonNull
    Call<MwQueryResponse<NotificationObject.QueryNotifications>> requestNotifications(@NonNull Service service, @NonNull String wikiList) {
        return service.getNotifications(wikiList);
    }

    @VisibleForTesting
    @NonNull
    Call<MwQueryResponse<MarkReadResponse.QueryMarkReadResponse>> requestMarkRead(@NonNull Service service, @NonNull String token, @NonNull String idList) {
        return service.markRead(token, idList);
    }

    @VisibleForTesting
    interface Service {
        String ACTION = "w/api.php?format=json&formatversion=2&action=";

        @GET(ACTION + "query&meta=notifications&notfilter=!read&notprop=list")
        @NonNull
        Call<MwQueryResponse<NotificationObject.QueryNotifications>> getNotifications(@Query("notwikis") @NonNull String wikiList);

        @FormUrlEncoded
        @POST(ACTION + "echomarkread")
        @NonNull
        Call<MwQueryResponse<MarkReadResponse.QueryMarkReadResponse>> markRead(@Field("token") @NonNull String token,
                                                                               @Field("list") @NonNull String idList);
    }
}
