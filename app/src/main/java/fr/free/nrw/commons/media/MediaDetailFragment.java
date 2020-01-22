package fr.free.nrw.commons.media;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.category.CategoryDetailsActivity;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.delete.DeleteHelper;
import fr.free.nrw.commons.delete.ReasonBuilder;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.ui.widget.CompatTextView;
import fr.free.nrw.commons.ui.widget.HtmlTextView;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MediaDetailFragment extends CommonsDaggerSupportFragment {

    private boolean editable;
    private boolean isCategoryImage;
    private MediaDetailPagerFragment.MediaDetailProvider detailProvider;
    private int index;
    private Locale locale;
    private boolean isDeleted = false;


    public static MediaDetailFragment forMedia(int index, boolean editable, boolean isCategoryImage) {
        MediaDetailFragment mf = new MediaDetailFragment();

        Bundle state = new Bundle();
        state.putBoolean("editable", editable);
        state.putBoolean("isCategoryImage", isCategoryImage);
        state.putInt("index", index);
        state.putInt("listIndex", 0);
        state.putInt("listTop", 0);

        mf.setArguments(state);

        return mf;
    }

    @Inject
    MediaDataExtractor mediaDataExtractor;
    @Inject
    ReasonBuilder reasonBuilder;
    @Inject
    DeleteHelper deleteHelper;
    @Inject
    ViewUtilWrapper viewUtil;

    private int initialListTop = 0;

    @BindView(R.id.mediaDetailImage)
    SimpleDraweeView image;
    @BindView(R.id.mediaDetailSpacer)
    MediaDetailSpacer spacer;
    @BindView(R.id.mediaDetailTitle)
    TextView title;
    @BindView(R.id.mediaDetailDesc)
    HtmlTextView desc;
    @BindView(R.id.mediaDetailAuthor)
    TextView author;
    @BindView(R.id.mediaDetailLicense)
    TextView license;
    @BindView(R.id.mediaDetailCoordinates)
    TextView coordinates;
    @BindView(R.id.mediaDetailuploadeddate)
    TextView uploadedDate;
    @BindView(R.id.mediaDetailDisc)
    TextView mediaDiscussion;
    @BindView(R.id.seeMore)
    TextView seeMore;
    @BindView(R.id.nominatedDeletionBanner)
    LinearLayout nominatedForDeletion;
    @BindView(R.id.mediaDetailCategoryContainer)
    LinearLayout categoryContainer;
    @BindView(R.id.authorLinearLayout)
    LinearLayout authorLayout;
    @BindView(R.id.nominateDeletion)
    Button delete;
    @BindView(R.id.mediaDetailScrollView)
    ScrollView scrollView;

    private ArrayList<String> categoryNames;
    private boolean categoriesLoaded = false;
    private boolean categoriesPresent = false;
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener; // for layout stuff, only used once!
    private ViewTreeObserver.OnScrollChangedListener scrollListener;

    //Had to make this class variable, to implement various onClicks, which access the media, also I fell why make separate variables when one can serve the purpose
    private Media media;
    private ArrayList<String> reasonList;


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", index);
        outState.putBoolean("editable", editable);
        outState.putBoolean("isCategoryImage", isCategoryImage);

        getScrollPosition();
        outState.putInt("listTop", initialListTop);
    }

    private void getScrollPosition() {
        initialListTop = scrollView.getScrollY();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getParentFragment() != null
            && getParentFragment() instanceof MediaDetailPagerFragment) {
            detailProvider =
                ((MediaDetailPagerFragment) getParentFragment()).getMediaDetailProvider();
        }
        if (savedInstanceState != null) {
            editable = savedInstanceState.getBoolean("editable");
            isCategoryImage = savedInstanceState.getBoolean("isCategoryImage");
            index = savedInstanceState.getInt("index");
            initialListTop = savedInstanceState.getInt("listTop");
        } else {
            editable = getArguments().getBoolean("editable");
            isCategoryImage = getArguments().getBoolean("isCategoryImage");
            index = getArguments().getInt("index");
            initialListTop = 0;
        }

        reasonList = new ArrayList<>();
        reasonList.add(getString(R.string.deletion_reason_uploaded_by_mistake));
        reasonList.add(getString(R.string.deletion_reason_publicly_visible));
        reasonList.add(getString(R.string.deletion_reason_not_interesting));
        reasonList.add(getString(R.string.deletion_reason_no_longer_want_public));
        reasonList.add(getString(R.string.deletion_reason_bad_for_my_privacy));

        categoryNames = new ArrayList<>();
        categoryNames.add(getString(R.string.detail_panel_cats_loading));

        final View view = inflater.inflate(R.layout.fragment_media_detail, container, false);

        ButterKnife.bind(this,view);
        seeMore.setText(StringUtil.fromHtml(getString(R.string.nominated_see_more)));

        if (isCategoryImage){
            authorLayout.setVisibility(VISIBLE);
        } else {
            authorLayout.setVisibility(GONE);
        }

        // Progressively darken the image in the background when we scroll detail pane up
        scrollListener = this::updateTheDarkness;
        view.getViewTreeObserver().addOnScrollChangedListener(scrollListener);

        // Layout layoutListener to size the spacer item relative to the available space.
        // There may be a .... better way to do this.
        layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private int currentHeight = -1;

            @Override
            public void onGlobalLayout() {
                int viewHeight = view.getHeight();
                //int textHeight = title.getLineHeight();
                int paddingDp = 112;
                float paddingPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, paddingDp, getResources().getDisplayMetrics());
                int newHeight = viewHeight - Math.round(paddingPx);

                if (newHeight != currentHeight) {
                    currentHeight = newHeight;
                    ViewGroup.LayoutParams params = spacer.getLayoutParams();
                    params.height = newHeight;
                    spacer.setLayoutParams(params);

                    scrollView.scrollTo(0, initialListTop);
                }
            }
        };
        view.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        locale = getResources().getConfiguration().locale;

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getParentFragment() != null && getParentFragment().getParentFragment() != null) {
            //Added a check because, not necessarily, the parent fragment will have a parent fragment, say
            // in the case when MediaDetailPagerFragment is directly started by the CategoryImagesActivity
            ((ContributionsFragment) (getParentFragment()
                    .getParentFragment())).nearbyNotificationCardView
                    .setVisibility(View.GONE);
        }
        media = detailProvider.getMediaAtPosition(index);
        displayMediaDetails();
    }

    private void displayMediaDetails() {
        //Always load image from Internet to allow viewing the desc, license, and cats
        setupImageView();
        title.setText(media.getDisplayTitle());
        desc.setHtmlText(media.getDescription());
        license.setText(media.getLicense());

        Disposable disposable = mediaDataExtractor.fetchMediaDetails(media.getFilename())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setTextFields);
        compositeDisposable.add(disposable);
    }

    /**
     * Uses two image sources.
     * - low resolution thumbnail is shown initially
     * - when the high resolution image is available, it replaces the low resolution image
     */
    private void setupImageView() {
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setLowResImageRequest(ImageRequest.fromUri(media.getThumbUrl()))
                .setImageRequest(ImageRequest.fromUri(media.getImageUrl()))
                .setOldController(image.getController())
                .build();
        image.setController(controller);
    }

    @Override
    public void onDestroyView() {
        if (layoutListener != null && getView() != null) {
            getView().getViewTreeObserver().removeGlobalOnLayoutListener(layoutListener); // old Android was on crack. CRACK IS WHACK
            layoutListener = null;
        }
        if (scrollListener != null && getView() != null) {
            getView().getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
            scrollListener = null;
        }

        compositeDisposable.clear();
        super.onDestroyView();
    }

    private void setTextFields(Media media) {
        this.media = media;
        setupImageView();
        desc.setHtmlText(prettyDescription(media));
        license.setText(prettyLicense(media));
        coordinates.setText(prettyCoordinates(media));
        uploadedDate.setText(prettyUploadedDate(media));
        mediaDiscussion.setText(prettyDiscussion(media));

        categoryNames.clear();
        categoryNames.addAll(media.getCategories());

        categoriesLoaded = true;
        categoriesPresent = (categoryNames.size() > 0);
        if (!categoriesPresent) {
            // Stick in a filler element.
            categoryNames.add(getString(R.string.detail_panel_cats_none));
        }
        rebuildCatList();

        if (media.getCreator() == null || media.getCreator().equals("")) {
            authorLayout.setVisibility(GONE);
        } else {
            author.setText(media.getCreator());
        }

        checkDeletion(media);
    }

    @OnClick(R.id.mediaDetailLicense)
    public void onMediaDetailLicenceClicked(){
        String url = media.getLicenseUrl();
        if (!StringUtils.isBlank(url) && getActivity() != null) {
            Utils.handleWebUrl(getActivity(), Uri.parse(url));
        } else {
            viewUtil.showShortToast(getActivity(), getString(R.string.null_url));
        }
    }

    @OnClick(R.id.mediaDetailCoordinates)
    public void onMediaDetailCoordinatesClicked(){
        if (media.getCoordinates() != null && getActivity() != null) {
            Utils.handleGeoCoordinates(getActivity(), media.getCoordinates());
        }
    }

    @OnClick(R.id.copyWikicode)
    public void onCopyWikicodeClicked(){
        String data = "[[" + media.getFilename() + "|thumb|" + media.getDescription() + "]]";
        Utils.copy("wikiCode",data,getContext());
        Timber.d("Generated wikidata copy code: %s", data);

        Toast.makeText(getContext(), getString(R.string.wikicode_copied), Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.nominateDeletion)
    public void onDeleteButtonClicked(){
        if(AccountUtil.getUserName(getContext()).equals(media.getCreator())){
        final ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.simple_spinner_dropdown_list, reasonList);
        final Spinner spinner = new Spinner(getActivity());
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        spinner.setAdapter(languageAdapter);
        spinner.setGravity(17);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(spinner);
        builder.setTitle(R.string.nominate_delete)
                .setPositiveButton(R.string.about_translate_proceed, (dialog, which) -> onDeleteClicked(spinner));
        builder.setNegativeButton(R.string.about_translate_cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
        if(isDeleted) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
        }
        //Reviewer correct me if i have misunderstood something over here
        //But how does this  if (delete.getVisibility() == View.VISIBLE) {
        //            enableDeleteButton(true);   makes sense ?
        else{
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(getString(R.string.dialog_box_text_nomination,media.getDisplayTitle()));
        final EditText input = new EditText(getActivity());
        alert.setView(input);
        input.requestFocus();
        alert.setPositiveButton(R.string.ok, (dialog1, whichButton) -> {
            String reason = input.getText().toString();
            onDeleteClickeddialogtext(reason);
        });
        alert.setNegativeButton(R.string.cancel, (dialog12, whichButton) -> {
        });
        AlertDialog d = alert.create();
        input.addTextChangedListener(new TextWatcher() {
            private void handleText() {
                final Button okButton = d.getButton(AlertDialog.BUTTON_POSITIVE);
                if (input.getText().length() == 0) {
                    okButton.setEnabled(false);
                } else {
                    okButton.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                handleText();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }
        }

    @SuppressLint("CheckResult")
    private void onDeleteClicked(Spinner spinner) {
        String reason = spinner.getSelectedItem().toString();
        Single<Boolean> resultSingle = reasonBuilder.getReason(media, reason)
                .flatMap(reasonString -> deleteHelper.makeDeletion(getContext(), media, reason));
        compositeDisposable.add(resultSingle
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    if (getActivity() != null) {
                        isDeleted = true;
                        enableDeleteButton(false);
                    }
                }));

    }

    @SuppressLint("CheckResult")
    private void onDeleteClickeddialogtext(String reason) {
        Single<Boolean> resultSingletext = reasonBuilder.getReason(media, reason)
                .flatMap(reasonString -> deleteHelper.makeDeletion(getContext(), media, reason));
        compositeDisposable.add(resultSingletext
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    if (getActivity() != null) {
                        isDeleted = true;
                        enableDeleteButton(false);
                    }
                }));

    }

    @OnClick(R.id.seeMore)
    public void onSeeMoreClicked(){
        if (nominatedForDeletion.getVisibility() == VISIBLE && getActivity() != null) {
            Utils.handleWebUrl(getActivity(), Uri.parse(media.getPageTitle().getMobileUri()));
        }
    }

    private void enableDeleteButton(boolean visibility) {
        delete.setEnabled(visibility);
        if (visibility) {
            delete.setTextColor(getResources().getColor(R.color.primaryTextColor));
        } else {
            delete.setTextColor(getResources().getColor(R.color.deleteButtonLight));
        }
    }

    private void rebuildCatList() {
        categoryContainer.removeAllViews();
        // @fixme add the category items

        //As per issue #1826(see https://github.com/commons-app/apps-android-commons/issues/1826), some categories come suffixed with strings prefixed with |. As per the discussion
        //that was meant for alphabetical sorting of the categories and can be safely removed.
        for (int i = 0; i < categoryNames.size(); i++) {
            String categoryName = categoryNames.get(i);
            //Removed everything after '|'
            int indexOfPipe = categoryName.indexOf('|');
            if (indexOfPipe != -1) {
                categoryName = categoryName.substring(0, indexOfPipe);
                //Set the updated category to the list as well
                categoryNames.set(i, categoryName);
            }
            View catLabel = buildCatLabel(categoryName, categoryContainer);
            categoryContainer.addView(catLabel);
        }
    }

    private View buildCatLabel(final String catName, ViewGroup categoryContainer) {
        final View item = LayoutInflater.from(getContext()).inflate(R.layout.detail_category_item, categoryContainer, false);
        final CompatTextView textView = item.findViewById(R.id.mediaDetailCategoryItemText);

        textView.setText(catName);
        if (categoriesLoaded && categoriesPresent) {
            textView.setOnClickListener(view -> {
                // Open Category Details page
                String selectedCategoryTitle = "Category:" + catName;
                Intent intent = new Intent(getContext(), CategoryDetailsActivity.class);
                intent.putExtra("categoryName", selectedCategoryTitle);
                getContext().startActivity(intent);
            });
        }
        return item;
    }

    private void updateTheDarkness() {
        // You must face the darkness alone
        int scrollY = scrollView.getScrollY();
        int scrollMax = getView().getHeight();
        float scrollPercentage = (float) scrollY / (float) scrollMax;
        final float transparencyMax = 0.75f;
        if (scrollPercentage > transparencyMax) {
            scrollPercentage = transparencyMax;
        }
        image.setAlpha(1.0f - scrollPercentage);
    }

    private String prettyDescription(Media media) {
        // @todo use UI language when multilingual descs are available
        String desc = media.getDescription(locale.getLanguage()).trim();
        if (desc.equals("")) {
            return getString(R.string.detail_description_empty);
        } else {
            return desc;
        }
    }
    private String prettyDiscussion(Media media) {
        String disc = media.getDiscussion().trim();
        if (disc.equals("")) {
            return getString(R.string.detail_discussion_empty);
        } else {
            return disc;
        }
    }

    private String prettyLicense(Media media) {
        String licenseKey = media.getLicense();
        Timber.d("Media license is: %s", licenseKey);
        if (licenseKey == null || licenseKey.equals("")) {
            return getString(R.string.detail_license_empty);
        }
        return licenseKey;
    }

    private String prettyUploadedDate(Media media) {
        Date date = media.getDateUploaded();
        if (date == null || date.toString() == null || date.toString().isEmpty()) {
            return "Uploaded date not available";
        }
        return DateUtil.getDateStringWithSkeletonPattern(date, "dd MMM yyyy");
    }

    /**
     * Returns the coordinates nicely formatted.
     *
     * @return Coordinates as text.
     */
    private String prettyCoordinates(Media media) {
        if (media.getCoordinates() == null) {
            return getString(R.string.media_detail_coordinates_empty);
        }
        return media.getCoordinates().getPrettyCoordinateString();
    }

    private void checkDeletion(Media media){
        if (media.getRequestedDeletion()){
            delete.setVisibility(GONE);
            nominatedForDeletion.setVisibility(VISIBLE);
        } else if (!isCategoryImage) {
            delete.setVisibility(VISIBLE);
            nominatedForDeletion.setVisibility(GONE);
        }
    }

}
