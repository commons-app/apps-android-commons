package fr.free.nrw.commons.repository;

import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.ImageCoordinates;
import fr.free.nrw.commons.upload.SimilarImageInterface;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The repository class for UploadActivity
 */
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

    /**
     * asks the RemoteDataSource to build contributions
     *
     * @return
     */
    public Observable<Contribution> buildContributions() {
        return remoteDataSource.buildContributions();
    }

    /**
     * asks the RemoteDataSource to start upload for the contribution
     *
     * @param contribution
     */
    public void startUpload(Contribution contribution) {
        remoteDataSource.startUpload(contribution);
    }

    /**
     * Fetches and returns all the Upload Items
     *
     * @return
     */
    public List<UploadItem> getUploads() {
        return remoteDataSource.getUploads();
    }

    /**
     * asks the RemoteDataSource to prepare the Upload Service
     */
    public void prepareService() {
        remoteDataSource.prepareService();
    }

    /**
     *Prepare for a fresh upload
     */
    public void cleanup() {
        localDataSource.cleanUp();
        remoteDataSource.cleanUp();
    }

    /**
     * Fetches and returns the selected categories for the current upload
     *
     * @return
     */
    public List<CategoryItem> getSelectedCategories() {
        return remoteDataSource.getSelectedCategories();
    }

    /**
     * all categories from MWApi
     *
     * @param query
     * @param imageTitleList
     * @param selectedDepictions
     * @return
     */
    public Observable<List<CategoryItem>> searchAll(String query, List<String> imageTitleList,
        List<DepictedItem> selectedDepictions) {
        return remoteDataSource.searchAll(query, imageTitleList, selectedDepictions);
    }

    /**
     * sets the list of selected categories for the current upload
     *
     * @param categoryStringList
     */
    public void setSelectedCategories(List<String> categoryStringList) {
        remoteDataSource.setSelectedCategories(categoryStringList);
    }

    /**
     * handles the category selection/deselection
     *
     * @param categoryItem
     */
    public void onCategoryClicked(CategoryItem categoryItem) {
        remoteDataSource.onCategoryClicked(categoryItem);
    }

    /**
     * prunes the category list for irrelevant categories see #750
     *
     * @param name
     * @return
     */
    public boolean containsYear(String name) {
        return remoteDataSource.containsYear(name);
    }

    /**
     * retursn the string list of available license from the LocalDataSource
     *
     * @return
     */
    public List<String> getLicenses() {
        return localDataSource.getLicenses();
    }

    /**
     * returns the selected license for the current upload
     *
     * @return
     */
    public String getSelectedLicense() {
        return localDataSource.getSelectedLicense();
    }

    /**
     * returns the number of Upload Items
     *
     * @return
     */
    public int getCount() {
        return localDataSource.getCount();
    }

    /**
     * ask the RemoteDataSource to pre process the image
     *
     * @param uploadableFile
     * @param place
     * @param similarImageInterface
     * @return
     */
    public Observable<UploadItem> preProcessImage(UploadableFile uploadableFile, Place place,
        SimilarImageInterface similarImageInterface) {
        return remoteDataSource.preProcessImage(uploadableFile, place, similarImageInterface);
    }

    /**
     * query the RemoteDataSource for image quality
     *
     * @param uploadItem
     * @return
     */
    public Single<Integer> getImageQuality(UploadItem uploadItem) {
        return remoteDataSource.getImageQuality(uploadItem);
    }

    /**
     * asks the LocalDataSource to update the Upload Item
     *
     * @param index
     * @param uploadItem
     */
    public void updateUploadItem(int index, UploadItem uploadItem) {
        localDataSource.updateUploadItem(index, uploadItem);
    }

    /**
     * asks the LocalDataSource to delete the file with the given file path
     *
     * @param filePath
     */
    public void deletePicture(String filePath) {
        localDataSource.deletePicture(filePath);
    }

    /**
     * fetches and returns the previous upload item
     *
     * @param index
     * @return
     */
    public UploadItem getPreviousUploadItem(int index) {
        return localDataSource.getPreviousUploadItem(index);
    }

    /**
     * Save boolean value locally
     *
     * @param key
     * @param value
     */
    public void saveValue(String key, boolean value) {
        localDataSource.saveValue(key, value);
    }

    /**
     * save string value locally
     *
     * @param key
     * @param value
     */
    public void saveValue(String key, String value) {
        localDataSource.saveValue(key, value);
    }

    /**
     * fetch the string value for the associated key
     *
     * @param key
     * @param value
     * @return
     */
    public String getValue(String key, String value) {
        return localDataSource.getValue(key, value);
    }

    /**
     * set selected license for the current upload
     *
     * @param licenseName
     */
    public void setSelectedLicense(String licenseName) {
        localDataSource.setSelectedLicense(licenseName);
    }

    public void onDepictItemClicked(DepictedItem depictedItem) {
        remoteDataSource.onDepictedItemClicked(depictedItem);
    }

    /**
     * Fetches and returns the selected depictions for the current upload
     *
     * @return
     */

    public List<DepictedItem> getSelectedDepictions() {
        return remoteDataSource.getSelectedDepictions();
    }

    /**
     * Search all depictions from
     *
     * @param query
     * @return
     */

    public Flowable<List<DepictedItem>> searchAllEntities(String query) {
        return remoteDataSource.searchAllEntities(query);
    }

    /**
     * Returns nearest place matching the passed latitude and longitude
     * @param decLatitude
     * @param decLongitude
     * @return
     */
    public Place checkNearbyPlaces(double decLatitude, double decLongitude) throws IOException {
        return remoteDataSource.getNearbyPlaces(decLatitude, decLongitude);
    }

    public void useSimilarPictureCoordinates(ImageCoordinates imageCoordinates, int uploadItemIndex) {
        remoteDataSource.useSimilarPictureCoordinates(imageCoordinates, uploadItemIndex);
    }
}
