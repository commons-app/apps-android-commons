package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.modifications.CategoryModifier;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.modifications.TemplateRemoveModifier;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@Singleton
public class UploadPresenter {
    private static final String TOP_CARD_STATE = "fr.free.nrw.commons.upload.top_card_state";
    private static final String BOTTOM_CARD_STATE = "fr.free.nrw.commons.upload.bottom_card_state";

    private final UploadModel uploadModel;
    private final UploadController uploadController;
    private UploadView view;// = UploadView.DUMMY;


    @Inject
    public UploadPresenter(UploadModel uploadModel, UploadController uploadController) {
        this.uploadModel = uploadModel;
        this.uploadController = uploadController;
    }

    public void receive(Uri mediaUri, String mimeType, String source) {
        receive(Collections.singletonList(mediaUri), mimeType, source);
    }

    @SuppressLint("CheckResult")
    public void receive(List<Uri> media, String mimeType, String source) {
        Completable.fromRunnable(() -> uploadModel.receive(media, mimeType, source)
        ).subscribeOn(
                Schedulers.io()
        ).observeOn(
                AndroidSchedulers.mainThread()
        ).subscribe(
                () -> {
                    updateCards(view);
                    updateLicenses(view);
                    updateContent();
                });

    }

    public void imageTitleChanged(String text) {
        uploadModel.getCurrentItem().title = text;
        uploadModel.getCurrentItem().error = text.trim().isEmpty();
        view.updateTopCardContent();
    }

    public void selectLicense(String licenseName) {
        uploadModel.setSelectedLicense(licenseName);
        view.updateLicenseSummary(uploadModel.getSelectedLicense());
    }

    public void descriptionChanged(String text) {
        uploadModel.getCurrentItem().description = text;
    }

    //region Wizard step management
    public void handleNext() {
        uploadModel.next();
        updateContent();
        view.dismissKeyboard();
    }

    public void handlePrevious() {
        uploadModel.previous();
        updateContent();
        view.dismissKeyboard();
    }

    public void thumbnailClicked(UploadModel.UploadItem item) {
        uploadModel.jumpTo(item);
        updateContent();
    }

    @SuppressLint("CheckResult")
    public void handleSubmit(CategoriesModel categoriesModel) {
        uploadModel.toContributions().subscribe(contribution -> {
            contribution.setCategories(categoriesModel.getCategoryStringList());
            uploadController.startUpload(contribution);
        });
    }

    //endregion

    //region Top and Bottom card state management
    public void toggleTopCardState() {
        uploadModel.setTopCardState(!uploadModel.isTopCardState());
        view.setTopCardState(uploadModel.isTopCardState());
    }

    public void toggleBottomCardState() {
        uploadModel.setBottomCardState(!uploadModel.isBottomCardState());
        view.setBottomCardState(uploadModel.isBottomCardState());
    }
    //endregion

    //region View / Lifecycle management
    public void initFromSavedState(Bundle state) {
        if (state != null) {
            uploadModel.setTopCardState(state.getBoolean(TOP_CARD_STATE, true));
            uploadModel.setBottomCardState(state.getBoolean(BOTTOM_CARD_STATE, true));
        }
        uploadController.prepareService();
    }

    public void cleanup() {
        uploadController.cleanup();
    }

    public Bundle getSavedState() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(TOP_CARD_STATE, uploadModel.isTopCardState());
        bundle.putBoolean(BOTTOM_CARD_STATE, uploadModel.isBottomCardState());
        return bundle;
    }

    public void removeView() {
        this.view = null;//UploadView.DUMMY;
    }

    public void addView(UploadView view) {
        this.view = view;

        updateCards(view);
        updateLicenses(view);
        updateContent();
    }

    void updateCards(UploadView view) {
        view.updateThumbnails(uploadModel.getUploads());
        view.setTopCardVisibility(uploadModel.getCount() > 1);
        view.setBottomCardVisibility(uploadModel.getCount() > 0);
        view.setTopCardState(uploadModel.isTopCardState());
        view.setBottomCardState(uploadModel.isBottomCardState());
    }

    void updateLicenses(UploadView view) {
        String selectedLicense = uploadModel.getSelectedLicense();
        view.updateLicenses(uploadModel.getLicenses(), selectedLicense);
        view.updateLicenseSummary(selectedLicense);
    }

    void updateContent() {
        view.setNextEnabled(uploadModel.isNextAvailable());
        view.setPreviousEnabled(uploadModel.isPreviousAvailable());
        view.setSubmitEnabled(uploadModel.isSubmitAvailable());

        view.setBackground(uploadModel.getCurrentItem().mediaUri);

        view.updateBottomCardContent(uploadModel.getCurrentStep(), uploadModel.getStepCount(), uploadModel.getCurrentItem());
        view.updateTopCardContent();

        showCorrectBottomCard(uploadModel.getCurrentStep(), uploadModel.getCount());
    }

    void showCorrectBottomCard(int currentStep, int uploadCount) {
        @UploadView.UploadPage int page;
        if (uploadCount == 0) {
            page = UploadView.PLEASE_WAIT;
        } else if (currentStep <= uploadCount) {
            page = UploadView.TITLE_CARD;
        } else if (currentStep == uploadCount + 1) {
            page = UploadView.CATEGORIES;
        } else {
            page = UploadView.LICENSE;
        }
        view.setBottomCardVisibility(page);
    }
    //endregion
}