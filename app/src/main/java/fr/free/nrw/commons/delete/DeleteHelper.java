package fr.free.nrw.commons.delete;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.review.ReviewActivity;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Single;
import timber.log.Timber;

import static androidx.core.app.NotificationCompat.DEFAULT_ALL;
import static androidx.core.app.NotificationCompat.PRIORITY_HIGH;

@Singleton
public class DeleteHelper {
    private static final int NOTIFICATION_DELETE = 1;

    private final MediaWikiApi mwApi;
    private final SessionManager sessionManager;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    @Inject
    public DeleteHelper(MediaWikiApi mwApi, SessionManager sessionManager) {
        this.mwApi = mwApi;
        this.sessionManager = sessionManager;
    }

    public Single<Boolean> makeDeletion(Context context, Media media, String reason) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context, CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL)
                .setOnlyAlertOnce(true);
        ViewUtil.showShortToast(context, "Trying to nominate " + media.getDisplayTitle() + " for deletion");

        return Single.fromCallable(() -> delete(media, reason))
                .flatMap(result -> Single.fromCallable(() ->
                        showDeletionNotification(context, media, result)));
    }

    private boolean delete(Media media, String reason) {
        String editToken;
        String authCookie;
        String summary = "Nominating " + media.getFilename() + " for deletion.";

        authCookie = sessionManager.getAuthCookie();
        mwApi.setAuthCookie(authCookie);

        Calendar calendar = Calendar.getInstance();
        String fileDeleteString = "{{delete|reason=" + reason +
                "|subpage=" + media.getFilename() +
                "|day=" + calendar.get(Calendar.DAY_OF_MONTH) +
                "|month=" + calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) +
                "|year=" + calendar.get(Calendar.YEAR) +
                "}}";

        String subpageString = "=== [[:" + media.getFilename() + "]] ===\n" +
                reason +
                " ~~~~";

        String logPageString = "\n{{Commons:Deletion requests/" + media.getFilename() +
                "}}\n";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        String date = sdf.format(calendar.getTime());

        String userPageString = "\n{{subst:idw|" + media.getFilename() +
                "}} ~~~~";

        try {
            editToken = mwApi.getEditToken();
            if (editToken.equals("+\\")) {
                return false;
            }

            mwApi.prependEdit(editToken, fileDeleteString + "\n",
                    media.getFilename(), summary);
            mwApi.edit(editToken, subpageString + "\n",
                    "Commons:Deletion_requests/" + media.getFilename(), summary);
            mwApi.appendEdit(editToken, logPageString + "\n",
                    "Commons:Deletion_requests/" + date, summary);
            mwApi.appendEdit(editToken, userPageString + "\n",
                    "User_Talk:" + sessionManager.getCurrentAccount().name, summary);
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
        return true;
    }

    public boolean showDeletionNotification(Context context, Media media, boolean result) {
        String message;
        String title = "Nominating for Deletion";

        if (result) {
            title += ": Success";
            message = "Successfully nominated " + media.getDisplayTitle() + " deletion.";
        } else {
            title += ": Failed";
            message = "Could not request deletion.";
        }

        notificationBuilder.setDefaults(DEFAULT_ALL)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setPriority(PRIORITY_HIGH);

        String urlForDelete = BuildConfig.COMMONS_URL + "/wiki/Commons:Deletion_requests/File:" + media.getFilename();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlForDelete));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, browserIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationManager.notify(NOTIFICATION_DELETE, notificationBuilder.build());
        return result;
    }

    public void askReasonAndExecute(Media media, Context context, String question, String problem) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(question);

        boolean[] checkedItems = {false, false, false, false};
        ArrayList<Integer> mUserReason = new ArrayList<>();

        String[] reasonList = {"Reason 1", "Reason 2", "Reason 3", "Reason 4"};


        if (problem.equals("spam")) {
            reasonList[0] = "A selfie";
            reasonList[1] = "Blurry";
            reasonList[2] = "Nonsense";
            reasonList[3] = "Other";
        } else if (problem.equals("copyRightViolation")) {
            reasonList[0] = "Press photo";
            reasonList[1] = "Random photo from internet";
            reasonList[2] = "Logo";
            reasonList[3] = "Other";
        }

        alert.setMultiChoiceItems(reasonList, checkedItems, (dialogInterface, position, isChecked) -> {
            if (isChecked) {
                mUserReason.add(position);
            } else {
                mUserReason.remove((Integer.valueOf(position)));
            }
        });

        alert.setPositiveButton("OK", (dialogInterface, i) -> {

            String reason = "Because it is ";
            for (int j = 0; j < mUserReason.size(); j++) {
                reason = reason + reasonList[mUserReason.get(j)];
                if (j != mUserReason.size() - 1) {
                    reason = reason + ", ";
                }
            }

            ((ReviewActivity) context).reviewController.swipeToNext();
            ((ReviewActivity) context).runRandomizer();

            makeDeletion(context, media, reason);
        });
        alert.setNegativeButton("Cancel", null);
        AlertDialog d = alert.create();
        d.show();
    }
}
