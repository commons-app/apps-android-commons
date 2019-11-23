package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.contributions.ContributionDao;
import fr.free.nrw.commons.contributions.ContributionsContentProvider;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.utils.CommonsDateUtil;
import fr.free.nrw.commons.wikidata.WikidataEditService;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import timber.log.Timber;

public class UploadService extends HandlerService<Contribution> {

    private static final String EXTRA_PREFIX = "fr.free.nrw.commons.upload";

    public static final int ACTION_UPLOAD_FILE = 1;

    public static final String ACTION_START_SERVICE = EXTRA_PREFIX + ".upload";
    public static final String EXTRA_SOURCE = EXTRA_PREFIX + ".source";
    public static final String EXTRA_FILES = EXTRA_PREFIX + ".files";
    public static final String EXTRA_CAMPAIGN = EXTRA_PREFIX + ".campaign";

    @Inject WikidataEditService wikidataEditService;
    @Inject SessionManager sessionManager;
    @Inject ContributionDao contributionDao;
    @Inject UploadClient uploadClient;
    @Inject MediaClient mediaClient;

    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder curNotification;
    private int toUpload;

    /**
     * The filePath names of unfinished uploads, used to prevent overwriting
     */
    private Set<String> unfinishedUploads = new HashSet<>();

    // DO NOT HAVE NOTIFICATION ID OF 0 FOR ANYTHING
    // See http://stackoverflow.com/questions/8725909/startforeground-does-not-show-my-notification
    // Seriously, Android?
    public static final int NOTIFICATION_UPLOAD_IN_PROGRESS = 1;
    public static final int NOTIFICATION_UPLOAD_COMPLETE = 2;
    public static final int NOTIFICATION_UPLOAD_FAILED = 3;

    public UploadService() {
        super("UploadService");
    }

    protected class NotificationUpdateProgressListener{

        String notificationTag;
        boolean notificationTitleChanged;
        Contribution contribution;

        String notificationProgressTitle;
        String notificationFinishingTitle;

        NotificationUpdateProgressListener(String notificationTag, String notificationProgressTitle, String notificationFinishingTitle, Contribution contribution) {
            this.notificationTag = notificationTag;
            this.notificationProgressTitle = notificationProgressTitle;
            this.notificationFinishingTitle = notificationFinishingTitle;
            this.contribution = contribution;
        }

        public void onProgress(long transferred, long total) {
            if (!notificationTitleChanged) {
                curNotification.setContentTitle(notificationProgressTitle);
                notificationTitleChanged = true;
                contribution.setState(Contribution.STATE_IN_PROGRESS);
            }
            if (transferred == total) {
                // Completed!
                curNotification.setContentTitle(notificationFinishingTitle)
                        .setTicker(notificationFinishingTitle)
                        .setProgress(0, 100, true);
            } else {
                curNotification.setProgress(100, (int) (((double) transferred / (double) total) * 100), false);
            }
            notificationManager.notify(notificationTag, NOTIFICATION_UPLOAD_IN_PROGRESS, curNotification.build());

            contribution.setTransferred(transferred);
            contributionDao.save(contribution);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("UploadService.onDestroy; %s are yet to be uploaded", unfinishedUploads);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CommonsApplication.createNotificationChannel(getApplicationContext());
        notificationManager = NotificationManagerCompat.from(this);
        curNotification = getNotificationBuilder(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL);
    }

    @Override
    protected void handle(int what, Contribution contribution) {
        switch (what) {
            case ACTION_UPLOAD_FILE:
                uploadContribution(contribution);
                break;
            default:
                throw new IllegalArgumentException("Unknown value for what");
        }
    }

    @Override
    public void queue(int what, Contribution contribution) {
        Timber.d("Upload service queue has contribution with wiki data entity id as %s", contribution.getWikiDataEntityId());
        switch (what) {
            case ACTION_UPLOAD_FILE:

                contribution.setState(Contribution.STATE_QUEUED);
                contribution.setTransferred(0);
                contributionDao.save(contribution);
                toUpload++;
                if (curNotification != null && toUpload != 1) {
                    curNotification.setContentText(getResources().getQuantityString(R.plurals.uploads_pending_notification_indicator, toUpload, toUpload));
                    Timber.d("%d uploads left", toUpload);
                    notificationManager.notify(contribution.getLocalUri().toString(), NOTIFICATION_UPLOAD_IN_PROGRESS, curNotification.build());
                }

                super.queue(what, contribution);
                break;
            default:
                throw new IllegalArgumentException("Unknown value for what");
        }
    }

    private boolean freshStart = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_START_SERVICE.equals(intent.getAction()) && freshStart) {
            ContentValues failedValues = new ContentValues();
            failedValues.put(ContributionDao.Table.COLUMN_STATE, Contribution.STATE_FAILED);

            int updated = getContentResolver().update(ContributionsContentProvider.BASE_URI,
                    failedValues,
                    ContributionDao.Table.COLUMN_STATE + " = ? OR " + ContributionDao.Table.COLUMN_STATE + " = ?",
                    new String[]{ String.valueOf(Contribution.STATE_QUEUED), String.valueOf(Contribution.STATE_IN_PROGRESS) }
            );
            Timber.d("Set %d uploads to failed", updated);
            Timber.d("Flags is %d id is %d", flags, startId);
            freshStart = false;
        }
        return START_REDELIVER_INTENT;
    }

    @SuppressLint("StringFormatInvalid")
    private NotificationCompat.Builder getNotificationBuilder(String channelId) {
        return new NotificationCompat.Builder(this, channelId).setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, true)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
    }

    @SuppressLint("CheckResult")
    private void uploadContribution(Contribution contribution) {
        Uri localUri = contribution.getLocalUri();
        if (localUri == null || localUri.getPath() == null) {
            Timber.d("localUri/path is null");
            return;
        }
        String notificationTag = localUri.toString();
        File localFile = new File(localUri.getPath());

        Timber.d("Before execution!");
        curNotification.setContentTitle(getString(R.string.upload_progress_notification_title_start, contribution.getDisplayTitle()))
                .setContentText(getResources().getQuantityString(R.plurals.uploads_pending_notification_indicator, toUpload, toUpload))
                .setTicker(getString(R.string.upload_progress_notification_title_in_progress, contribution.getDisplayTitle()));
        notificationManager
                .notify(notificationTag, NOTIFICATION_UPLOAD_IN_PROGRESS, curNotification.build());

        String filename = contribution.getFilename();

        NotificationUpdateProgressListener notificationUpdater = new NotificationUpdateProgressListener(notificationTag,
                getString(R.string.upload_progress_notification_title_in_progress, contribution.getDisplayTitle()),
                getString(R.string.upload_progress_notification_title_finishing, contribution.getDisplayTitle()),
                contribution
        );

        Observable.fromCallable(() -> "Temp_" + contribution.hashCode() + filename)
                .flatMap(stashFilename -> uploadClient
                        .uploadFileToStash(getApplicationContext(), stashFilename, localFile,
                                notificationUpdater))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doFinally(() -> {
                    if (filename != null) {
                        unfinishedUploads.remove(filename);
                    }
                    toUpload--;
                    if (toUpload == 0) {
                        // Sync modifications right after all uploads are processed
                        ContentResolver.requestSync(sessionManager.getCurrentAccount(), BuildConfig.MODIFICATION_AUTHORITY, new Bundle());
                        stopForeground(true);
                    }
                })
                .flatMap(uploadStash -> {
                    notificationManager.cancel(notificationTag, NOTIFICATION_UPLOAD_IN_PROGRESS);

                    Timber.d("Stash upload response 1 is %s", uploadStash.toString());

                    String resultStatus = uploadStash.getResult();
                    if (!resultStatus.equals("Success")) {
                        Timber.d("Contribution upload failed. Wikidata entity won't be edited");
                        showFailedNotification(contribution);
                        return Observable.never();
                    } else {
                        Timber.d("making sure of uniqueness of name: %s", filename);
                        String uniqueFilename = findUniqueFilename(filename);
                        unfinishedUploads.add(uniqueFilename);
                        return uploadClient.uploadFileFromStash(
                                getApplicationContext(),
                                contribution,
                                uniqueFilename,
                                uploadStash.getFilekey());
                    }
                })
                .subscribe(uploadResult -> {
                    Timber.d("Stash upload response 2 is %s", uploadResult.toString());

                    notificationManager.cancel(notificationTag, NOTIFICATION_UPLOAD_IN_PROGRESS);

                    String resultStatus = uploadResult.getResult();
                    if (!resultStatus.equals("Success")) {
                        Timber.d("Contribution upload failed. Wikidata entity won't be edited");
                        showFailedNotification(contribution);
                    } else {
                        String canonicalFilename = "File:" + uploadResult.getFilename();
                        Timber.d("Contribution upload success. Initiating Wikidata edit for entity id %s",
                                contribution.getWikiDataEntityId());
                        wikidataEditService.createClaimWithLogging(contribution.getWikiDataEntityId(), canonicalFilename);
                        contribution.setFilename(canonicalFilename);
                        contribution.setImageUrl(uploadResult.getImageinfo().getOriginalUrl());
                        contribution.setState(Contribution.STATE_COMPLETED);
                        contribution.setDateUploaded(CommonsDateUtil.getIso8601DateFormatShort()
                                .parse(uploadResult.getImageinfo().getTimestamp()));
                        contributionDao.save(contribution);
                    }
                }, throwable -> {
                    Timber.w(throwable, "Exception during upload");
                    notificationManager.cancel(notificationTag, NOTIFICATION_UPLOAD_IN_PROGRESS);
                    showFailedNotification(contribution);
                });
    }

    @SuppressLint("StringFormatInvalid")
    @SuppressWarnings("deprecation")
    private void showFailedNotification(Contribution contribution) {
        curNotification.setTicker(getString(R.string.upload_failed_notification_title, contribution.getDisplayTitle()))
                .setContentTitle(getString(R.string.upload_failed_notification_title, contribution.getDisplayTitle()))
                .setContentText(getString(R.string.upload_failed_notification_subtitle))
                .setProgress(0, 0, false);
        notificationManager.notify(contribution.getLocalUri().toString(), NOTIFICATION_UPLOAD_FAILED, curNotification.build());

        contribution.setState(Contribution.STATE_FAILED);
        contributionDao.save(contribution);
    }

    private String findUniqueFilename(String fileName) throws IOException {
        String sequenceFileName;
        for (int sequenceNumber = 1; true; sequenceNumber++) {
            if (sequenceNumber == 1) {
                sequenceFileName = fileName;
            } else {
                if (fileName.indexOf('.') == -1) {
                    // We really should have appended a filePath type suffix already.
                    // But... we might not.
                    sequenceFileName = fileName + " " + sequenceNumber;
                } else {
                    Pattern regex = Pattern.compile("^(.*)(\\..+?)$");
                    Matcher regexMatcher = regex.matcher(fileName);
                    sequenceFileName = regexMatcher.replaceAll("$1 " + sequenceNumber + "$2");
                }
            }
            if (!mediaClient.checkPageExistsUsingTitle(String.format("File:%s",sequenceFileName)).blockingGet()
                    && !unfinishedUploads.contains(sequenceFileName)) {
                break;
            }
        }
        return sequenceFileName;
    }
}
