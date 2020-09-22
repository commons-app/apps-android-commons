package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.ChunkInfo;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.contributions.ContributionDao;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import fr.free.nrw.commons.di.CommonsDaggerService;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.utils.ViewUtil;
import fr.free.nrw.commons.wikidata.WikidataEditService;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.CompletableSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class UploadService extends CommonsDaggerService {

  private static final String EXTRA_PREFIX = "fr.free.nrw.commons.upload";

  private static final List<String> STASH_ERROR_CODES = Arrays
      .asList("uploadstash-file-not-found", "stashfailed", "verification-error", "chunk-too-small");

  public static final String ACTION_START_SERVICE = EXTRA_PREFIX + ".upload";
  public static final String PROCESS_PENDING_LIMITED_CONNECTION_MODE_UPLOADS = EXTRA_PREFIX + "process_limited_connection_mode_uploads";
  public static final String EXTRA_FILES = EXTRA_PREFIX + ".files";
  @Inject
  WikidataEditService wikidataEditService;
  @Inject
  SessionManager sessionManager;
  @Inject
  ContributionDao contributionDao;
  @Inject
  UploadClient uploadClient;
  @Inject
  MediaClient mediaClient;
  @Inject
  @Named(CommonsApplicationModule.MAIN_THREAD)
  Scheduler mainThreadScheduler;
  @Inject
  @Named(CommonsApplicationModule.IO_THREAD)
  Scheduler ioThreadScheduler;
  @Inject
  @Named("default_preferences")
  public JsonKvStore defaultKvStore;

  private NotificationManagerCompat notificationManager;
  private NotificationCompat.Builder curNotification;
  private int toUpload;
  private CompositeDisposable compositeDisposable;

  /**
   * The filePath names of unfinished uploads, used to prevent overwriting
   */
  private Set<String> unfinishedUploads = new HashSet<>();

  // DO NOT HAVE NOTIFICATION ID OF 0 FOR ANYTHING
  // See http://stackoverflow.com/questions/8725909/startforeground-does-not-show-my-notification
  // Seriously, Android?
  public static final int NOTIFICATION_UPLOAD_IN_PROGRESS = 1;
  public static final int NOTIFICATION_UPLOAD_FAILED = 3;
  public static final int NOTIFICATION_UPLOAD_PAUSED = 4;

  protected class NotificationUpdateProgressListener {

    String notificationTag;
    boolean notificationTitleChanged;
    Contribution contribution;

    String notificationProgressTitle;
    String notificationFinishingTitle;

    NotificationUpdateProgressListener(String notificationTag, String notificationProgressTitle,
        String notificationFinishingTitle, Contribution contribution) {
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
        curNotification
            .setProgress(100, (int) (((double) transferred / (double) total) * 100), false);
      }
      notificationManager
          .notify(notificationTag, NOTIFICATION_UPLOAD_IN_PROGRESS, curNotification.build());

      contribution.setTransferred(transferred);

      compositeDisposable.add(contributionDao.update(contribution)
          .subscribeOn(ioThreadScheduler)
          .subscribe());
    }

    public void onChunkUploaded(Contribution contribution, ChunkInfo chunkInfo) {
      contribution.setChunkInfo(chunkInfo);
      compositeDisposable.add(contributionDao.update(contribution)
          .subscribeOn(ioThreadScheduler)
          .subscribe());
    }
  }

  /**
   * Sets contribution state to paused and disposes the active disposable
   * @param contribution
   */
  public void pauseUpload(Contribution contribution) {
    uploadClient.pauseUpload();
    contribution.setState(Contribution.STATE_PAUSED);
    compositeDisposable.add(contributionDao.update(contribution)
        .subscribeOn(ioThreadScheduler)
        .subscribe());
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    compositeDisposable.dispose();
    Timber.d("UploadService.onDestroy; %s are yet to be uploaded", unfinishedUploads);
  }

  public class UploadServiceLocalBinder extends Binder {

    public UploadService getService() {
      return UploadService.this;
    }
  }

  private final IBinder localBinder = new UploadServiceLocalBinder();

  private PublishProcessor<Contribution> contributionsToUpload;

  @Override
  public IBinder onBind(Intent intent) {
    return localBinder;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    CommonsApplication.createNotificationChannel(getApplicationContext());
    compositeDisposable = new CompositeDisposable();
    notificationManager = NotificationManagerCompat.from(this);
    curNotification = getNotificationBuilder(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL);
    contributionsToUpload = PublishProcessor.create();
    compositeDisposable.add(contributionsToUpload.subscribe(this::handleUpload));
  }

  public void handleUpload(Contribution contribution) {
    contribution.setState(Contribution.STATE_QUEUED);
    contribution.setTransferred(0);
    toUpload++;
    if (curNotification != null && toUpload != 1) {
      curNotification.setContentText(getResources()
          .getQuantityString(R.plurals.uploads_pending_notification_indicator, toUpload, toUpload));
      Timber.d("%d uploads left", toUpload);
      notificationManager
          .notify(contribution.getLocalUri().toString(), NOTIFICATION_UPLOAD_IN_PROGRESS,
              curNotification.build());
    }

    compositeDisposable.add(contributionDao
        .save(contribution)
        .subscribeOn(ioThreadScheduler)
        .subscribe(() -> uploadContribution(contribution)));
  }

  private boolean freshStart = true;

  public void queue(Contribution contribution) {
    if (defaultKvStore
        .getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED, false)) {
      contribution.setState(Contribution.STATE_QUEUED_LIMITED_CONNECTION_MODE);
      contributionDao.save(contribution)
          .subscribeOn(ioThreadScheduler)
          .subscribe();
      return;
    }
    contributionsToUpload.offer(contribution);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (ACTION_START_SERVICE.equals(intent.getAction()) && freshStart) {
      compositeDisposable.add(contributionDao.updateStates(Contribution.STATE_FAILED,
          new int[]{Contribution.STATE_QUEUED, Contribution.STATE_IN_PROGRESS})
          .observeOn(mainThreadScheduler)
          .subscribeOn(ioThreadScheduler)
          .subscribe());
      freshStart = false;
    } else if (PROCESS_PENDING_LIMITED_CONNECTION_MODE_UPLOADS.equals(intent.getAction())) {
        contributionDao.getContribution(Contribution.STATE_QUEUED_LIMITED_CONNECTION_MODE)
            .flatMapObservable(
                (Function<List<Contribution>, ObservableSource<Contribution>>) contributions -> Observable.fromIterable(contributions))
            .concatMapCompletable(contribution -> Completable.fromAction(() -> queue(contribution)))
        .subscribeOn(ioThreadScheduler)
        .subscribe();
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
        .setContentIntent(
            PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
  }

  @SuppressLint("CheckResult")
  private void uploadContribution(Contribution contribution) {
    if (contribution.getLocalUri() == null || contribution.getLocalUri().getPath() == null) {
      Timber.d("localUri/path is null");
      return;
    }
    String notificationTag = contribution.getLocalUri().toString();

    Timber.d("Before execution!");
    final Media media = contribution.getMedia();
    final String displayTitle = media.getDisplayTitle();
    curNotification.setContentTitle(getString(R.string.upload_progress_notification_title_start,
        displayTitle))
        .setContentText(getResources()
            .getQuantityString(R.plurals.uploads_pending_notification_indicator, toUpload,
                toUpload))
        .setTicker(getString(R.string.upload_progress_notification_title_in_progress,
            displayTitle))
        .setOngoing(true);
    notificationManager
        .notify(notificationTag, NOTIFICATION_UPLOAD_IN_PROGRESS, curNotification.build());

    String filename = media.getFilename();

    NotificationUpdateProgressListener notificationUpdater = new NotificationUpdateProgressListener(
        notificationTag,
        getString(R.string.upload_progress_notification_title_in_progress,
            displayTitle),
        getString(R.string.upload_progress_notification_title_finishing,
            displayTitle),
        contribution
    );

    Observable.fromCallable(() -> "Temp_" + contribution.hashCode() + filename)
        .flatMap(stashFilename -> uploadClient
            .uploadFileToStash(getApplicationContext(), stashFilename, contribution,
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
            ContentResolver
                .requestSync(sessionManager.getCurrentAccount(), BuildConfig.MODIFICATION_AUTHORITY,
                    new Bundle());
            stopForeground(true);
          }
        })
        .flatMap(uploadStash -> {
          notificationManager.cancel(notificationTag, NOTIFICATION_UPLOAD_IN_PROGRESS);

          Timber.d("Stash upload response 1 is %s", uploadStash.toString());

          if (uploadStash.getState() == StashUploadState.SUCCESS) {
            Timber.d("making sure of uniqueness of name: %s", filename);
            String uniqueFilename = findUniqueFilename(filename);
            unfinishedUploads.add(uniqueFilename);
            return uploadClient.uploadFileFromStash(
                getApplicationContext(),
                contribution,
                uniqueFilename,
                uploadStash.getFileKey()).doOnError(new Consumer<Throwable>() {
              @Override
              public void accept(Throwable throwable) throws Exception {
                Timber.e(throwable, "Error occurred in uploading file from stash");
                if (STASH_ERROR_CODES.contains(throwable.getMessage())) {
                  clearChunks(contribution);
                }
              }
            });
          } else if (uploadStash.getState() == StashUploadState.PAUSED) {
            Timber.d("Contribution upload paused");
            showPausedNotification(contribution);
            return Observable.never();
          } else {
            Timber.d("Contribution upload failed. Wikidata entity won't be edited");
            showFailedNotification(contribution);
            return Observable.never();
          }
        })
        .subscribe(
            uploadResult -> onUpload(contribution, notificationTag, uploadResult),
            throwable -> {
              Timber.w(throwable, "Exception during upload");
              notificationManager.cancel(notificationTag, NOTIFICATION_UPLOAD_IN_PROGRESS);
              showFailedNotification(contribution);
            });
  }

  private void clearChunks(Contribution contribution) {
    contribution.setChunkInfo(null);
    compositeDisposable.add(contributionDao.update(contribution)
        .subscribeOn(ioThreadScheduler)
        .subscribe());
  }

  private void onUpload(Contribution contribution, String notificationTag,
      UploadResult uploadResult) {
    Timber.d("Stash upload response 2 is %s", uploadResult.toString());

    notificationManager.cancel(notificationTag, NOTIFICATION_UPLOAD_IN_PROGRESS);

    if (uploadResult.isSuccessful()) {
      onSuccessfulUpload(contribution, uploadResult);
    } else {
      Timber.d("Contribution upload failed. Wikidata entity won't be edited");
      showFailedNotification(contribution);
    }
  }

  private void onSuccessfulUpload(Contribution contribution, UploadResult uploadResult) {
    compositeDisposable
        .add(wikidataEditService.addDepictionsAndCaptions(uploadResult, contribution));
    WikidataPlace wikidataPlace = contribution.getWikidataPlace();
    if (wikidataPlace != null && wikidataPlace.getImageValue() == null) {
      if (!contribution.hasInvalidLocation()) {
        wikidataEditService.createClaim(wikidataPlace, uploadResult.getFilename(),
            contribution.getMedia().getCaptions());
      } else {
        ViewUtil.showShortToast(this, getString(R.string.wikidata_edit_failure));
        Timber
            .d("Image location and nearby place location mismatched, so Wikidata item won't be edited");
      }
    }
    saveCompletedContribution(contribution, uploadResult);
  }

  private void saveCompletedContribution(Contribution contribution, UploadResult uploadResult) {
    compositeDisposable.add(mediaClient.getMedia("File:" + uploadResult.getFilename())
        .map(contribution::completeWith)
        .flatMapCompletable(
            newContribution -> {
              newContribution.setDateModified(new Date());
              return contributionDao.saveAndDelete(contribution, newContribution);
            })
        .subscribe());
  }

  @SuppressLint("StringFormatInvalid")
  @SuppressWarnings("deprecation")
  private void showFailedNotification(Contribution contribution) {
    final String displayTitle = contribution.getMedia().getDisplayTitle();
    curNotification.setTicker(getString(R.string.upload_failed_notification_title, displayTitle))
        .setContentTitle(getString(R.string.upload_failed_notification_title, displayTitle))
        .setContentText(getString(R.string.upload_failed_notification_subtitle))
        .setProgress(0, 0, false)
        .setOngoing(false);
    notificationManager.notify(contribution.getLocalUri().toString(), NOTIFICATION_UPLOAD_FAILED,
        curNotification.build());

    contribution.setState(Contribution.STATE_FAILED);

    compositeDisposable.add(contributionDao
        .update(contribution)
        .subscribeOn(ioThreadScheduler)
        .subscribe());
  }

  private void showPausedNotification(Contribution contribution) {
    final String displayTitle = contribution.getMedia().getDisplayTitle();
    curNotification.setTicker(getString(R.string.upload_paused_notification_title, displayTitle))
        .setContentTitle(getString(R.string.upload_paused_notification_title, displayTitle))
        .setContentText(getString(R.string.upload_paused_notification_subtitle))
        .setProgress(0, 0, false)
        .setOngoing(false);
    notificationManager.notify(contribution.getLocalUri().toString(), NOTIFICATION_UPLOAD_PAUSED,
        curNotification.build());

    contribution.setState(Contribution.STATE_PAUSED);

    compositeDisposable.add(contributionDao
        .update(contribution)
        .subscribeOn(ioThreadScheduler)
        .subscribe());
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
      if (!mediaClient.checkPageExistsUsingTitle(String.format("File:%s", sequenceFileName))
          .blockingGet()
          && !unfinishedUploads.contains(sequenceFileName)) {
        break;
      }
    }
    return sequenceFileName;
  }
}
