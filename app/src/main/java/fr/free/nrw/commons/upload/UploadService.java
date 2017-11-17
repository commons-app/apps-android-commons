package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.contributions.ContributionsContentProvider;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;
import fr.free.nrw.commons.mwapi.EventLog;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.UploadResult;
import timber.log.Timber;

public class UploadService extends HandlerService<Contribution> {

    private static final String EXTRA_PREFIX = "fr.free.nrw.commons.upload";

    public static final int ACTION_UPLOAD_FILE = 1;

    public static final String ACTION_START_SERVICE = EXTRA_PREFIX + ".upload";
    public static final String EXTRA_SOURCE = EXTRA_PREFIX + ".source";
    public static final String EXTRA_CAMPAIGN = EXTRA_PREFIX + ".campaign";

    private NotificationManager notificationManager;
    private ContentProviderClient contributionsProviderClient;
    private CommonsApplication app;

    private NotificationCompat.Builder curProgressNotification;

    private int toUpload;

    // The file names of unfinished uploads, used to prevent overwriting
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

    private class NotificationUpdateProgressListener implements MediaWikiApi.ProgressListener {

        String notificationTag;
        boolean notificationTitleChanged;
        Contribution contribution;

        String notificationProgressTitle;
        String notificationFinishingTitle;

        public NotificationUpdateProgressListener(String notificationTag, String notificationProgressTitle, String notificationFinishingTitle, Contribution contribution) {
            this.notificationTag = notificationTag;
            this.notificationProgressTitle = notificationProgressTitle;
            this.notificationFinishingTitle = notificationFinishingTitle;
            this.contribution = contribution;
        }

        @Override
        public void onProgress(long transferred, long total) {
            Timber.d("Uploaded %d of %d", transferred, total);
            if (!notificationTitleChanged) {
                curProgressNotification.setContentTitle(notificationProgressTitle);
                notificationTitleChanged = true;
                contribution.setState(Contribution.STATE_IN_PROGRESS);
            }
            if (transferred == total) {
                // Completed!
                curProgressNotification.setContentTitle(notificationFinishingTitle);
                curProgressNotification.setProgress(0, 100, true);
            } else {
                curProgressNotification.setProgress(100, (int) (((double) transferred / (double) total) * 100), false);
            }
            startForeground(NOTIFICATION_UPLOAD_IN_PROGRESS, curProgressNotification.build());

            contribution.setTransferred(transferred);
            contribution.save();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        contributionsProviderClient.release();
        Timber.d("UploadService.onDestroy; %s are yet to be uploaded", unfinishedUploads);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        app = CommonsApplication.getInstance();
        contributionsProviderClient = this.getContentResolver().acquireContentProviderClient(ContributionsContentProvider.AUTHORITY);
    }

    @Override
    protected void handle(int what, Contribution contribution) {
        switch (what) {
            case ACTION_UPLOAD_FILE:
                //FIXME: Google Photos bug
                uploadContribution(contribution);
                break;
            default:
                throw new IllegalArgumentException("Unknown value for what");
        }
    }

    @Override
    public void queue(int what, Contribution contribution) {
        switch (what) {
            case ACTION_UPLOAD_FILE:

                contribution.setState(Contribution.STATE_QUEUED);
                contribution.setTransferred(0);
                contribution.setContentProviderClient(contributionsProviderClient);

                contribution.save();
                toUpload++;
                if (curProgressNotification != null && toUpload != 1) {
                    curProgressNotification.setContentText(getResources().getQuantityString(R.plurals.uploads_pending_notification_indicator, toUpload, toUpload));
                    Timber.d("%d uploads left", toUpload);
                    this.startForeground(NOTIFICATION_UPLOAD_IN_PROGRESS, curProgressNotification.build());
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
        if (intent.getAction().equals(ACTION_START_SERVICE) && freshStart) {
            ContentValues failedValues = new ContentValues();
            failedValues.put(Contribution.Table.COLUMN_STATE, Contribution.STATE_FAILED);

            int updated = getContentResolver().update(ContributionsContentProvider.BASE_URI,
                    failedValues,
                    Contribution.Table.COLUMN_STATE + " = ? OR " + Contribution.Table.COLUMN_STATE + " = ?",
                    new String[]{ String.valueOf(Contribution.STATE_QUEUED), String.valueOf(Contribution.STATE_IN_PROGRESS) }
            );
            Timber.d("Set %d uploads to failed", updated);
            Timber.d("Flags is %d id is %d", flags, startId);
            freshStart = false;
        }
        return START_REDELIVER_INTENT;
    }

    @SuppressLint("StringFormatInvalid")
    private void uploadContribution(Contribution contribution) {
        MediaWikiApi api = app.getMWApi();

        InputStream file;

        String notificationTag = contribution.getLocalUri().toString();

        try {
            //FIXME: Google Photos bug
            file = this.getContentResolver().openInputStream(contribution.getLocalUri());
        } catch (FileNotFoundException e) {
            Timber.d("File not found");
            Toast fileNotFound = Toast.makeText(this, R.string.upload_failed, Toast.LENGTH_LONG);
            fileNotFound.show();
            return;
        }

        //As the file is null there's no point in continuing the upload process
        //mwapi.upload accepts a NonNull input stream
        if(file == null) {
            Timber.d("File not found");
            return;
        }

        Timber.d("Before execution!");
        curProgressNotification = new NotificationCompat.Builder(this).setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setAutoCancel(true)
                .setContentTitle(getString(R.string.upload_progress_notification_title_start, contribution.getDisplayTitle()))
                .setContentText(getResources().getQuantityString(R.plurals.uploads_pending_notification_indicator, toUpload, toUpload))
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ContributionsActivity.class), 0))
                .setTicker(getString(R.string.upload_progress_notification_title_in_progress, contribution.getDisplayTitle()));

        this.startForeground(NOTIFICATION_UPLOAD_IN_PROGRESS, curProgressNotification.build());

        String filename = null;
        try {
            filename = Utils.fixExtension(
                    contribution.getFilename(),
                    MimeTypeMap.getSingleton().getExtensionFromMimeType((String)contribution.getTag("mimeType")));

            synchronized (unfinishedUploads) {
                Timber.d("making sure of uniqueness of name: %s", filename);
                filename = findUniqueFilename(filename);
                unfinishedUploads.add(filename);
            }
            if (!api.validateLogin()) {
                // Need to revalidate!
                if (app.revalidateAuthToken()) {
                    Timber.d("Successfully revalidated token!");
                } else {
                    Timber.d("Unable to revalidate :(");
                    // TODO: Put up a new notification, ask them to re-login
                    stopForeground(true);
                    Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
                    failureToast.show();
                    return;
                }
            }
            NotificationUpdateProgressListener notificationUpdater = new NotificationUpdateProgressListener(notificationTag,
                    getString(R.string.upload_progress_notification_title_in_progress, contribution.getDisplayTitle()),
                    getString(R.string.upload_progress_notification_title_finishing, contribution.getDisplayTitle()),
                    contribution
            );
            UploadResult uploadResult = api.uploadFile(filename, file, contribution.getDataLength(), contribution.getPageContents(), contribution.getEditSummary(), notificationUpdater);

            Timber.d("Response is %s", uploadResult.toString());

            curProgressNotification = null;

            String resultStatus = uploadResult.getResultStatus();
            if (!resultStatus.equals("Success")) {
                showFailedNotification(contribution);
                EventLog.schema(CommonsApplication.EVENT_UPLOAD_ATTEMPT)
                        .param("username", app.getCurrentAccount().name)
                        .param("source", contribution.getSource())
                        .param("multiple", contribution.getMultiple())
                        .param("result", uploadResult.getErrorCode())
                        .param("filename", contribution.getFilename())
                        .log();
            } else {
                contribution.setFilename(uploadResult.getCanonicalFilename());
                contribution.setImageUrl(uploadResult.getImageUrl());
                contribution.setState(Contribution.STATE_COMPLETED);
                contribution.setDateUploaded(uploadResult.getDateUploaded());
                contribution.save();

                EventLog.schema(CommonsApplication.EVENT_UPLOAD_ATTEMPT)
                        .param("username", app.getCurrentAccount().name)
                        .param("source", contribution.getSource()) //FIXME
                        .param("filename", contribution.getFilename())
                        .param("multiple", contribution.getMultiple())
                        .param("result", "success")
                        .log();
            }
        } catch (IOException e) {
            Timber.d("I have a network fuckup");
            showFailedNotification(contribution);
        } finally {
            if (filename != null) {
                unfinishedUploads.remove(filename);
            }
            toUpload--;
            if (toUpload == 0) {
                // Sync modifications right after all uplaods are processed
                ContentResolver.requestSync((CommonsApplication.getInstance()).getCurrentAccount(), ModificationsContentProvider.AUTHORITY, new Bundle());
                stopForeground(true);
            }
        }
    }

    @SuppressLint("StringFormatInvalid")
    private void showFailedNotification(Contribution contribution) {
        Notification failureNotification = new NotificationCompat.Builder(this).setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ContributionsActivity.class), 0))
                .setTicker(getString(R.string.upload_failed_notification_title, contribution.getDisplayTitle()))
                .setContentTitle(getString(R.string.upload_failed_notification_title, contribution.getDisplayTitle()))
                .setContentText(getString(R.string.upload_failed_notification_subtitle))
                .build();
        notificationManager.notify(NOTIFICATION_UPLOAD_FAILED, failureNotification);

        contribution.setState(Contribution.STATE_FAILED);
        contribution.save();
    }

    private String findUniqueFilename(String fileName) throws IOException {
        MediaWikiApi api = app.getMWApi();
        String sequenceFileName;
        for (int sequenceNumber = 1; true; sequenceNumber++) {
            if (sequenceNumber == 1) {
                sequenceFileName = fileName;
            } else {
                if (fileName.indexOf('.') == -1) {
                    // We really should have appended a file type suffix already.
                    // But... we might not.
                    sequenceFileName = fileName + " " + sequenceNumber;
                } else {
                    Pattern regex = Pattern.compile("^(.*)(\\..+?)$");
                    Matcher regexMatcher = regex.matcher(fileName);
                    sequenceFileName = regexMatcher.replaceAll("$1 " + sequenceNumber + "$2");
                }
            }
            if (!api.fileExistsWithName(sequenceFileName)
                    && !unfinishedUploads.contains(sequenceFileName)) {
                break;
            }
        }
        return sequenceFileName;
    }
}
