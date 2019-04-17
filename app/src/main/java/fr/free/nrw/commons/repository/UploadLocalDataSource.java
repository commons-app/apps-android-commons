package fr.free.nrw.commons.repository;

import fr.free.nrw.commons.kvstore.BasicKvStore;
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
    private BasicKvStore defaultKvStore;
    private JsonKvStore directKvStore;

    @Inject
    public UploadLocalDataSource(@Named("default_preferences") BasicKvStore defaultKvStore,
            @Named("direct_nearby_upload_prefs") JsonKvStore directKvStore,
            UploadModel uploadModel) {
        this.defaultKvStore = defaultKvStore;
        this.directKvStore = directKvStore;
        this.uploadModel = uploadModel;
    }


    public List<String> getLicenses() {
        return uploadModel.getLicenses();
    }

    public String getFromDefaultKvStore(String key, String defaultValue) {
        return defaultKvStore.getString(key, defaultValue);
    }

    public void saveInDefaultKvStore(String key, String value) {
        defaultKvStore.putString(key, value);
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
        directKvStore.putBoolean(key, value);
    }
}
