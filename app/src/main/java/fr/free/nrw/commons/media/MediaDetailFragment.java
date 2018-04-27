package fr.free.nrw.commons.media;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Provider;

import fr.free.nrw.commons.License;
import fr.free.nrw.commons.LicenseList;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.MediaWikiImageView;
import fr.free.nrw.commons.PageTitle;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.delete.DeleteTask;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.ui.widget.CompatTextView;
import timber.log.Timber;

import static android.widget.Toast.LENGTH_SHORT;

public class MediaDetailFragment extends CommonsDaggerSupportFragment {

    private boolean editable;
    private boolean isFeaturedMedia;
    private MediaDetailPagerFragment.MediaDetailProvider detailProvider;
    private int index;

    public static MediaDetailFragment forMedia(int index, boolean editable, boolean isFeaturedMedia) {
        MediaDetailFragment mf = new MediaDetailFragment();

        Bundle state = new Bundle();
        state.putBoolean("editable", editable);
        state.putBoolean("isFeaturedMedia", isFeaturedMedia);
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


    private MediaWikiImageView image;
    private MediaDetailSpacer spacer;
    private int initialListTop = 0;

    private TextView title;
    private TextView desc;
    private TextView author;
    private TextView license;
    private TextView coordinates;
    private TextView uploadedDate;
    private TextView seeMore;
    private LinearLayout nominatedforDeletion;
    private LinearLayout categoryContainer;
    private LinearLayout authorLayout;
    private Button delete;
    private ScrollView scrollView;
    private ArrayList<String> categoryNames;
    private boolean categoriesLoaded = false;
    private boolean categoriesPresent = false;
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener; // for layout stuff, only used once!
    private ViewTreeObserver.OnScrollChangedListener scrollListener;
    private DataSetObserver dataObserver;
    private AsyncTask<Void, Void, Boolean> detailFetchTask;
    private LicenseList licenseList;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", index);
        outState.putBoolean("editable", editable);
        outState.putBoolean("isFeaturedMedia", isFeaturedMedia);

        getScrollPosition();
        outState.putInt("listTop", initialListTop);
    }

    private void getScrollPosition() {
        initialListTop = scrollView.getScrollY();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        detailProvider = (MediaDetailPagerFragment.MediaDetailProvider) getActivity();

        if (savedInstanceState != null) {
            editable = savedInstanceState.getBoolean("editable");
            isFeaturedMedia = savedInstanceState.getBoolean("isFeaturedMedia");
            index = savedInstanceState.getInt("index");
            initialListTop = savedInstanceState.getInt("listTop");
        } else {
            editable = getArguments().getBoolean("editable");
            isFeaturedMedia = getArguments().getBoolean("isFeaturedMedia");
            index = getArguments().getInt("index");
            initialListTop = 0;
        }

        categoryNames = new ArrayList<>();
        categoryNames.add(getString(R.string.detail_panel_cats_loading));

        final View view = inflater.inflate(R.layout.fragment_media_detail, container, false);

        image = (MediaWikiImageView) view.findViewById(R.id.mediaDetailImage);
        scrollView = (ScrollView) view.findViewById(R.id.mediaDetailScrollView);

        // Detail consists of a list view with main pane in header view, plus category list.
        spacer = (MediaDetailSpacer) view.findViewById(R.id.mediaDetailSpacer);
        title = (TextView) view.findViewById(R.id.mediaDetailTitle);
        desc = (TextView) view.findViewById(R.id.mediaDetailDesc);
        author = (TextView) view.findViewById(R.id.mediaDetailAuthor);
        license = (TextView) view.findViewById(R.id.mediaDetailLicense);
        coordinates = (TextView) view.findViewById(R.id.mediaDetailCoordinates);
        uploadedDate = (TextView) view.findViewById(R.id.mediaDetailuploadeddate);
        seeMore = (TextView) view.findViewById(R.id.seeMore);
        nominatedforDeletion = (LinearLayout) view.findViewById(R.id.nominatedDeletionBanner);
        delete = (Button) view.findViewById(R.id.nominateDeletion);
        categoryContainer = (LinearLayout) view.findViewById(R.id.mediaDetailCategoryContainer);
        authorLayout = (LinearLayout) view.findViewById(R.id.authorLinearLayout);

        if (isFeaturedMedia){
            authorLayout.setVisibility(View.VISIBLE);
        } else {
            authorLayout.setVisibility(View.GONE);
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
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Media media = detailProvider.getMediaAtPosition(index);
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
                    displayMediaDetails(detailProvider.getMediaAtPosition(index));
                }
            };
            detailProvider.registerDataSetObserver(dataObserver);
        } else {
            Timber.d("MediaDetailFragment ready to display details");
            displayMediaDetails(media);
        }
    }

    private void displayMediaDetails(final Media media) {
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
                    setOnClickListeners(media);
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

        checkDeletion(media);
    }

    private void setOnClickListeners(final Media media) {
        if (licenseLink(media) != null) {
            license.setOnClickListener(v -> openWebBrowser(licenseLink(media)));
        } else {
            Toast toast = Toast.makeText(getContext(), getString(R.string.null_url), Toast.LENGTH_SHORT);
            toast.show();
        }
        if (media.getCoordinates() != null) {
            coordinates.setOnClickListener(v -> openMap(media.getCoordinates()));
        }
        if (delete.getVisibility() == View.VISIBLE) {
            enableDeleteButton(true);

            delete.setOnClickListener(v -> {

                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setMessage("Why should this file be deleted?");
                final EditText input = new EditText(getActivity());
                alert.setView(input);
                input.requestFocus();
                alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String reason = input.getText().toString();
                        DeleteTask deleteTask = new DeleteTask(getActivity(), media, reason);
                        deleteTask.execute();
                        enableDeleteButton(false);
                    }
                });
                alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
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
            });
        }
        if (nominatedforDeletion.getVisibility() == View.VISIBLE){
            seeMore.setOnClickListener(v -> {
                openWebBrowser(media.getFilePageTitle().getMobileUri().toString());
            });
        }
    }

    private void enableDeleteButton(boolean visibility) {
        delete.setEnabled(visibility);
        if(visibility) {
            delete.setTextColor(getResources().getColor(R.color.primaryTextColor));
        } else {
            delete.setTextColor(getResources().getColor(R.color.deleteButtonLight));
        }
    }

    private void rebuildCatList() {
        categoryContainer.removeAllViews();
        // @fixme add the category items
        for (String cat : categoryNames) {
            View catLabel = buildCatLabel(cat, categoryContainer);
            categoryContainer.addView(catLabel);
        }
    }

    private View buildCatLabel(final String catName, ViewGroup categoryContainer) {
        final View item = LayoutInflater.from(getContext()).inflate(R.layout.detail_category_item, categoryContainer, false);
        final CompatTextView textView = (CompatTextView) item.findViewById(R.id.mediaDetailCategoryItemText);

        textView.setText(catName);
        if (categoriesLoaded && categoriesPresent) {
            textView.setOnClickListener(view -> {
                String selectedCategoryTitle = "Category:" + catName;
                Intent viewIntent = new Intent();
                viewIntent.setAction(Intent.ACTION_VIEW);
                viewIntent.setData(new PageTitle(selectedCategoryTitle).getCanonicalUri());
                //check if web browser available
                if (viewIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(viewIntent);
                } else {
                    Toast toast = Toast.makeText(getContext(), getString(R.string.no_web_browser), LENGTH_SHORT);
                    toast.show();
                }
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
        String desc = media.getDescription("en").trim();
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
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy");
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
            delete.setVisibility(View.GONE);
            nominatedforDeletion.setVisibility(View.VISIBLE);
        }
        else{
            delete.setVisibility(View.VISIBLE);
            nominatedforDeletion.setVisibility(View.GONE);
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
