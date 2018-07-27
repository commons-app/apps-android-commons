package fr.free.nrw.commons.achievements;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
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
    private Boolean isUploadFetched = false;
    private Boolean isStatisticsFetched = false;
    private Boolean isRevertFetched = false;
    private Achievements achievements = new Achievements();
    private LevelController.LevelInfo levelInfo;

    @BindView(R.id.achievement_badge)
    ImageView imageView;
    @BindView(R.id.achievement_level)
    TextView levelNumber;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.thanks_received)
    TextView thanksReceived;
    @BindView(R.id.images_uploaded_progressbar)
    CircleProgressBar imagesUploadedProgressbar;
    @BindView(R.id.images_used_by_wiki_progressbar)
    CircleProgressBar imagesUsedByWikiProgessbar;
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
    @Inject
    SessionManager sessionManager;
    @Inject
    MediaWikiApi mediaWikiApi;

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
        /**
         * DisplayMetrics used to fetch the size of the screen
         */
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        /**
         * Used for the setting the size of imageView at runtime
         */
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)
                imageView.getLayoutParams();
        params.height = (int) (height * BADGE_IMAGE_HEIGHT_RATIO);
        params.width = (int) (width * BADGE_IMAGE_WIDTH_RATIO);
        imageView.setImageResource(R.drawable.badge);
        imageView.requestLayout();

        setSupportActionBar(toolbar);
        progressBar.setVisibility(View.VISIBLE);
        hideLayouts();
        setAchievements();
        setUploadCount();
        setRevertCount();
        initDrawer();
    }

    /**
     * to invoke the AlertDialog on clicking info button
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
     *
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
            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent, "Share image via"));
        } catch (IOException e) {
            //Do Nothing
        }
    }

    /**
     * To call the API to get results in form Single<JSONObject>
     * which then calls parseJson when results are fetched
     */
    private void setAchievements() {
        if(checkAccount()) {
            compositeDisposable.add(mediaWikiApi
                    .getAchievements(sessionManager.getCurrentAccount().name)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            jsonObject -> parseJson(jsonObject),
                            t -> Timber.e(t, "Fetching achievements statisticss failed")
                    ));
        }
    }

    /**
     * To call the API to get reverts count in form of JSONObject
     *
     */

    private void setRevertCount(){
        if(checkAccount()) {
            compositeDisposable.add(mediaWikiApi
                    .getRevertRespObjectSingle(sessionManager.getCurrentAccount().name)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            object -> parseJsonRevertCount(object),
                            t -> Timber.e(t, "Fetching revert count failed")
                    ));
        }
    }

    /**
     * used to set number of deleted images
     * @param object
     */
    private void parseJsonRevertCount(JSONObject object){
        try {
            achievements.setRevertCount(object.getInt("deletedUploads"));
        } catch (JSONException e) {
            Timber.d( e, e.getMessage());
        }
        isRevertFetched = true;
        hideProgressBar();
    }

    /**
     * used to the count of images uploaded by user
     */
    private void setUploadCount() {
        if(checkAccount()) {
            compositeDisposable.add(mediaWikiApi
                    .getUploadCount(sessionManager.getCurrentAccount().name)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            uploadCount -> setAchievementsUploadCount(uploadCount),
                            t -> Timber.e(t, "Fetching upload count failed")
                    ));
        }
    }

    /**
     * used to set achievements upload count and call hideProgressbar
     * @param uploadCount
     */
    private void setAchievementsUploadCount(int uploadCount){
        achievements.setImagesUploaded(uploadCount);
        isUploadFetched = true;
        hideProgressBar();
    }

    /**
     * used to the uploaded images progressbar
     * @param uploadCount
     */
    private void setUploadProgress(int uploadCount){
        imagesUploadedProgressbar.setProgress
                (100*uploadCount/levelInfo.getMaxUploadCount());
        imagesUploadedProgressbar.setProgressTextFormatPattern
                (uploadCount +"/" + levelInfo.getMaxUploadCount() );
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
     * used to parse the JSONObject containing results
     * @param object
     */
    private void parseJson(JSONObject object) {
        try {
            achievements.setUniqueUsedImages(object.getInt("uniqueUsedImages"));
            achievements.setArticlesUsingImages(object.getInt("articlesUsingImages"));
            achievements.setThanksReceived(object.getInt("thanksReceived"));
            achievements.setImagesEditedBySomeoneElse(object.getInt("imagesEditedBySomeoneElse"));
            JSONObject featuredImages = object.getJSONObject("featuredImages");
            achievements.setFeaturedImages
                    (featuredImages.getInt("Quality_images") +
                            featuredImages.getInt("Featured_pictures_on_Wikimedia_Commons"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        isStatisticsFetched = true;
        hideProgressBar();
    }

    /**
     * Used the inflate the fetched statistics of the images uploaded by user
     * and assign badge and level
     * @param achievements
     */
    private void inflateAchievements(Achievements achievements ){
        thanksReceived.setText(Integer.toString(achievements.getThanksReceived()));
        imagesUsedByWikiProgessbar.setProgress
                (100*achievements.getUniqueUsedImages()/levelInfo.getMaxUniqueImages() );
        imagesUsedByWikiProgessbar.setProgressTextFormatPattern
                (achievements.getUniqueUsedImages() + "/" + levelInfo.getMaxUniqueImages());
        imagesFeatured.setText(Integer.toString(achievements.getFeaturedImages()));
        String levelUpInfoString = getString(R.string.level);
        levelUpInfoString += " " + Integer.toString(levelInfo.getLevelNumber());
        levelNumber.setText(levelUpInfoString);
        final ContextThemeWrapper wrapper = new ContextThemeWrapper(this, levelInfo.getLevelStyle());
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.badge, wrapper.getTheme());
        Bitmap bitmap = BitmapUtils.drawableToBitmap(drawable);
        BitmapDrawable bitmapImage = BitmapUtils.writeOnDrawable(bitmap, Integer.toString(levelInfo.getLevelNumber()),this);
        imageView.setImageDrawable(bitmapImage);
    }

    /**
     * Creates a way to change current activity to AchievementActivity
     * @param context
     */
    public static void startYourself(Context context) {
        Intent intent = new Intent(context, AchievementsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    /**
     * to hide progressbar
     */
    private void hideProgressBar() {
        if (progressBar != null && isUploadFetched && isStatisticsFetched && isRevertFetched) {
            levelInfo = LevelController.LevelInfo.from(achievements.getImagesUploaded(),
                    achievements.getUniqueUsedImages(),
                    achievements.getNotRevertPercentage());
            inflateAchievements(achievements);
            setUploadProgress(achievements.getImagesUploaded());
            setImageRevertPercentage(achievements.getNotRevertPercentage());
            progressBar.setVisibility(View.GONE);
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
        ImageView screenShotImage = (ImageView) view.findViewById(R.id.alert_image);
        screenShotImage.setImageBitmap(screenshot);
        TextView shareMessage = (TextView) view.findViewById(R.id.alert_text);
        shareMessage.setText(R.string.achievements_share_message);
        alertadd.setView(view);
        alertadd.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                shareScreen(screenshot);
            }
        });
        alertadd.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
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
                .setNeutralButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                .create()
                .show();
    }

    /**
     * check to ensure that user is logged in
     * @return
     */
    private boolean checkAccount(){
        Account currentAccount = sessionManager.getCurrentAccount();
        if(currentAccount == null) {
        Timber.d("Current account is null");
        ViewUtil.showLongToast(this, getResources().getString(R.string.user_not_logged_in));
        sessionManager.forceLogin(this);
        return false;
        }
        return true;
    }

}
