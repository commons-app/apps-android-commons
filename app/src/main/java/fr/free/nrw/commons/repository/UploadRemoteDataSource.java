package fr.free.nrw.commons.repository;

import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.SimilarImageInterface;
import fr.free.nrw.commons.upload.UploadController;
import fr.free.nrw.commons.upload.UploadModel;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * This class would act as the data source for remote operations for UploadActivity
 */
@Singleton
public class UploadRemoteDataSource {

    private UploadModel uploadModel;
    private UploadController uploadController;
    private CategoriesModel categoriesModel;

    @Inject
    public UploadRemoteDataSource(UploadModel uploadModel, UploadController uploadController,
                                  CategoriesModel categoriesModel) {
        this.uploadModel = uploadModel;
        this.uploadController = uploadController;
        this.categoriesModel = categoriesModel;
    }

    /**
     * asks the UploadModel to build the contributions
     *
     * @return
     */
    public Observable<Contribution> buildContributions() {
        return uploadModel.buildContributions();
    }

    /**
     * asks the UploadService to star the uplaod for
     *
     * @param contribution
     */
    public void startUpload(Contribution contribution) {
        uploadController.startUpload(contribution);
    }

    /**
     * returns the list of UploadItem from the UploadModel
     *
     * @return
     */
    public List<UploadItem> getUploads() {
        return uploadModel.getUploads();
    }

    /**
     * Prepare the UploadService for the upload
     */
    public void prepareService() {
        uploadController.prepareService();
    }

    /**
     * Clean up the UploadController
     */
    public void cleanup() {
        uploadController.cleanup();
    }

    /**
     * Clean up the selected categories
     */
    public void clearSelectedCategories(){
        //This needs further refactoring, this should not be here, right now the structure wont suppoort rhis
        categoriesModel.cleanUp();
    }

    /**
     * returnt the list of selected categories
     *
     * @return
     */
    public List<CategoryItem> getSelectedCategories() {
        return categoriesModel.getSelectedCategories();
    }

    /**
     * all categories from MWApi
     *
     * @param query
     * @param imageTitleList
     * @return
     */
    public Observable<CategoryItem> searchAll(String query, List<String> imageTitleList) {
        return categoriesModel.searchAll(query, imageTitleList);
    }

    /**
     * returns the string list of categories
     *
     * @return
     */
    public List<String> getCategoryStringList() {
        return categoriesModel.getCategoryStringList();
    }

    /**
     * sets the selected categories in the UploadModel
     *
     * @param categoryStringList
     */
    public void setSelectedCategories(List<String> categoryStringList) {
        uploadModel.setSelectedCategories(categoryStringList);
    }

    /**
     * handles category selection/unselection
     *
     * @param categoryItem
     */
    public void onCategoryClicked(CategoryItem categoryItem) {
        categoriesModel.onCategoryItemClicked(categoryItem);
    }

    /**
     * returns category sorted based on similarity with query
     *
     * @param query
     * @return
     */
    public Comparator<CategoryItem> sortBySimilarity(String query) {
        return categoriesModel.sortBySimilarity(query);
    }

    /**
     * prunes the category list for irrelevant categories see #750
     *
     * @param name
     * @return
     */
    public boolean containsYear(String name) {
        return categoriesModel.containsYear(name);
    }

    /**
     * pre process the UploadableFile
     *
     * @param uploadableFile
     * @param place
     * @param source
     * @param similarImageInterface
     * @return
     */
    public Observable<UploadItem> preProcessImage(UploadableFile uploadableFile, Place place,
                                                  String source, SimilarImageInterface similarImageInterface) {
        return uploadModel.preProcessImage(uploadableFile, place, source, similarImageInterface);
    }

    /**
     * ask the UplaodModel for the image quality of the UploadItem
     *
     * @param uploadItem
     * @param shouldValidateTitle
     * @return
     */
    public Single<Integer> getImageQuality(UploadItem uploadItem, boolean shouldValidateTitle) {
        return uploadModel.getImageQuality(uploadItem, shouldValidateTitle);
    }
}
