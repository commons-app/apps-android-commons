package fr.free.nrw.commons.repository;

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
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

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

    public Observable<Contribution> buildContributions() {
        return uploadModel.buildContributions();
    }

    public void startUpload(Contribution contribution) {
        uploadController.startUpload(contribution);
    }

    public List<UploadItem> getUploads() {
        return uploadModel.getUploads();
    }

    public void prepareService() {
        uploadController.prepareService();
    }

    public void cleanup() {
        uploadController.cleanup();
    }

    public List<CategoryItem> getSelectedCategories() {
        return categoriesModel.getSelectedCategories();
    }

    public Observable<CategoryItem> searchAll(String query, List<String> imageTitleList) {
        return categoriesModel.searchAll(query, imageTitleList);
    }

    public Observable<CategoryItem> searchCategories(String query, List<String> imageTitleList) {
        return categoriesModel.searchCategories(query, imageTitleList);
    }

    public Observable<CategoryItem> defaultCategories(List<String> imageTitleList) {
        return categoriesModel.defaultCategories(imageTitleList);
    }

    public List<String> getCategoryStringList() {
        return categoriesModel.getCategoryStringList();
    }

    public void setSelectedCategories(List<String> categoryStringList) {
        uploadModel.setSelectedCategories(categoryStringList);
    }

    public void onCategoryClicked(CategoryItem categoryItem) {
        categoriesModel.onCategoryItemClicked(categoryItem);
    }

    public Comparator<CategoryItem> sortBySimilarity(String query) {
        return categoriesModel.sortBySimilarity(query);
    }

    public boolean containsYear(String name) {
        return categoriesModel.containsYear(name);
    }

    public Observable<UploadItem> preProcessImage(UploadableFile uploadableFile, Place place,
            String source, SimilarImageInterface similarImageInterface) {
        return uploadModel.preProcessImage(uploadableFile,place,source,similarImageInterface);
    }

    public Single<Integer> getImageQuality(UploadItem uploadItem, boolean shouldValidateTitle) {
        return uploadModel.getImageQuality(uploadItem,shouldValidateTitle);
    }
}
