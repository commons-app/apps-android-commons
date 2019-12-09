package fr.free.nrw.commons.upload.mediaDetails;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.chrisbanes.photoview.PhotoView;
import com.jakewharton.rxbinding2.widget.RxTextView;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.Description;
import fr.free.nrw.commons.upload.DescriptionsAdapter;
import fr.free.nrw.commons.upload.SimilarImageDialogFragment;
import fr.free.nrw.commons.upload.Title;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.upload.UploadModel;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

import static fr.free.nrw.commons.utils.ImageUtils.getErrorMessageForResult;

public class UploadMediaDetailFragment extends UploadBaseFragment implements
        UploadMediaDetailsContract.View {

    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.ib_map)
    AppCompatImageButton ibMap;
    @BindView(R.id.ib_expand_collapse)
    AppCompatImageButton ibExpandCollapse;
    @BindView(R.id.ll_container_media_detail)
    LinearLayout llContainerMediaDetail;
    @BindView(R.id.et_title)
    EditText etTitle;
    @BindView(R.id.rv_descriptions)
    RecyclerView rvDescriptions;
    @BindView(R.id.backgroundImage)
    PhotoView photoViewBackgroundImage;
    @BindView(R.id.btn_next)
    AppCompatButton btnNext;
    @BindView(R.id.btn_previous)
    AppCompatButton btnPrevious;
    private DescriptionsAdapter descriptionsAdapter;
    @BindView(R.id.btn_copy_prev_title_desc)
    AppCompatButton btnCopyPreviousTitleDesc;

    private UploadModel.UploadItem uploadItem;
    private List<Description> descriptions;

    @Inject
    UploadMediaDetailsContract.UserActionListener presenter;

    @Inject
    @Named("default_preferences")
    JsonKvStore defaultKvStore;

    private UploadableFile uploadableFile;
    private String source;
    private Place place;

    private Title title;
    private boolean isExpanded = true;

    private UploadMediaDetailFragmentCallback callback;

    public void setCallback(UploadMediaDetailFragmentCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setImageTobeUploaded(UploadableFile uploadableFile, String source, Place place) {
        this.uploadableFile = uploadableFile;
        this.source = source;
        this.place = place;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upload_media_detail_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        init();
    }

    private void init() {
        tvTitle.setText(getString(R.string.step_count, callback.getIndexInViewFlipper(this) + 1,
                callback.getTotalNumberOfSteps()));
        title = new Title();
        initRecyclerView();
        initPresenter();
        Disposable disposable = RxTextView.textChanges(etTitle)
                .subscribe(text -> {
                    if (!TextUtils.isEmpty(text)) {
                        btnNext.setEnabled(true);
                        btnNext.setClickable(true);
                        btnNext.setAlpha(1.0f);
                        title.setTitleText(text.toString());
                        uploadItem.setTitle(title);
                    } else {
                        btnNext.setAlpha(0.5f);
                        btnNext.setEnabled(false);
                        btnNext.setClickable(false);
                    }
                });
        compositeDisposable.add(disposable);
        presenter.receiveImage(uploadableFile, source, place);

        if (callback.getIndexInViewFlipper(this) == 0) {
            btnPrevious.setEnabled(false);
            btnPrevious.setAlpha(0.5f);
        } else {
            btnPrevious.setEnabled(true);
            btnPrevious.setAlpha(1.0f);
        }

        //If this is the first media, we have nothing to copy, lets not show the button
        if (callback.getIndexInViewFlipper(this) == 0) {
            btnCopyPreviousTitleDesc.setVisibility(View.GONE);
        } else {
            btnCopyPreviousTitleDesc.setVisibility(View.VISIBLE);
        }

        attachImageViewScaleChangeListener();

        addEtTitleTouchListener();
    }

    /**
     * Handles the drawable click listener for Edit Text
     */
    private void addEtTitleTouchListener() {
        etTitle.setOnTouchListener((v, event) -> {
            //2 is for drawable right
            float twelveDpInPixels = convertDpToPixel(12, getContext());
            if (event.getAction() == MotionEvent.ACTION_UP && etTitle.getCompoundDrawables() != null
                    && etTitle.getCompoundDrawables().length > 2 && etTitle
                    .getCompoundDrawables()[2].getBounds()
                    .contains((int) (etTitle.getWidth() - (event.getX() + twelveDpInPixels)),
                            (int) (event.getY() - twelveDpInPixels))) {
                showInfoAlert(R.string.media_detail_title, R.string.title_info);
                return true;
            }
            return false;
        });
    }

    /**
     * converts dp to pixel
     * @param dp
     * @param context
     * @return
     */
    private float convertDpToPixel(float dp, Context context) {
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Attaches the scale change listener to the image view
     */
    private void attachImageViewScaleChangeListener() {
        photoViewBackgroundImage.setOnScaleChangeListener(
                (scaleFactor, focusX, focusY) -> {
                    //Whenever the uses plays with the image, lets collapse the media detail container
                    expandCollapseLlMediaDetail(false);
                });
    }

    /**
     * attach the presenter with the view
     */
    private void initPresenter() {
        presenter.onAttachView(this);
    }

    /**
     * init the recycler veiw
     */
    private void initRecyclerView() {
        descriptionsAdapter = new DescriptionsAdapter(defaultKvStore.getString(Prefs.KEY_LANGUAGE_VALUE, ""));
        descriptionsAdapter.setCallback(this::showInfoAlert);
        rvDescriptions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDescriptions.setAdapter(descriptionsAdapter);
    }

    /**
     * returns the default locale value of the user's device
     * @return
     */
    private String getUserDefaultLocale() {
        return getContext().getResources().getConfiguration().locale.getLanguage();
    }

    /**
     * show dialog with info
     * @param titleStringID
     * @param messageStringId
     */
    private void showInfoAlert(int titleStringID, int messageStringId) {
        DialogUtil.showAlertDialog(getActivity(), getString(titleStringID), getString(messageStringId), getString(android.R.string.ok), null, true);
    }

    @OnClick(R.id.btn_next)
    public void onNextButtonClicked() {
        uploadItem.setDescriptions(descriptionsAdapter.getDescriptions());
        presenter.verifyImageQuality(uploadItem, true);
    }

    @OnClick(R.id.btn_previous)
    public void onPreviousButtonClicked() {
        callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this));
    }

    @OnClick(R.id.btn_add_description)
    public void onButtonAddDescriptionClicked() {
        Description description = new Description();
        description.setManuallyAdded(true);//This was manually added by the user
        descriptionsAdapter.addDescription(description);
    }

    @Override
    public void showSimilarImageFragment(String originalFilePath, String possibleFilePath) {
        SimilarImageDialogFragment newFragment = new SimilarImageDialogFragment();
        newFragment.setCallback(new SimilarImageDialogFragment.Callback() {
            @Override
            public void onPositiveResponse() {
                Timber.d("positive response from similar image fragment");
            }

            @Override
            public void onNegativeResponse() {
                Timber.d("negative response from similar image fragment");
            }
        });
        Bundle args = new Bundle();
        args.putString("originalImagePath", originalFilePath);
        args.putString("possibleImagePath", possibleFilePath);
        newFragment.setArguments(args);
        newFragment.show(getChildFragmentManager(), "dialog");
    }

    @Override
    public void onImageProcessed(UploadItem uploadItem, Place place) {
        this.uploadItem = uploadItem;
        if (uploadItem.getTitle() != null) {
            etTitle.setText(uploadItem.getTitle().toString());
        }

        descriptions = uploadItem.getDescriptions();
        photoViewBackgroundImage.setImageURI(uploadItem.getMediaUri());
        setDescriptionsInAdapter(descriptions);
    }

    @Override
    public void showProgress(boolean shouldShow) {
        callback.showProgress(shouldShow);
    }

    @Override
    public void onImageValidationSuccess() {
        presenter.setUploadItem(callback.getIndexInViewFlipper(this), uploadItem);
        callback.onNextButtonClicked(callback.getIndexInViewFlipper(this));
    }

    @Override
    public void showMessage(int stringResourceId, int colorResourceId) {
        ViewUtil.showLongToast(getContext(), stringResourceId);
    }

    @Override
    public void showMessage(String message, int colorResourceId) {
        ViewUtil.showLongToast(getContext(), message);
    }

    @Override
    public void showDuplicatePicturePopup() {
        String uploadTitleFormat = getString(R.string.upload_title_duplicate);
        DialogUtil.showAlertDialog(getActivity(),
                getString(R.string.warning),
                String.format(Locale.getDefault(),
                        uploadTitleFormat,
                        uploadItem.getFileName()),
                () -> {

                },
                () -> {
                    uploadItem.setImageQuality(ImageUtils.IMAGE_KEEP);
                    onNextButtonClicked();
                });
    }

    @Override
    public void showBadImagePopup(Integer errorCode) {
        String errorMessageForResult = getErrorMessageForResult(getContext(), errorCode);
        if (!StringUtils.isBlank(errorMessageForResult)) {
            DialogUtil.showAlertDialog(getActivity(),
                    getString(R.string.warning),
                    errorMessageForResult,
                    () -> deleteThisPicture(),
                    () -> {
                        uploadItem.setImageQuality(ImageUtils.IMAGE_KEEP);
                        onNextButtonClicked();
                    });
        }
        //If the error message is null, we will probably not show anything
    }

    @Override public void showMapWithImageCoordinates(boolean shouldShow) {
        ibMap.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setTitleAndDescription(String title, List<Description> descriptions) {
        etTitle.setText(title);
        setDescriptionsInAdapter(descriptions);
    }

    private void deleteThisPicture() {
        callback.deletePictureAtIndex(callback.getIndexInViewFlipper(this));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDetachView();
    }

    @OnClick(R.id.rl_container_title)
    public void onRlContainerTitleClicked() {
        expandCollapseLlMediaDetail(!isExpanded);
    }

    /**
     * show hide media detail based on
     * @param shouldExpand
     */
    private void expandCollapseLlMediaDetail(boolean shouldExpand){
        llContainerMediaDetail.setVisibility(shouldExpand ? View.VISIBLE : View.GONE);
        isExpanded = !isExpanded;
        ibExpandCollapse.setRotation(ibExpandCollapse.getRotation() + 180);
    }

    @OnClick(R.id.ib_map) public void onIbMapClicked() {
        Utils.handleGeoCoordinates(getContext(),
            new LatLng(uploadItem.getGpsCoords().getDecLatitude(),
                uploadItem.getGpsCoords().getDecLongitude(), 0.0f));
    }


    public interface UploadMediaDetailFragmentCallback extends Callback {

        void deletePictureAtIndex(int index);
    }


    @OnClick(R.id.btn_copy_prev_title_desc)
    public void onButtonCopyPreviousTitleDesc(){
        presenter.fetchPreviousTitleAndDescription(callback.getIndexInViewFlipper(this));
    }

    private void setDescriptionsInAdapter(List<Description> descriptions) {
        if (descriptions == null) {
            descriptions = new ArrayList<>();
        }
        if (descriptions.size() == 0) {
            descriptionsAdapter.addDescription(new Description());
        } else {
            descriptionsAdapter.setItems(descriptions);
        }
    }

}
