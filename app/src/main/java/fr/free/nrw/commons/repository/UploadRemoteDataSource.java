package fr.free.nrw.commons.repository;

import fr.free.nrw.commons.upload.ImageCoordinates;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.NearbyPlaces;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.SimilarImageInterface;
import fr.free.nrw.commons.upload.UploadController;
import fr.free.nrw.commons.upload.UploadModel;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import fr.free.nrw.commons.upload.structure.depictions.DepictModel;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * This class would act as the data source for remote operations for UploadActivity
 */
@Singleton
public class UploadRemoteDataSource {

    private static final double NEARBY_RADIUS_IN_KILO_METERS = 0.1; //100 meters

    private UploadModel uploadModel;
    private UploadController uploadController;
    private CategoriesModel categoriesModel;
    private DepictModel depictModel;
    private NearbyPlaces nearbyPlaces;

    @Inject
    public UploadRemoteDataSource(UploadModel uploadModel, UploadController uploadController,
                                  CategoriesModel categoriesModel, NearbyPlaces nearbyPlaces, DepictModel depictModel) {
        this.uploadModel = uploadModel;
        this.uploadController = uploadController;
        this.categoriesModel = categoriesModel;
        this.nearbyPlaces = nearbyPlaces;
        this.depictModel = depictModel;
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
     * @return
     */
    public Single<Integer> getImageQuality(UploadItem uploadItem) {
        return uploadModel.getImageQuality(uploadItem);
    }

    /**
     * gets nearby places matching with upload item's GPS location
     *
     * @param latitude
     * @param longitude
     * @return
     */
    public Place getNearbyPlaces(double latitude, double longitude) {
        try {
            List<Place> fromWikidataQuery = nearbyPlaces.getFromWikidataQuery(new LatLng(latitude, longitude, 0.0f),
                    Locale.getDefault().getLanguage(),
                    NEARBY_RADIUS_IN_KILO_METERS);
            return fromWikidataQuery.size() > 0 ? fromWikidataQuery.get(0) : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * handles category selection/unselection
     * @param depictedItem
     */

    public void onDepictedItemClicked(DepictedItem depictedItem) {
        uploadModel.onDepictItemClicked(depictedItem);
    }

    /**
     * returns the list of selected depictions
     * @return
     */

    public List<DepictedItem> getSelectedDepictions() {
        return uploadModel.getSelectedDepictions();
    }

    /**
     * get all depictions
     */

    public Observable<DepictedItem> searchAllEntities(String query) {
        return depictModel.searchAllEntities(query);
    }

    public void useSimilarPictureCoordinates(ImageCoordinates imageCoordinates, int uploadItemIndex) {
        uploadModel.useSimilarPictureCoordinates(imageCoordinates, uploadItemIndex);
    }
}
