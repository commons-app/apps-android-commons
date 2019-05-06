package fr.free.nrw.commons.achievements;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dinuscxj.progressbar.CircleProgressBar;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import javax.inject.Inject;

import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;



/**
 * activity for sharing feedback on uploaded activity
 */
public class AchievementsActivity extends NavigationBaseActivity {

    private static final double BADGE_IMAGE_WIDTH_RATIO = 0.4;
    private static final double BADGE_IMAGE_HEIGHT_RATIO = 0.3;

    private LevelController.LevelInfo levelInfo;

    @BindView(R.id.achievement_badge_image)
    ImageView imageView;
    @BindView(R.id.achievement_badge_text)
    TextView badgeText;
    @BindView(R.id.achievement_level)
    TextView levelNumber;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.thanks_received)
    TextView thanksReceived;
    @BindView(R.id.images_uploaded_progressbar)
    CircleProgressBar imagesUploadedProgressbar;
    @BindView(R.id.images_used_by_wiki_progress_bar)
    CircleProgressBar imagesUsedByWikiProgressBar;
    @BindView(R.id.image_reverts_progressbar)
    CircleProgressBar imageRevertsProgressbar;
    @BindView(R.id.image_featured)
    TextView imagesFeatured;
    @BindView(R.id.images_revert_limit_text)
    TextView imagesRevertLimitText;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.layout_image_uploaded)
    RelativeLayout layoutImageUploaded;
    @BindView(R.id.layout_image_reverts)
    RelativeLayout layoutImageReverts;
    @BindView(R.id.layout_image_used_by_wiki)
    RelativeLayout layoutImageUsedByWiki;
    @BindView(R.id.layout_statistics)
    LinearLayout layoutStatistics;
    @BindView(R.id.images_used_by_wiki_text)
    TextView imageByWikiText;
    @BindView(R.id.images_reverted_text)
    TextView imageRevertedText;
    @BindView(R.id.images_upload_text_param)
    TextView imageUploadedText;
    @BindView(R.id.wikidata_edits)
    TextView wikidataEditsText;


    @Inject
    SessionManager sessionManager;
    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;
    MenuItem item;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    /**
     * This method helps in the creation Achievement screen and
     * dynamically set the size of imageView
     *
     * @param savedInstanceState Data bundle
     */
    @Override
    @SuppressLint("StringFormatInvalid")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);
        ButterKnife.bind(this);

        // DisplayMetrics used to fetch the size of the screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        // Used for the setting the size of imageView at runtime
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)
                imageView.getLayoutParams();
        params.height = (int) (height * BADGE_IMAGE_HEIGHT_RATIO);
        params.width = (int) (width * BADGE_IMAGE_WIDTH_RATIO);
        imageView.requestLayout();

        setSupportActionBar(toolbar);
        progressBar.setVisibility(View.VISIBLE);

        hideLayouts();
        setAchievements();
        setWikidataEditCount();
        initDrawer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    /**
     * To invoke the AlertDialog on clicking info button
     */
    @OnClick(R.id.achievement_info)
    public void showInfoDialog(){
        launchAlert(getResources().getString(R.string.Achievements)
                ,getResources().getString(R.string.achievements_info_message));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_about, menu);
        item=menu.getItem(0);
        item.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.share_app_icon) {
            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            Bitmap screenShot = Utils.getScreenShot(rootView);
            showAlert(screenShot);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * To take bitmap and store it temporary storage and share it
     * @param bitmap
     */
    void shareScreen(Bitmap bitmap) {
        try {
            File file = new File(this.getExternalCacheDir(), "screen.png");
            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            file.setReadable(true, false);
            Uri fileUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName()+".provider", file);
            grantUriPermission(getPackageName(), fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent, "Share image via"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * To call the API to get results in form Single<JSONObject>
     * which then calls parseJson when results are fetched
     */
    private void setAchievements() {
        progressBar.setVisibility(View.VISIBLE);
        if (checkAccount()) {
            try{

                compositeDisposable.add(okHttpJsonApiClient
                        .getAchievements(Objects.requireNonNull(sessionManager.getCurrentAccount()).name)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                response -> {
                                    if (response != null) {
                                        setUploadCount(Achievements.from(response));
                                    } else {
                                        Timber.d("success");
                                        layoutImageReverts.setVisibility(View.INVISIBLE);
                                        imageView.setVisibility(View.INVISIBLE);
                                        showSnackBarWithRetry();
                                    }
                                },
                                t -> {
                                    Timber.e(t, "Fetching achievements statistics failed");
                                    showSnackBarWithRetry();
                                }
                        ));
            }
            catch (Exception e){
                Timber.d(e+"success");
            }
        }
    }

    @SuppressLint("CheckResult")
    private void setWikidataEditCount() {
        String userName = sessionManager.getUserName();
        if (StringUtils.isBlank(userName)) {
            return;
        }
        compositeDisposable.add(okHttpJsonApiClient.getWikidataEdits(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(edits -> wikidataEditsText.setText(String.valueOf(edits)), e -> {
                    Timber.e("Error:" + e);
                }));
    }

    private void showSnackBarWithRetry() {
        progressBar.setVisibility(View.GONE);
        ViewUtil.showDismissibleSnackBar(findViewById(android.R.id.content),
                R.string.achievements_fetch_failed, R.string.retry, view -> setAchievements());
    }

    /**
     * Shows a generic error toast when error occurs while loading achievements or uploads
     */
    private void onError() {
        ViewUtil.showLongToast(this, getResources().getString(R.string.error_occurred));
        progressBar.setVisibility(View.GONE);
    }

    /**
     * used to the count of images uploaded by user
     */
    private void setUploadCount(Achievements achievements) {
        if (checkAccount()) {
            compositeDisposable.add(okHttpJsonApiClient
                    .getUploadCount(Objects.requireNonNull(sessionManager.getCurrentAccount()).name)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            uploadCount -> setAchievementsUploadCount(achievements, uploadCount),
                            t -> {
                                Timber.e(t, "Fetching upload count failed");
                                onError();
                            }
                    ));
        }
    }

    /**
     * used to set achievements upload count and call hideProgressbar
     * @param uploadCount
     */
    private void setAchievementsUploadCount(Achievements achievements, int uploadCount) {
        achievements.setImagesUploaded(uploadCount);
        hideProgressBar(achievements);
    }

    /**
     * used to the uploaded images progressbar
     * @param uploadCount
     */
    private void setUploadProgress(int uploadCount){
        if (uploadCount==0){
            setZeroAchievements();
        }else {

            imagesUploadedProgressbar.setProgress
                    (100*uploadCount/levelInfo.getMaxUploadCount());
            imagesUploadedProgressbar.setProgressTextFormatPattern
                    (uploadCount +"/" + levelInfo.getMaxUploadCount() );
        }

    }

    private void setZeroAchievements() {
        AlertDialog.Builder builder=new AlertDialog.Builder(this)
                .setMessage("You haven't made any contributions yet")
                .setPositiveButton("Ok", (dialog, which) -> {
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        imagesUploadedProgressbar.setVisibility(View.INVISIBLE);
        imageRevertsProgressbar.setVisibility(View.INVISIBLE);
        imagesUsedByWikiProgressBar.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.INVISIBLE);
        imageByWikiText.setText(R.string.no_image);
        imageRevertedText.setText(R.string.no_image_reverted);
        imageUploadedText.setText(R.string.no_image_uploaded);
        imageView.setVisibility(View.INVISIBLE);

    }

    /**
     * used to set the non revert image percentage
     * @param notRevertPercentage
     */
    private void setImageRevertPercentage(int notRevertPercentage){
        imageRevertsProgressbar.setProgress(notRevertPercentage);
        String revertPercentage = Integer.toString(notRevertPercentage);
        imageRevertsProgressbar.setProgressTextFormatPattern(revertPercentage + "%%");
        imagesRevertLimitText.setText(getResources().getString(R.string.achievements_revert_limit_message)+ levelInfo.getMinNonRevertPercentage() + "%");
    }

    /**
     * Used the inflate the fetched statistics of the images uploaded by user
     * and assign badge and level
     * @param achievements
     */
    private void inflateAchievements(Achievements achievements) {
        thanksReceived.setText(String.valueOf(achievements.getThanksReceived()));
        imagesUsedByWikiProgressBar.setProgress
                (100*achievements.getUniqueUsedImages()/levelInfo.getMaxUniqueImages() );
        imagesUsedByWikiProgressBar.setProgressTextFormatPattern
                (achievements.getUniqueUsedImages() + "/" + levelInfo.getMaxUniqueImages());
        imagesFeatured.setText(String.valueOf(achievements.getFeaturedImages()));
        String levelUpInfoString = getString(R.string.level);
        levelUpInfoString += " " + Integer.toString(levelInfo.getLevelNumber());
        levelNumber.setText(levelUpInfoString);
        imageView.setImageDrawable(VectorDrawableCompat.create(getResources(), R.drawable.badge,
                new ContextThemeWrapper(this, levelInfo.getLevelStyle()).getTheme()));
        badgeText.setText(Integer.toString(levelInfo.getLevelNumber()));
    }

    /**
     * Creates a way to change current activity to AchievementActivity
     * @param context
     */
    public static void startYourself(Context context) {
        Intent intent = new Intent(context, AchievementsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    /**
     * to hide progressbar
     */
    private void hideProgressBar(Achievements achievements) {
        if (progressBar != null) {
            levelInfo = LevelController.LevelInfo.from(achievements.getImagesUploaded(),
                    achievements.getUniqueUsedImages(),
                    achievements.getNotRevertPercentage());
            inflateAchievements(achievements);
            setUploadProgress(achievements.getImagesUploaded());
            setImageRevertPercentage(achievements.getNotRevertPercentage());
            progressBar.setVisibility(View.GONE);
            item.setVisible(true);
            layoutImageReverts.setVisibility(View.VISIBLE);
            layoutImageUploaded.setVisibility(View.VISIBLE);
            layoutImageUsedByWiki.setVisibility(View.VISIBLE);
            layoutStatistics.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
            levelNumber.setVisibility(View.VISIBLE);
        }
    }

    /**
     * used to hide the layouts while fetching results from api
     */
    private void hideLayouts(){
        layoutImageUsedByWiki.setVisibility(View.INVISIBLE);
        layoutImageUploaded.setVisibility(View.INVISIBLE);
        layoutImageReverts.setVisibility(View.INVISIBLE);
        layoutStatistics.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.INVISIBLE);
        levelNumber.setVisibility(View.INVISIBLE);
    }

    /**
     * It display the alertDialog with Image of screenshot
     * @param screenshot
     */
    public void showAlert(Bitmap screenshot){
        AlertDialog.Builder alertadd = new AlertDialog.Builder(AchievementsActivity.this);
        LayoutInflater factory = LayoutInflater.from(AchievementsActivity.this);
        final View view = factory.inflate(R.layout.image_alert_layout, null);
        ImageView screenShotImage = view.findViewById(R.id.alert_image);
        screenShotImage.setImageBitmap(screenshot);
        TextView shareMessage = view.findViewById(R.id.alert_text);
        shareMessage.setText(R.string.achievements_share_message);
        alertadd.setView(view);
        alertadd.setPositiveButton(R.string.about_translate_proceed, (dialog, which) -> shareScreen(screenshot));
        alertadd.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        alertadd.show();
    }

    @OnClick(R.id.images_upload_info)
    public void showUploadInfo(){
        launchAlert(getResources().getString(R.string.images_uploaded)
                ,getResources().getString(R.string.images_uploaded_explanation));
    }

    @OnClick(R.id.images_reverted_info)
    public void showRevertedInfo(){
        launchAlert(getResources().getString(R.string.image_reverts)
                ,getResources().getString(R.string.images_reverted_explanation));
    }

    @OnClick(R.id.images_used_by_wiki_info)
    public void showUsedByWikiInfo(){
        launchAlert(getResources().getString(R.string.images_used_by_wiki)
                ,getResources().getString(R.string.images_used_explanation));
    }

    @OnClick(R.id.images_nearby_info)
    public void showImagesViaNearbyInfo(){
        launchAlert(getResources().getString(R.string.statistics_wikidata_edits)
                ,getResources().getString(R.string.images_via_nearby_explanation));
    }

    @OnClick(R.id.images_featured_info)
    public void showFeaturedImagesInfo(){
        launchAlert(getResources().getString(R.string.statistics_featured)
                ,getResources().getString(R.string.images_featured_explanation));
    }

    @OnClick(R.id.thanks_received_info)
    public void showThanksReceivedInfo(){
        launchAlert(getResources().getString(R.string.statistics_thanks)
                ,getResources().getString(R.string.thanks_received_explanation));
    }

    /**
     * takes title and message as input to display alerts
     * @param title
     * @param message
     */
    private void launchAlert(String title, String message){
        new AlertDialog.Builder(AchievementsActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                .create()
                .show();
    }

    /**
     * check to ensure that user is logged in
     * @return
     */
    private boolean checkAccount(){
        Account currentAccount = sessionManager.getCurrentAccount();
        if (currentAccount == null) {
            Timber.d("Current account is null");
            ViewUtil.showLongToast(this, getResources().getString(R.string.user_not_logged_in));
            sessionManager.forceLogin(this);
            return false;
        }
        return true;
    }

}
