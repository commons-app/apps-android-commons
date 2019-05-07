package fr.free.nrw.commons.repository;

import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.upload.UploadModel;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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


    public List<String> getLicenses() {
        return uploadModel.getLicenses();
    }

    public String getFromDefaultKvStore(String key, String defaultValue) {
        return defaultKVStore.getString(key, defaultValue);
    }

    public void saveInDefaultKvStore(String key, String value) {
        defaultKVStore.putString(key, value);
    }

    public int getCount() {
        return uploadModel.getCount();
    }

    public String getSelectedLicense() {
        return uploadModel.getSelectedLicense();
    }

    public void setSelectedLicense(String licenseName) {
        uploadModel.setSelectedLicense(licenseName);
    }

    public void updateUploadItem(int index, UploadItem uploadItem) {
        uploadModel.updateUploadItem(index, uploadItem);
    }

    public void saveInDirectKvStore(String key, boolean value) {
        defaultKVStore.putBoolean(key, value);
    }

    public void cleanUp() {
        uploadModel.cleanUp();
    }

    public void deletePicture(String filePath) {
        uploadModel.deletePicture(filePath);
    }
}
