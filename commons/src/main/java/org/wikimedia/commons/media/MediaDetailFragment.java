package org.wikimedia.commons.media;

import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.toolbox.NetworkImageView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

import com.android.volley.toolbox.*;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;
import org.wikimedia.commons.*;

import java.io.IOException;
import java.util.ArrayList;

public class MediaDetailFragment extends SherlockFragment {

    private boolean editable;
    private DisplayImageOptions displayOptions;
    private MediaDetailPagerFragment.MediaDetailProvider detailProvider;
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
    private MediaDetailSpacer spacer;
    private int initialListIndex = 0;
    private int initialListTop = 0;

    private TextView title;
    private TextView desc;
    private TextView license;
    private ListView listView;
    private ArrayList<String> categoryNames;
    private boolean categoriesLoaded = false;
    private boolean categoriesPresent = false;
    private ArrayAdapter categoryAdapter;
    private ViewTreeObserver.OnGlobalLayoutListener observer; // for layout stuff, only used once!
    private AsyncTask<Void,Void,Boolean> detailFetchTask;


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", index);
        outState.putBoolean("editable", editable);

        getScrollPosition();
        outState.putInt("listIndex", initialListIndex);
        outState.putInt("listTop", initialListTop);
    }

    private void getScrollPosition() {
        int initialListIndex = listView.getFirstVisiblePosition();
        View firstVisibleItem = listView.getChildAt(initialListIndex);
        if (firstVisibleItem == null) {
            initialListTop = 0;
        } else {
            initialListTop = firstVisibleItem.getTop();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        detailProvider = (MediaDetailPagerFragment.MediaDetailProvider)getActivity();

        if(savedInstanceState != null) {
            editable = savedInstanceState.getBoolean("editable");
            index = savedInstanceState.getInt("index");
            initialListIndex = savedInstanceState.getInt("listIndex");
            initialListTop = savedInstanceState.getInt("listTop");
        } else {
            editable = getArguments().getBoolean("editable");
            index = getArguments().getInt("index");
        }
        final Media media = detailProvider.getMediaAtPosition(index);
        categoryNames = new ArrayList<String>();
        categoryNames.add(getString(R.string.detail_panel_cats_loading));

        final View view = inflater.inflate(R.layout.fragment_media_detail, container, false);

        image = (ImageView) view.findViewById(R.id.mediaDetailImage);
        loadingProgress = (ProgressBar) view.findViewById(R.id.mediaDetailImageLoading);
        loadingFailed = (ImageView) view.findViewById(R.id.mediaDetailImageFailed);
        listView = (ListView) view.findViewById(R.id.mediaDetailListView);

        // Detail consists of a list view with main pane in header view, plus category list.
        View detailView = getActivity().getLayoutInflater().inflate(R.layout.detail_main_panel, null, false);
        listView.addHeaderView(detailView, null, false);
        categoryAdapter = new ArrayAdapter(getActivity(), R.layout.detail_category_item, categoryNames);
        listView.setAdapter(categoryAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (categoriesLoaded && categoriesPresent) {
                    String selectedCategoryTitle = "Category:" + categoryNames.get(position - 1);
                    Intent viewIntent = new Intent();
                    viewIntent.setAction(Intent.ACTION_VIEW);
                    viewIntent.setData(Utils.uriForWikiPage(selectedCategoryTitle));
                    startActivity(viewIntent);
                }
            }
        });

        spacer = (MediaDetailSpacer) detailView.findViewById(R.id.mediaDetailSpacer);
        title = (TextView) detailView.findViewById(R.id.mediaDetailTitle);
        desc = (TextView) detailView.findViewById(R.id.mediaDetailDesc);
        license = (TextView) detailView.findViewById(R.id.mediaDetailLicense);

        // Enable or disable editing on the title
        /*
        title.setClickable(editable);
        title.setFocusable(editable);
        title.setCursorVisible(editable);
        title.setFocusableInTouchMode(editable);
        if(!editable) {
            title.setBackgroundDrawable(null);
        }
        */


        String actualUrl = TextUtils.isEmpty(media.getImageUrl()) ? media.getLocalUri().toString() : media.getThumbnailUrl(640);
        if(actualUrl.startsWith("http")) {
            ImageLoader loader = ((CommonsApplication)getActivity().getApplicationContext()).getImageLoader();
            MediaWikiImageView mwImage = (MediaWikiImageView)image;
            mwImage.setLoadingView(loadingProgress); //FIXME: Set this as an attribute
            mwImage.setMedia(media, loader);
            Log.d("Volley", actualUrl);
            // FIXME: For transparent images

            // Load image metadata: desc, license, categories
            // FIXME: keep the spinner going while we load data
            // FIXME: cache this data
            detailFetchTask = new AsyncTask<Void, Void, Boolean>() {
                private MediaDataExtractor extractor;
                private LicenseList licenseList;

                @Override
                protected void onPreExecute() {
                    licenseList = new LicenseList(getActivity());
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

                        // Fill some fields
                        desc.setText(media.getDescription("en"));

                        String licenseKey = media.getLicense();
                        License licenseObj = licenseList.get(licenseKey);
                        if (licenseObj == null) {
                            license.setText(licenseKey);
                        } else {
                            license.setText(licenseObj.getName());
                        }
                        Log.d("Commons", "Media license is: " + media.getLicense());


                        categoryNames.removeAll(categoryNames);
                        categoryNames.addAll(media.getCategories());

                        categoriesLoaded = true;
                        categoriesPresent = (categoryNames.size() > 0);
                        if (!categoriesPresent) {
                            // Stick in a filler element.
                            categoryNames.add(getString(R.string.detail_panel_cats_none));
                        }

                        categoryAdapter.notifyDataSetChanged();
                    } else {
                        Log.d("Commons", "Failed to load photo details.");
                    }
                }
            };
            Utils.executeAsyncTask(detailFetchTask);
        } else {
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
                }

                public void onLoadingCancelled(String s, View view) {
                    throw new RuntimeException("Image loading cancelled. But why?");
                }
            });
        }

        title.setText(media.getDisplayTitle());
        desc.setText(""); // fill in from network...
        license.setText(""); // fill in from network...

        /*
        title.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                detailProvider.getMediaAtPosition(index).setFilename(title.getText().toString());
                detailProvider.getMediaAtPosition(index).setTag("isDirty", true);
                detailProvider.notifyDatasetChanged();
            }

            public void afterTextChanged(Editable editable) {

            }
        });
        */

        // Layout observer to size the spacer item relative to the available space.
        // There may be a .... better way to do this.
        observer = new ViewTreeObserver.OnGlobalLayoutListener() {
            private int currentHeight = -1;

            public void onGlobalLayout() {
                int viewHeight = view.getHeight();
                //int textHeight = title.getLineHeight();
                int paddingDp = 48;
                float paddingPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, paddingDp, getResources().getDisplayMetrics());
                int newHeight = viewHeight - Math.round(paddingPx);

                if (newHeight != currentHeight) {
                    currentHeight = newHeight;
                    ViewGroup.LayoutParams params = spacer.getLayoutParams();
                    params.height = newHeight;
                    spacer.setLayoutParams(params);

                    // hack hack to trigger relayout
                    categoryAdapter.notifyDataSetChanged();

                    listView.setSelectionFromTop(initialListIndex, initialListTop);
                }

            }
        };
        view.getViewTreeObserver().addOnGlobalLayoutListener(observer);
        return view;
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
        if (observer != null) {
            getView().getViewTreeObserver().removeGlobalOnLayoutListener(observer); // old Android was on crack. CRACK IS WHACK
            observer = null;
        }
        super.onDestroyView();
    }
}
