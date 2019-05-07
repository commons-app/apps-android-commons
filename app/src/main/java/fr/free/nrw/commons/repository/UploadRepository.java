package fr.free.nrw.commons.repository;

import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.SimilarImageInterface;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UploadRepository {

    private UploadLocalDataSource localDataSource;
    private UploadRemoteDataSource remoteDataSource;

    @Inject
    public UploadRepository(UploadLocalDataSource localDataSource,
            UploadRemoteDataSource remoteDataSource) {
        this.localDataSource = localDataSource;
        this.remoteDataSource = remoteDataSource;
    }

    public Observable<Contribution> buildContributions() {
        return remoteDataSource.buildContributions();
    }

    public void startUpload(Contribution contribution) {
        remoteDataSource.startUpload(contribution);
    }

    public UploadItem getCurrentItem() {
        return null;
    }

    public List<UploadItem> getUploads() {
        return remoteDataSource.getUploads();
    }

    public void prepareService() {
        remoteDataSource.prepareService();
    }

    public void cleanup() {
        localDataSource.cleanUp();
    }

    public List<CategoryItem> getSelectedCategories() {
        return remoteDataSource.getSelectedCategories();
    }

    public Observable<CategoryItem> searchAll(String query, List<String> imageTitleList) {
        return remoteDataSource.searchAll(query, imageTitleList);
    }

    public Observable<CategoryItem> searchCategories(String query, List<String> imageTitleList) {
        return remoteDataSource.searchCategories(query, imageTitleList);
    }

    public Observable<CategoryItem> defaultCategories(List<String> imageTitleList) {
        return remoteDataSource.defaultCategories(imageTitleList);
    }

    public List<String> getCategoryStringList() {
        return remoteDataSource.getCategoryStringList();
    }

    public void setSelectedCategories(List<String> categoryStringList) {
        remoteDataSource.setSelectedCategories(categoryStringList);
    }

    public void onCategoryClicked(CategoryItem categoryItem) {
        remoteDataSource.onCategoryClicked(categoryItem);
    }

    public Comparator<? super CategoryItem> sortBySimilarity(String query) {
        return remoteDataSource.sortBySimilarity(query);
    }

    public boolean containsYear(String name) {
        return remoteDataSource.containsYear(name);
    }

    public List<String> getLicenses() {
        return localDataSource.getLicenses();
    }

    public String getFromDefaultKvStore(String key, String defaultValue) {
        return localDataSource.getFromDefaultKvStore(key, defaultValue);
    }

    public void saveInDefaultKvStore(String key, String value) {
        localDataSource.saveInDefaultKvStore(key, value);
    }

    public void setSelectedLicense(String licenseName) {
        localDataSource.setSelectedLicense(licenseName);
    }

    public String getSelectedLicense() {
        return localDataSource.getSelectedLicense();
    }

    public int getCount() {
        return localDataSource.getCount();
    }

    public Observable<UploadItem> preProcessImage(UploadableFile uploadableFile, Place place,
            String source, SimilarImageInterface similarImageInterface) {
        return remoteDataSource
                .preProcessImage(uploadableFile, place, source, similarImageInterface);
    }

    public Single<Integer> getImageQuality(UploadItem uploadItem, boolean shouldValidateTitle) {
        return remoteDataSource.getImageQuality(uploadItem, shouldValidateTitle);
    }

    public void updateUploadItem(int index, UploadItem uploadItem) {
        localDataSource.updateUploadItem(index, uploadItem);
    }

    public void saveInDirectKvStore(String key, boolean value) {
        localDataSource.saveInDirectKvStore(key, value);
    }

    public void deletePicture(String filePath) {
        localDataSource.deletePicture(filePath);
    }
}
