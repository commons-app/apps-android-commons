package fr.free.nrw.commons.repository;

import androidx.annotation.Nullable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.upload.UploadModel;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;

/**
 * The Local Data Source for UploadRepository, fetches and returns data from local db/shared prefernces
 */

@Singleton
public class UploadLocalDataSource {

    private final UploadModel uploadModel;
    private JsonKvStore defaultKVStore;

    @Inject
    public UploadLocalDataSource(
            @Named("default_preferences") JsonKvStore defaultKVStore,
            UploadModel uploadModel) {
        this.defaultKVStore = defaultKVStore;
        this.uploadModel = uploadModel;
    }


    /**
     * Fetches and returns the string list of valid licenses
     *
     * @return
     */
    public List<String> getLicenses() {
        return uploadModel.getLicenses();
    }

    /**
     * Returns the number of Upload Items
     *
     * @return
     */
    public int getCount() {
        return uploadModel.getCount();
    }

    /**
     * Fetches and return the selected license for the current upload
     *
     * @return
     */
    public String getSelectedLicense() {
        return uploadModel.getSelectedLicense();
    }

    /**
     * Set selected license for the current upload
     *
     * @param licenseName
     */
    public void setSelectedLicense(String licenseName) {
        uploadModel.setSelectedLicense(licenseName);
    }

    /**
     * Updates the current upload item
     *
     * @param index
     * @param uploadItem
     */
    public void updateUploadItem(int index, UploadItem uploadItem) {
        uploadModel.updateUploadItem(index, uploadItem);
    }

    /**
     * upload is halted, cleanup the acquired resources
     */
    public void cleanUp() {
        uploadModel.cleanUp();
    }

    /**
     * Deletes the upload item at the current index
     *
     * @param filePath
     */
    public void deletePicture(String filePath) {
        uploadModel.deletePicture(filePath);
    }

    /**
     * Fethces and returns the previous upload item, if any, returns null otherwise
     *
     * @param index
     * @return
     */
    @Nullable
    public UploadItem getPreviousUploadItem(int index) {
        if (index - 1 >= 0) {
            return uploadModel.getItems().get(index - 1);
        }
        return null; //There is no previous item to copy details
    }

    /**
     * saves boolean value in default store
     *
     * @param key
     * @param value
     */
    public void saveValue(String key, boolean value) {
        defaultKVStore.putBoolean(key, value);
    }

    /**
     * saves string value in default store
     *
     * @param key
     * @param value
     */
    public void saveValue(String key, String value) {
        defaultKVStore.putString(key, value);
    }

    /**
     * Fetches and returns string value from the default store
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public String getValue(String key, String defaultValue) {
        return defaultKVStore.getString(key, defaultValue);
    }

    /**
     * Fetches and returns boolean value from the default store
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public boolean getValue(String key, boolean defaultValue) {
        return defaultKVStore.getBoolean(key, defaultValue);
    }
}
