package fr.free.nrw.commons.profile.achievements;

import android.accounts.Account;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import com.dinuscxj.progressbar.CircleProgressBar;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.databinding.FragmentAchievementsBinding;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.DialogUtil;
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

    @Inject
    SessionManager sessionManager;

    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;

    private FragmentAchievementsBinding binding;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    // To keep track of the number of wiki edits made by a user
    private int numberOfEdits = 0;

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
        binding = FragmentAchievementsBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        binding.achievementInfo.setOnClickListener(view -> showInfoDialog());
        binding.imagesUploadInfo.setOnClickListener(view -> showUploadInfo());
        binding.imagesRevertedInfo.setOnClickListener(view -> showRevertedInfo());
        binding.imagesUsedByWikiInfo.setOnClickListener(view -> showUsedByWikiInfo());
        binding.imagesNearbyInfo.setOnClickListener(view -> showImagesViaNearbyInfo());
        binding.imagesFeaturedInfo.setOnClickListener(view -> showFeaturedImagesInfo());
        binding.thanksReceivedInfo.setOnClickListener(view -> showThanksReceivedInfo());
        binding.qualityImagesInfo.setOnClickListener(view -> showQualityImagesInfo());

        // DisplayMetrics used to fetch the size of the screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        // Used for the setting the size of imageView at runtime
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)
                binding.achievementBadgeImage.getLayoutParams();
        params.height = (int) (height * BADGE_IMAGE_HEIGHT_RATIO);
        params.width = (int) (width * BADGE_IMAGE_WIDTH_RATIO);
        binding.achievementBadgeImage.requestLayout();
        binding.progressBar.setVisibility(View.VISIBLE);

        setHasOptionsMenu(true);

        // Set the initial value of WikiData edits to 0
        binding.wikidataEdits.setText("0");
        if(sessionManager.getUserName() == null || sessionManager.getUserName().equals(userName)){
            binding.tvAchievementsOfUser.setVisibility(View.GONE);
        }else{
            binding.tvAchievementsOfUser.setVisibility(View.VISIBLE);
            binding.tvAchievementsOfUser.setText(getString(R.string.achievements_of_user,userName));
        }

        // Achievements currently unimplemented in Beta flavor. Skip all API calls.
        if(ConfigUtils.isBetaFlavour()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.imagesUsedByWikiText.setText(R.string.no_image);
            binding.imagesRevertedText.setText(R.string.no_image_reverted);
            binding.imagesUploadTextParam.setText(R.string.no_image_uploaded);
            binding.wikidataEdits.setText("0");
            binding.imageFeatured.setText("0");
            binding.qualityImages.setText("0");
            binding.achievementLevel.setText("0");
            setMenuVisibility(true);
            return rootView;
        }
        setWikidataEditCount();
        setAchievements();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
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
    protected void showInfoDialog(){
        launchAlert(
            getResources().getString(R.string.Achievements),
            getResources().getString(R.string.achievements_info_message));
    }

    /**
     * To call the API to get results in form Single<JSONObject>
     * which then calls parseJson when results are fetched
     */
    private void setAchievements() {
        binding.progressBar.setVisibility(View.VISIBLE);
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
                                        binding.layoutImageReverts.setVisibility(View.INVISIBLE);
                                        binding.achievementBadgeImage.setVisibility(View.INVISIBLE);
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
                    binding.wikidataEdits.setText(String.valueOf(edits));
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
            binding.progressBar.setVisibility(View.GONE);
            ViewUtil.showDismissibleSnackBar(getActivity().findViewById(android.R.id.content),
                    R.string.achievements_fetch_failed_ultimate_achievement, R.string.retry, view -> setAchievements());
        } else {
            binding.progressBar.setVisibility(View.GONE);
            ViewUtil.showDismissibleSnackBar(getActivity().findViewById(android.R.id.content),
                    R.string.achievements_fetch_failed, R.string.retry, view -> setAchievements());
        }
    }

    /**
     * Shows a generic error toast when error occurs while loading achievements or uploads
     */
    private void onError() {
        ViewUtil.showLongToast(getActivity(), getResources().getString(R.string.error_occurred));
        binding.progressBar.setVisibility(View.GONE);
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
            binding.imagesUploadedProgressbar.setVisibility(View.VISIBLE);
            binding.imagesUploadedProgressbar.setProgress
                    (100*uploadCount/levelInfo.getMaxUploadCount());
            binding.tvUploadedImages.setText
                (uploadCount + "/" + levelInfo.getMaxUploadCount());
        }

    }

    private void setZeroAchievements() {
        String message = !Objects.equals(sessionManager.getUserName(), userName) ?
                getString(R.string.no_achievements_yet, userName) :
                getString(R.string.you_have_no_achievements_yet);
        DialogUtil.showAlertDialog(getActivity(),
            null,
            message,
            getString(R.string.ok),
            () -> {},
            true);
        binding.imagesUploadedProgressbar.setVisibility(View.INVISIBLE);
        binding.imageRevertsProgressbar.setVisibility(View.INVISIBLE);
        binding.imagesUsedByWikiProgressBar.setVisibility(View.INVISIBLE);
        binding.achievementBadgeImage.setVisibility(View.INVISIBLE);
        binding.imagesUsedByWikiText.setText(R.string.no_image);
        binding.imagesRevertedText.setText(R.string.no_image_reverted);
        binding.imagesUploadTextParam.setText(R.string.no_image_uploaded);
        binding.achievementBadgeImage.setVisibility(View.INVISIBLE);
    }

    /**
     * used to set the non revert image percentage
     * @param notRevertPercentage
     */
    private void setImageRevertPercentage(int notRevertPercentage){
        binding.imageRevertsProgressbar.setVisibility(View.VISIBLE);
        binding.imageRevertsProgressbar.setProgress(notRevertPercentage);
        String revertPercentage = Integer.toString(notRevertPercentage);
        binding.imageRevertsProgressbar.setProgressTextFormatPattern(revertPercentage + "%%");
        binding.imagesRevertLimitText.setText(getResources().getString(R.string.achievements_revert_limit_message)+ levelInfo.getMinNonRevertPercentage() + "%");
    }

    /**
     * Used the inflate the fetched statistics of the images uploaded by user
     * and assign badge and level
     * @param achievements
     */
    private void inflateAchievements(Achievements achievements) {
        binding.imagesUsedByWikiProgressBar.setVisibility(View.VISIBLE);
        binding.achievementLevel.setText(String.valueOf(achievements.getThanksReceived()));
        binding.imagesUsedByWikiProgressBar.setProgress
                (100 * achievements.getUniqueUsedImages() / levelInfo.getMaxUniqueImages());
        if(binding.tvWikiPb != null) {
            binding.tvWikiPb.setText
                (achievements.getUniqueUsedImages() + "/" + levelInfo.getMaxUniqueImages());
        }
        binding.imageFeatured.setText(String.valueOf(achievements.getFeaturedImages()));
        binding.qualityImages.setText(String.valueOf(achievements.getQualityImages()));
        String levelUpInfoString = getString(R.string.level).toUpperCase();
        levelUpInfoString += " " + levelInfo.getLevelNumber();
        binding.achievementLevel.setText(levelUpInfoString);
        binding.achievementBadgeImage.setImageDrawable(VectorDrawableCompat.create(getResources(), R.drawable.badge,
                new ContextThemeWrapper(getActivity(), levelInfo.getLevelStyle()).getTheme()));
        binding.achievementBadgeText.setText(Integer.toString(levelInfo.getLevelNumber()));
    }

    /**
     * to hide progressbar
     */
    private void hideProgressBar(Achievements achievements) {
        if (binding.progressBar != null) {
            levelInfo = LevelController.LevelInfo.from(achievements.getImagesUploaded(),
                    achievements.getUniqueUsedImages(),
                    achievements.getNotRevertPercentage());
            inflateAchievements(achievements);
            setUploadProgress(achievements.getImagesUploaded());
            setImageRevertPercentage(achievements.getNotRevertPercentage());
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    protected void showUploadInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.images_uploaded),
            getResources().getString(R.string.images_uploaded_explanation),
            IMAGES_UPLOADED_URL);
    }

    protected void showRevertedInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.image_reverts),
            getResources().getString(R.string.images_reverted_explanation),
            IMAGES_REVERT_URL);
    }

    protected void showUsedByWikiInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.images_used_by_wiki),
            getResources().getString(R.string.images_used_explanation),
            IMAGES_USED_URL);
    }

    protected void showImagesViaNearbyInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.statistics_wikidata_edits),
            getResources().getString(R.string.images_via_nearby_explanation),
            IMAGES_NEARBY_PLACES_URL);
    }

    protected void showFeaturedImagesInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.statistics_featured),
            getResources().getString(R.string.images_featured_explanation),
            IMAGES_FEATURED_URL);
    }

    protected void showThanksReceivedInfo(){
        launchAlertWithHelpLink(
            getResources().getString(R.string.statistics_thanks),
            getResources().getString(R.string.thanks_received_explanation),
            THANKS_URL);
    }

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
        DialogUtil.showAlertDialog(getActivity(),
            title,
            message,
            getString(R.string.ok),
            () -> {},
            true);
    }

    /**
     *  Launch Alert with a READ MORE button and clicking it open a custom webpage
     */
    private void launchAlertWithHelpLink(String title, String message, String helpLinkUrl) {
        DialogUtil.showAlertDialog(getActivity(),
            title,
            message,
            getString(R.string.ok),
            getString(R.string.read_help_link),
            () -> {},
            () -> Utils.handleWebUrl(requireContext(), Uri.parse(helpLinkUrl)),
            null,
            true);
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
