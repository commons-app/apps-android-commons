package fr.free.nrw.commons.profile.achievements;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.dinuscxj.progressbar.CircleProgressBar;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import fr.free.nrw.commons.profile.ProfileActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Objects;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import timber.log.Timber;

/**
 * fragment for sharing feedback on uploaded activity
 */
public class AchievementsFragment extends CommonsDaggerSupportFragment {

    private static final double BADGE_IMAGE_WIDTH_RATIO = 0.4;
    private static final double BADGE_IMAGE_HEIGHT_RATIO = 0.3;

    /**
     * Help link URLs
     */
    private static final String IMAGES_UPLOADED_URL = "https://commons.wikimedia.org/wiki/Commons:Project_scope";
    private static final String IMAGES_REVERT_URL = "https://commons.wikimedia.org/wiki/Commons:Deletion_policy#Reasons_for_deletion";
    private static final String IMAGES_USED_URL = "https://en.wikipedia.org/wiki/Wikipedia:Manual_of_Style/Images";
    private static final String IMAGES_NEARBY_PLACES_URL = "https://www.wikidata.org/wiki/Property:P18";
    private static final String IMAGES_FEATURED_URL = "https://commons.wikimedia.org/wiki/Commons:Featured_pictures";
    private static final String QUALITY_IMAGE_URL = "https://commons.wikimedia.org/wiki/Commons:Quality_images";
    private static final String THANKS_URL = "https://www.mediawiki.org/wiki/Extension:Thanks";

    private LevelController.LevelInfo levelInfo;

    @BindView(R.id.achievement_badge_image)
    ImageView imageView;

    @BindView(R.id.achievement_badge_text)
    TextView badgeText;

    @BindView(R.id.achievement_level)
    TextView levelNumber;

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

    @BindView(R.id.quality_images)
    TextView tvQualityImages;

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

    @BindView(R.id.tv_achievements_of_user)
    AppCompatTextView tvAchievementsOfUser;

    @Inject
    SessionManager sessionManager;

    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    // To keep track of the number of wiki edits made by a user
    private int numberOfEdits = 0;

    // menu item for action bar
    private MenuItem item;

    private String userName;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString(ProfileActivity.KEY_USERNAME);
        }
    }

    /**
     * This method helps in the creation Achievement screen and
     * dynamically set the size of imageView
     *
     * @param savedInstanceState Data bundle
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_achievements, container, false);
        ButterKnife.bind(this, rootView);

        // DisplayMetrics used to fetch the size of the screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        // Used for the setting the size of imageView at runtime
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)
                imageView.getLayoutParams();
        params.height = (int) (height * BADGE_IMAGE_HEIGHT_RATIO);
        params.width = (int) (width * BADGE_IMAGE_WIDTH_RATIO);
        imageView.requestLayout();
        progressBar.setVisibility(View.VISIBLE);

        setHasOptionsMenu(true);

        // Set the initial value of WikiData edits to 0
        wikidataEditsText.setText("0");
        if(sessionManager.getUserName().equals(userName)){
            tvAchievementsOfUser.setVisibility(View.GONE);
        }else{
            tvAchievementsOfUser.setVisibility(View.VISIBLE);
            tvAchievementsOfUser.setText(getString(R.string.achievements_of_user,userName));
        }

        // Achievements currently unimplemented in Beta flavor. Skip all API calls.
        if(ConfigUtils.isBetaFlavour()) {
            progressBar.setVisibility(View.GONE);
            imageByWikiText.setText(R.string.no_image);
            imageRevertedText.setText(R.string.no_image_reverted);
            imageUploadedText.setText(R.string.no_image_uploaded);
            wikidataEditsText.setText("0");
            imagesFeatured.setText("0");
            tvQualityImages.setText("0");
            thanksReceived.setText("0");
            setMenuVisibility(true);
            return rootView;
        }
        setWikidataEditCount();
        setAchievements();
        return rootView;
    }

    @Override
    public void setMenuVisibility(boolean visible) {
        super.setMenuVisibility(visible);

        // Whenever this fragment is revealed in a menu,
        // notify Beta users the page data is unavailable
        if(ConfigUtils.isBetaFlavour() && visible) {
            Context ctx = null;
            if(getContext() != null) {
                ctx = getContext();
            } else if(getView() != null && getView().getContext() != null) {
                ctx = getView().getContext();
            }
            if(ctx != null) {
                Toast.makeText(ctx,
                    R.string.achievements_unavailable_beta,
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * To invoke the AlertDialog on clicking info button
     */
    @OnClick(R.id.achievement_info)
    public void showInfoDialog(){
        launchAlert(
            getResources().getString(R.string.Achievements),
            getResources().getString(R.string.achievements_info_message));
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
                        .getAchievements(Objects.requireNonNull(userName))
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
                                        // If the number of edits made by the user are more than 150,000
                                        // in some cases such high number of wiki edit counts cause the
                                        // achievements calculator to fail in some cases, for more details
                                        // refer Issue: #3295
                                        if (numberOfEdits <= 150000) {
                                            showSnackBarWithRetry(false);
                                        } else {
                                            showSnackBarWithRetry(true);
                                        }
                                    }
                                },
                                t -> {
                                    Timber.e(t, "Fetching achievements statistics failed");
                                    if (numberOfEdits <= 150000) {
                                        showSnackBarWithRetry(false);
                                    } else {
                                        showSnackBarWithRetry(true);
                                    }
                                }
                        ));
            }
            catch (Exception e){
                Timber.d(e+"success");
            }
        }
    }

    /**
     * To call the API to fetch the count of wiki data edits
     *  in the form of JavaRx Single object<JSONobject>
     */
    private void setWikidataEditCount() {
        if (StringUtils.isBlank(userName)) {
            return;
        }
        compositeDisposable.add(okHttpJsonApiClient
                .getWikidataEdits(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(edits -> {
                    numberOfEdits = edits;
                    wikidataEditsText.setText(String.valueOf(edits));
                }, e -> {
                    Timber.e("Error:" + e);
                }));
    }

    /**
     * Shows a snack bar which has an action button which on click dismisses the snackbar and invokes the
     * listener passed
     * @param tooManyAchievements if this value is true it means that the number of achievements of the
     * user are so high that it wrecks havoc with the Achievements calculator due to which request may time
     * out. Well this is the Ultimate Achievement
     */
    private void showSnackBarWithRetry(boolean tooManyAchievements) {
        if (tooManyAchievements) {
            progressBar.setVisibility(View.GONE);
            ViewUtil.showDismissibleSnackBar(getActivity().findViewById(android.R.id.content),
                    R.string.achievements_fetch_failed_ultimate_achievement, R.string.retry, view -> setAchievements());
        } else {
            progressBar.setVisibility(View.GONE);
            ViewUtil.showDismissibleSnackBar(getActivity().findViewById(android.R.id.content),
                    R.string.achievements_fetch_failed, R.string.retry, view -> setAchievements());
        }
    }

    /**
     * Shows a generic error toast when error occurs while loading achievements or uploads
     */
    private void onError() {
        ViewUtil.showLongToast(getActivity(), getResources().getString(R.string.error_occurred));
        progressBar.setVisibility(View.GONE);
    }

    /**
     * used to the count of images uploaded by user
     */
    private void setUploadCount(Achievements achievements) {
        if (checkAccount()) {
            compositeDisposable.add(okHttpJsonApiClient
                    .getUploadCount(Objects.requireNonNull(userName))
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
            imagesUploadedProgressbar.setVisibility(View.VISIBLE);
            imagesUploadedProgressbar.setProgress
                    (100*uploadCount/levelInfo.getMaxUploadCount());
            imagesUploadedProgressbar.setProgressTextFormatPattern
                    (uploadCount +"/" + levelInfo.getMaxUploadCount() );
        }

    }

    private void setZeroAchievements() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            .setMessage(
                !Objects.equals(sessionManager.getUserName(), userName) ?
                    getString(R.string.no_achievements_yet, userName) :
                    getString(R.string.you_have_no_achievements_yet)
            )
            .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
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
        imageRevertsProgressbar.setVisibility(View.VISIBLE);
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
        imagesUsedByWikiProgressBar.setVisibility(View.VISIBLE);
        thanksReceived.setText(String.valueOf(achievements.getThanksReceived()));
        imagesUsedByWikiProgressBar.setProgress
                (100 * achievements.getUniqueUsedImages() / levelInfo.getMaxUniqueImages());
        imagesUsedByWikiProgressBar.setProgressTextFormatPattern
                (achievements.getUniqueUsedImages() + "/" + levelInfo.getMaxUniqueImages());
        imagesFeatured.setText(String.valueOf(achievements.getFeaturedImages()));
        tvQualityImages.setText(String.valueOf(achievements.getQualityImages()));
        String levelUpInfoString = getString(R.string.level).toUpperCase();
        levelUpInfoString += " " + levelInfo.getLevelNumber();
        levelNumber.setText(levelUpInfoString);
        imageView.setImageDrawable(VectorDrawableCompat.create(getResources(), R.drawable.badge,
                new ContextThemeWrapper(getActivity(), levelInfo.getLevelStyle()).getTheme()));
        badgeText.setText(Integer.toString(levelInfo.getLevelNumber()));
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
        }
    }


    @OnClick(R.id.images_upload_info)
    public void showUploadInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.images_uploaded),
            getResources().getString(R.string.images_uploaded_explanation),
            IMAGES_UPLOADED_URL);
    }

    @OnClick(R.id.images_reverted_info)
    public void showRevertedInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.image_reverts),
            getResources().getString(R.string.images_reverted_explanation),
            IMAGES_REVERT_URL);
    }

    @OnClick(R.id.images_used_by_wiki_info)
    public void showUsedByWikiInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.images_used_by_wiki),
            getResources().getString(R.string.images_used_explanation),
            IMAGES_USED_URL);
    }

    @OnClick(R.id.images_nearby_info)
    public void showImagesViaNearbyInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.statistics_wikidata_edits),
            getResources().getString(R.string.images_via_nearby_explanation),
            IMAGES_NEARBY_PLACES_URL);
    }

    @OnClick(R.id.images_featured_info)
    public void showFeaturedImagesInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.statistics_featured),
            getResources().getString(R.string.images_featured_explanation),
            IMAGES_FEATURED_URL);
    }

    @OnClick(R.id.thanks_received_info)
    public void showThanksReceivedInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.statistics_thanks),
            getResources().getString(R.string.thanks_received_explanation),
            THANKS_URL);
    }

    @OnClick(R.id.quality_images_info)
    public void showQualityImagesInfo() {
        launchAlertWithHelpLink(
            getResources().getString(R.string.statistics_quality),
            getResources().getString(R.string.quality_images_info),
            QUALITY_IMAGE_URL);
    }

    /**
     * takes title and message as input to display alerts
     * @param title
     * @param message
     */
    private void launchAlert(String title, String message){
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                .create()
                .show();
    }

    /**
     *  Launch Alert with a READ MORE button and clicking it open a custom webpage
     */
    private void launchAlertWithHelpLink(String title, String message, String helpLinkUrl){
        new Builder(getActivity())
            .setTitle(title)
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
            .setNegativeButton(R.string.read_help_link, (dialog ,id) ->{
                Utils.handleWebUrl(requireContext(), Uri.parse(helpLinkUrl));;
            })
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
            ViewUtil.showLongToast(getActivity(), getResources().getString(R.string.user_not_logged_in));
            sessionManager.forceLogin(getActivity());
            return false;
        }
        return true;
    }
}
