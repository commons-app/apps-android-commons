package fr.free.nrw.commons.media;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.License;
import fr.free.nrw.commons.LicenseList;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.MediaWikiImageView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.category.CategoryDetailsActivity;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.delete.DeleteTask;
import fr.free.nrw.commons.delete.ReasonBuilder;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.ui.widget.CompatTextView;
import timber.log.Timber;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;

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
    Provider<MediaDataExtractor> mediaDataExtractorProvider;
    @Inject
    MediaWikiApi mwApi;
    @Inject
    SessionManager sessionManager;

    private int initialListTop = 0;

    @BindView(R.id.mediaDetailImage)
    MediaWikiImageView image;
    @BindView(R.id.mediaDetailSpacer)
    MediaDetailSpacer spacer;
    @BindView(R.id.mediaDetailTitle)
    TextView title;
    @BindView(R.id.mediaDetailDesc)
    TextView desc;
    @BindView(R.id.mediaDetailAuthor)
    TextView author;
    @BindView(R.id.mediaDetailLicense)
    TextView license;
    @BindView(R.id.mediaDetailCoordinates)
    TextView coordinates;
    @BindView(R.id.mediaDetailuploadeddate)
    TextView uploadedDate;
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
    private DataSetObserver dataObserver;
    private AsyncTask<Void, Void, Boolean> detailFetchTask;
    private LicenseList licenseList;

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
        seeMore.setText(Html.fromHtml(getString(R.string.nominated_see_more)));

        if (isCategoryImage){
            authorLayout.setVisibility(VISIBLE);
        } else {
            authorLayout.setVisibility(GONE);
        }

        licenseList = new LicenseList(getActivity());

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
        if(getParentFragment()!=null && getParentFragment().getParentFragment()!=null) {
            //Added a check because, not necessarily, the parent fragment will have a parent fragment, say
            // in the case when MediaDetailPagerFragment is directly started by the CategoryImagesActivity
            ((ContributionsFragment) (getParentFragment().getParentFragment())).nearbyNoificationCardView
                .setVisibility(View.GONE);
        }
        media = detailProvider.getMediaAtPosition(index);
        if (media == null) {
            // Ask the detail provider to ping us when we're ready
            Timber.d("MediaDetailFragment not yet ready to display details; registering observer");
            dataObserver = new DataSetObserver() {
                @Override
                public void onChanged() {
                    if (!isAdded()) {
                        return;
                    }
                    Timber.d("MediaDetailFragment ready to display delayed details!");
                    detailProvider.unregisterDataSetObserver(dataObserver);
                    dataObserver = null;
                    media=detailProvider.getMediaAtPosition(index);
                    displayMediaDetails();
                }
            };
            detailProvider.registerDataSetObserver(dataObserver);
        } else {
            Timber.d("MediaDetailFragment ready to display details");
            displayMediaDetails();
        }
    }

    private void displayMediaDetails() {
        //Always load image from Internet to allow viewing the desc, license, and cats
        image.setMedia(media);

        // FIXME: For transparent images
        // FIXME: keep the spinner going while we load data
        // FIXME: cache this data
        // Load image metadata: desc, license, categories
        detailFetchTask = new AsyncTask<Void, Void, Boolean>() {
            private MediaDataExtractor extractor;

            @Override
            protected void onPreExecute() {
                extractor = mediaDataExtractorProvider.get();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                // Local files have no filename yet
                if (media.getFilename() == null) {
                    return Boolean.FALSE;
                }
                try {
                    extractor.fetch(media.getFilename(), licenseList);
                    return Boolean.TRUE;
                } catch (IOException e) {
                    Timber.d(e);
                }
                return Boolean.FALSE;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                detailFetchTask = null;
                if (!isAdded()) {
                    return;
                }

                if (success) {
                    extractor.fill(media);
                    setTextFields(media);
                } else {
                    Timber.d("Failed to load photo details.");
                }
            }
        };
        detailFetchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        title.setText(media.getDisplayTitle());
        desc.setText(""); // fill in from network...
        license.setText(""); // fill in from network...
    }

    @Override
    public void onDestroyView() {
        if (detailFetchTask != null) {
            detailFetchTask.cancel(true);
            detailFetchTask = null;
        }
        if (layoutListener != null && getView() != null) {
            getView().getViewTreeObserver().removeGlobalOnLayoutListener(layoutListener); // old Android was on crack. CRACK IS WHACK
            layoutListener = null;
        }
        if (scrollListener != null && getView() != null) {
            getView().getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
            scrollListener = null;
        }
        if (dataObserver != null) {
            detailProvider.unregisterDataSetObserver(dataObserver);
            dataObserver = null;
        }
        super.onDestroyView();
    }

    private void setTextFields(Media media) {
        desc.setText(prettyDescription(media));
        license.setText(prettyLicense(media));
        coordinates.setText(prettyCoordinates(media));
        uploadedDate.setText(prettyUploadedDate(media));

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
        if (!TextUtils.isEmpty(licenseLink(media))) {
            openWebBrowser(licenseLink(media));
        } else {
            if (isCategoryImage) {
                Timber.d("Unable to fetch license URL for %s", media.getLicense());
            } else {
                Toast toast = Toast.makeText(getContext(), getString(R.string.null_url), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    @OnClick(R.id.mediaDetailCoordinates)
    public void onMediaDetailCoordinatesClicked(){
        if (media.getCoordinates() != null) {
            openMap(media.getCoordinates());
        }
    }

    @OnClick(R.id.copyWikicode)
    public void onCopyWikicodeClicked(){
        String data = "[[" + media.getFilename() + "|thumb|" + media.getDescription() + "]]";
        ClipboardManager clipboard = (ClipboardManager) getContext().getApplicationContext().getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("wikiCode", data));

        Timber.d("Generated wikidata copy code: %s", data);

        Toast.makeText(getContext(), getString(R.string.wikicode_copied), Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.nominateDeletion)
    public void onDeleteButtonClicked(){
        final ArrayAdapter<String> languageAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.simple_spinner_dropdown_list, reasonList);
        final Spinner spinner = new Spinner(getActivity());
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        spinner.setAdapter(languageAdapter);
        spinner.setGravity(17);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(spinner);
        builder.setTitle(R.string.nominate_delete)
                .setPositiveButton(R.string.about_translate_proceed, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String reason = spinner.getSelectedItem().toString();
                        ReasonBuilder reasonBuilder = new ReasonBuilder(reason,
                                getActivity(),
                                media,
                                sessionManager,
                                mwApi);
                        reason = reasonBuilder.getReason();
                        DeleteTask deleteTask = new DeleteTask(getActivity(), media, reason);
                        deleteTask.execute();
                        isDeleted = true;
                        enableDeleteButton(false);
                    }
                });
        builder.setNegativeButton(R.string.about_translate_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        if(isDeleted) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    @OnClick(R.id.seeMore)
    public void onSeeMoreClicked(){
        if (nominatedForDeletion.getVisibility()== VISIBLE) {
            openWebBrowser(media.getFilePageTitle().getMobileUri().toString());
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

    private String prettyLicense(Media media) {
        String licenseKey = media.getLicense();
        Timber.d("Media license is: %s", licenseKey);
        if (licenseKey == null || licenseKey.equals("")) {
            return getString(R.string.detail_license_empty);
        }
        License licenseObj = licenseList.get(licenseKey);
        if (licenseObj == null) {
            return licenseKey;
        } else {
            return licenseObj.getName();
        }
    }

    private String prettyUploadedDate(Media media) {
        Date date = media.getDateUploaded();
        if (date == null || date.toString() == null || date.toString().isEmpty()) {
            return "Uploaded date not available";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return formatter.format(date);
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

    private @Nullable
    String licenseLink(Media media) {
        String licenseKey = media.getLicense();
        if (licenseKey == null || licenseKey.equals("")) {
            return null;
        }
        License licenseObj = licenseList.get(licenseKey);
        if (licenseObj == null) {
            return null;
        } else {
            return licenseObj.getUrl(Locale.getDefault().getLanguage());
        }
    }

    private void openWebBrowser(String url) {
        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        //check if web browser available
        if (browser.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(browser);
        } else {
            Toast toast = Toast.makeText(getContext(), getString(R.string.no_web_browser), LENGTH_SHORT);
            toast.show();
        }

    }

    private void openMap(LatLng coordinates) {
        //Open map app at given position
        Uri gmmIntentUri = Uri.parse(
                "geo:0,0?q=" + coordinates.getLatitude() + "," + coordinates.getLongitude());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

        if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }
}
