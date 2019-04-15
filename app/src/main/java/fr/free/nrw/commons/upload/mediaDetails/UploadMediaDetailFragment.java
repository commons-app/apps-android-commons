package fr.free.nrw.commons.upload.mediaDetails;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.chrisbanes.photoview.PhotoView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.Description;
import fr.free.nrw.commons.upload.DescriptionsAdapter;
import fr.free.nrw.commons.upload.SimilarImageDialogFragment;
import fr.free.nrw.commons.upload.SimilarImageDialogFragment.onResponse;
import fr.free.nrw.commons.upload.Title;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.upload.UploadModel;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public class UploadMediaDetailFragment extends UploadBaseFragment implements
        IUploadMediaDetails.View {

    @BindView(R.id.rl_container_title)
    RelativeLayout rlContainerTitle;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.ll_container_media_detail)
    LinearLayout llContainerMediaDetail;
    @BindView(R.id.til_container_title)
    TextInputLayout tilContainerTitle;
    @BindView(R.id.et_title)
    EditText etTitle;
    @BindView(R.id.rv_descriptions)
    RecyclerView rvDescriptions;
    @BindView(R.id.btn_add_description)
    AppCompatButton btnAddDescription;
    @BindView(R.id.backgroundImage)
    PhotoView photoViewBackgroundImage;
    @BindView(R.id.btn_next)
    AppCompatButton btnNext;
    @BindView(R.id.btn_previous)
    AppCompatButton btnPrevious;
    @BindView(R.id.pb_upload_media_details)
    ProgressBar pbUploadMediaDetails;
    private DescriptionsAdapter descriptionsAdapter;

    int indexInViewFlipper = 0;

    private UploadModel.UploadItem uploadItem;
    private List<Description> descriptions;

    @Inject
    IUploadMediaDetails.UserActionListener presenter;
    private UploadableFile uploadableFile;
    private String source;
    private Place place;

    private Title title;
    private boolean isExpanded = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplicationlessInjection
                .getInstance(getActivity().getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);
    }

    public void setImageTobeUploaded(UploadableFile uploadableFile, String source, Place place) {
        this.uploadableFile = uploadableFile;
        this.source = source;
        this.place = place;
    }

    public void setIndexInViewFlipper(int indexInViewFlipper) {
        this.indexInViewFlipper = indexInViewFlipper;
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
        tvTitle.setText(getString(R.string.step_count, indexInViewFlipper + 1,
                totalNumberOfSteps));
        title = new Title();
        initRecyclerView();
        initPresenter();
        RxTextView.textChanges(etTitle)
                .subscribe(text -> {
                    btnNext.setEnabled(!TextUtils.isEmpty(text));
                    if (!TextUtils.isEmpty(text)) {
                        title.setTitleText(text.toString());
                        uploadItem.setTitle(title);
                    }
                });
        presenter.receiveImage(uploadableFile, source, place);

        if (indexInViewFlipper == 0) {
            btnPrevious.setEnabled(false);
            btnPrevious.setAlpha(0.5f);
        } else {
            btnPrevious.setEnabled(true);
            btnPrevious.setAlpha(1.0f);
        }
    }

    private void initPresenter() {
        presenter.onAttachView(this);
    }

    private void initRecyclerView() {
        descriptionsAdapter = new DescriptionsAdapter();
        descriptionsAdapter.setCallback(this::showInfoAlert);
        rvDescriptions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDescriptions.setAdapter(descriptionsAdapter);
    }

    private void addNewDescription() {
        rvDescriptions.scrollToPosition(descriptionsAdapter.getItemCount() - 1);
    }

    private void showInfoAlert(int titleStringID, int messageStringId, String... formatArgs) {
        new AlertDialog.Builder(getContext()).setTitle(titleStringID)
                .setMessage(getString(messageStringId, (Object[]) formatArgs))
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                .create()
                .show();
    }

    @OnClick(R.id.btn_next)
    public void onNextButtonClicked() {
        uploadItem.setDescriptions(descriptionsAdapter.getDescriptions());
        presenter.verifyImageQuality(uploadItem, true);
    }

    @OnClick(R.id.btn_previous)
    public void onPreviousButtonClicked() {
        callback.onPreviousButtonClicked(indexInViewFlipper);
    }

    @OnClick(R.id.btn_add_description)
    public void onButtonAddDescriptionClicked() {
        descriptionsAdapter.addDescription(new Description());
    }

    @Override
    public void showSimilarImageFragment(String originalFilePath, String possibleFilePath) {
        SimilarImageDialogFragment newFragment = new SimilarImageDialogFragment();
        newFragment.setmOnResponse(new onResponse() {
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
        if (descriptions == null) {
            descriptions = new ArrayList<>();
        }
        photoViewBackgroundImage.setImageURI(uploadItem.getMediaUri());
        descriptionsAdapter.setItems(descriptions);
        addNewDescription();
    }

    @Override
    public void showProgress(boolean shouldShow) {
        pbUploadMediaDetails.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onImageValidationSuccess() {
        presenter.setUploadItem(uploadItem);
        callback.onNextButtonClicked(indexInViewFlipper);
    }

    @Override
    public void showMessage(int stringResourceId, int colorResourceId) {
        ViewUtil.showLongToast(getContext(), stringResourceId);
        //TODO, add a proper view with retry
    }

    @Override
    public void showMessage(String message, int colorResourceId) {
        ViewUtil.showLongToast(getContext(), message);
        //TODO, add a proper view with retry
    }

    @Override
    public void showDuplicatePicturePopup() {
        DialogUtil.showAlertDialog(getActivity(),
                getString(R.string.warning),
                String.format(getString(R.string.upload_title_duplicate),
                        uploadItem.getFileName()),
                null,
                () -> {
                    uploadItem.setImageQuality(ImageUtils.IMAGE_KEEP);
                    onNextButtonClicked();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDetachView();
    }

    @OnClick(R.id.rl_container_title)
    public void onRlContainerTitleClicked() {
        llContainerMediaDetail.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
        isExpanded = !isExpanded;
    }

}
