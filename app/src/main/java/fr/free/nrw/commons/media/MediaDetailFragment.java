package fr.free.nrw.commons.media;

import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

import java.io.IOException;
import java.util.ArrayList;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.License;
import fr.free.nrw.commons.LicenseList;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.MediaWikiImageView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;

public class MediaDetailFragment extends Fragment {

    private boolean editable;
    private DisplayImageOptions displayOptions;
    private fr.free.nrw.commons.media.MediaDetailPagerFragment.MediaDetailProvider detailProvider;
    private int index;

    public static MediaDetailFragment forMedia(int index) {
        return forMedia(index, false);
    }

    public static MediaDetailFragment forMedia(int index, boolean editable) {
        MediaDetailFragment mf = new MediaDetailFragment();

        Bundle state = new Bundle();
        state.putBoolean("editable", editable);
        state.putInt("index", index);
        state.putInt("listIndex", 0);
        state.putInt("listTop", 0);

        mf.setArguments(state);

        return mf;
    }

    private ImageView image;
    //private EditText title;
    private ProgressBar loadingProgress;
    private ImageView loadingFailed;
    private fr.free.nrw.commons.media.MediaDetailSpacer spacer;
    private int initialListTop = 0;

    private TextView title;
    private TextView desc;
    private TextView license;
    private LinearLayout categoryContainer;
    private ScrollView scrollView;
    private ArrayList<String> categoryNames;
    private boolean categoriesLoaded = false;
    private boolean categoriesPresent = false;
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener; // for layout stuff, only used once!
    private ViewTreeObserver.OnScrollChangedListener scrollListener;
    DataSetObserver dataObserver;
    private AsyncTask<Void,Void,Boolean> detailFetchTask;
    private LicenseList licenseList;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", index);
        outState.putBoolean("editable", editable);

        getScrollPosition();
        outState.putInt("listTop", initialListTop);
    }

    private void getScrollPosition() {
        initialListTop = scrollView.getScrollY();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        detailProvider = (fr.free.nrw.commons.media.MediaDetailPagerFragment.MediaDetailProvider)getActivity();

        if(savedInstanceState != null) {
            editable = savedInstanceState.getBoolean("editable");
            index = savedInstanceState.getInt("index");
            initialListTop = savedInstanceState.getInt("listTop");
        } else {
            editable = getArguments().getBoolean("editable");
            index = getArguments().getInt("index");
            initialListTop = 0;
        }
        categoryNames = new ArrayList<String>();
        categoryNames.add(getString(R.string.detail_panel_cats_loading));

        final View view = inflater.inflate(R.layout.fragment_media_detail, container, false);

        image = (ImageView) view.findViewById(R.id.mediaDetailImage);
        loadingProgress = (ProgressBar) view.findViewById(R.id.mediaDetailImageLoading);
        loadingFailed = (ImageView) view.findViewById(R.id.mediaDetailImageFailed);
        scrollView = (ScrollView) view.findViewById(R.id.mediaDetailScrollView);

        // Detail consists of a list view with main pane in header view, plus category list.
        spacer = (fr.free.nrw.commons.media.MediaDetailSpacer) view.findViewById(R.id.mediaDetailSpacer);
        title = (TextView) view.findViewById(R.id.mediaDetailTitle);
        desc = (TextView) view.findViewById(R.id.mediaDetailDesc);
        license = (TextView) view.findViewById(R.id.mediaDetailLicense);
        categoryContainer = (LinearLayout) view.findViewById(R.id.mediaDetailCategoryContainer);

        licenseList = new LicenseList(getActivity());

        Media media = detailProvider.getMediaAtPosition(index);
        if (media == null) {
            // Ask the detail provider to ping us when we're ready
            Log.d("Commons", "MediaDetailFragment not yet ready to display details; registering observer");
            dataObserver = new DataSetObserver() {
                public void onChanged() {
                    Log.d("Commons", "MediaDetailFragment ready to display delayed details!");
                    detailProvider.unregisterDataSetObserver(dataObserver);
                    dataObserver = null;
                    displayMediaDetails(detailProvider.getMediaAtPosition(index));
                }
            };
            detailProvider.registerDataSetObserver(dataObserver);
        } else {
            Log.d("Commons", "MediaDetailFragment ready to display details");
            displayMediaDetails(media);
        }

        // Progressively darken the image in the background when we scroll detail pane up
        scrollListener = new ViewTreeObserver.OnScrollChangedListener() {
            public void onScrollChanged() {
                updateTheDarkness();
            }
        };
        view.getViewTreeObserver().addOnScrollChangedListener(scrollListener);

        // Layout layoutListener to size the spacer item relative to the available space.
        // There may be a .... better way to do this.
        layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private int currentHeight = -1;

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

    private void displayMediaDetails(final Media media) {
        //Always load image from Internet to allow viewing the desc, license, and cats
        String actualUrl = media.getThumbnailUrl(640);
        if(actualUrl.startsWith("http")) {
            Log.d("Volley", "Actual URL starts with http and is: " + actualUrl);

            ImageLoader loader = ((CommonsApplication)getActivity().getApplicationContext()).getImageLoader();
            MediaWikiImageView mwImage = (MediaWikiImageView)image;
            mwImage.setLoadingView(loadingProgress); //FIXME: Set this as an attribute
            mwImage.setMedia(media, loader);

            // FIXME: For transparent images
            // FIXME: keep the spinner going while we load data
            // FIXME: cache this data
            // Load image metadata: desc, license, categories
            detailFetchTask = new AsyncTask<Void, Void, Boolean>() {
                private MediaDataExtractor extractor;

                @Override
                protected void onPreExecute() {
                    extractor = new MediaDataExtractor(media.getFilename(), licenseList);
                }

                @Override
                protected Boolean doInBackground(Void... voids) {
                    try {
                        extractor.fetch();
                        return Boolean.TRUE;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return Boolean.FALSE;
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    detailFetchTask = null;

                    if (success.booleanValue()) {
                        extractor.fill(media);

                        // Set text of desc, license, and categories
                        desc.setText(prettyDescription(media));
                        license.setText(prettyLicense(media));

                        categoryNames.removeAll(categoryNames);
                        categoryNames.addAll(media.getCategories());

                        categoriesLoaded = true;
                        categoriesPresent = (categoryNames.size() > 0);
                        if (!categoriesPresent) {
                            // Stick in a filler element.
                            categoryNames.add(getString(R.string.detail_panel_cats_none));
                        }
                        rebuildCatList();
                    } else {
                        Log.d("Commons", "Failed to load photo details.");
                    }
                }
            };
            Utils.executeAsyncTask(detailFetchTask);
        } else {
            //This should not usually happen, image along with associated details should always be loaded from Internet, but keeping this for now for backup.
            //Even if image is loaded from device storage, it will display, albeit with empty desc and cat.
            Log.d("Volley", "Actual URL does not start with http and is: " + actualUrl);
            com.nostra13.universalimageloader.core.ImageLoader.getInstance().displayImage(actualUrl, image, displayOptions, new ImageLoadingListener() {
                public void onLoadingStarted(String s, View view) {
                    loadingProgress.setVisibility(View.VISIBLE);
                }

                public void onLoadingFailed(String s, View view, FailReason failReason) {
                    loadingProgress.setVisibility(View.GONE);
                    loadingFailed.setVisibility(View.VISIBLE);
                }

                public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                    loadingProgress.setVisibility(View.GONE);
                    loadingFailed.setVisibility(View.GONE);
                    image.setVisibility(View.VISIBLE);
                    if(bitmap.hasAlpha()) {
                        image.setBackgroundResource(android.R.color.white);
                    }

                    // Set text of desc, license, and categories
                    desc.setText(prettyDescription(media));
                    license.setText(prettyLicense(media));

                    categoryNames.removeAll(categoryNames);
                    categoryNames.addAll(media.getCategories());

                    categoriesLoaded = true;
                    categoriesPresent = (categoryNames.size() > 0);
                    if (!categoriesPresent) {
                        // Stick in a filler element.
                        categoryNames.add(getString(R.string.detail_panel_cats_none));
                    }
                    rebuildCatList();
                }

                public void onLoadingCancelled(String s, View view) {
                    Log.e("Volley", "Image loading cancelled. But why?");
                }
            });
        }

        title.setText(media.getDisplayTitle());
        desc.setText(""); // fill in from network...
        license.setText(""); // fill in from network...
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        displayOptions = Utils.getGenericDisplayOptions().build();
    }

    @Override
    public void onDestroyView() {
        if (detailFetchTask != null) {
            detailFetchTask.cancel(true);
            detailFetchTask = null;
        }
        if (layoutListener != null) {
            getView().getViewTreeObserver().removeGlobalOnLayoutListener(layoutListener); // old Android was on crack. CRACK IS WHACK
            layoutListener = null;
        }
        if (scrollListener != null) {
            getView().getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
            scrollListener  = null;
        }
        if (dataObserver != null) {
            detailProvider.unregisterDataSetObserver(dataObserver);
            dataObserver = null;
        }
        super.onDestroyView();
    }

    private void rebuildCatList() {
        // @fixme add the category items
        for (String cat : categoryNames) {
            View catLabel = buildCatLabel(cat);
            categoryContainer.addView(catLabel);
        }
    }

    private View buildCatLabel(String cat) {
        final String catName = cat;
        final View item = getLayoutInflater(null).inflate(R.layout.detail_category_item, null, false);
        final TextView textView = (TextView)item.findViewById(R.id.mediaDetailCategoryItemText);

        textView.setText(cat);
        if (categoriesLoaded && categoriesPresent) {
            textView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    String selectedCategoryTitle = "Category:" + catName;
                    Intent viewIntent = new Intent();
                    viewIntent.setAction(Intent.ACTION_VIEW);
                    viewIntent.setData(Utils.uriForWikiPage(selectedCategoryTitle));
                    startActivity(viewIntent);
                }
            });
        }
        return item;
    }

    private void updateTheDarkness() {
        // You must face the darkness alone
        int scrollY = scrollView.getScrollY();
        int scrollMax = getView().getHeight();
        float scrollPercentage = (float)scrollY / (float)scrollMax;
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
        Log.d("Commons", "Media license is: " + licenseKey);
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
}
