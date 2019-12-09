package fr.free.nrw.commons.contributions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.filepicker.DefaultCallback;
import fr.free.nrw.commons.filepicker.FilePicker;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;

import static fr.free.nrw.commons.contributions.Contribution.SOURCE_CAMERA;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_GALLERY;
import static fr.free.nrw.commons.upload.UploadService.EXTRA_FILES;
import static fr.free.nrw.commons.upload.UploadService.EXTRA_SOURCE;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

@Singleton
public class ContributionController {

    public static final String ACTION_INTERNAL_UPLOADS = "internalImageUploads";

    private final JsonKvStore defaultKvStore;

    @Inject
    public ContributionController(@Named("default_preferences") JsonKvStore defaultKvStore) {
        this.defaultKvStore = defaultKvStore;
    }

    /**
     * Check for permissions and initiate camera click
     */
    public void initiateCameraPick(Activity activity) {
        boolean useExtStorage = defaultKvStore.getBoolean("useExternalStorage", true);
        if (!useExtStorage) {
            initiateCameraUpload(activity);
            return;
        }

        PermissionUtils.checkPermissionsAndPerformAction(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                () -> initiateCameraUpload(activity),
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale);
    }

    /**
     * Check for permissions and initiate gallery picker
     */
    public void initiateGalleryPick(Activity activity, boolean allowMultipleUploads) {
        PermissionUtils.checkPermissionsAndPerformAction(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                () -> initiateGalleryUpload(activity, allowMultipleUploads),
                R.string.storage_permission_title,
                R.string.read_storage_permission_rationale);
    }

    /**
     * Open chooser for gallery uploads
     */
    private void initiateGalleryUpload(Activity activity, boolean allowMultipleUploads) {
        setPickerConfiguration(activity, allowMultipleUploads);
        FilePicker.openGallery(activity, 0);
    }

    /**
     * Sets configuration for file picker
     */
    private void setPickerConfiguration(Activity activity,
                                        boolean allowMultipleUploads) {
        boolean copyToExternalStorage = defaultKvStore.getBoolean("useExternalStorage", true);
        FilePicker.configuration(activity)
                .setCopyTakenPhotosToPublicGalleryAppFolder(copyToExternalStorage)
                .setAllowMultiplePickInGallery(allowMultipleUploads);
    }

    /**
     * Initiate camera upload by opening camera
     */
    private void initiateCameraUpload(Activity activity) {
        setPickerConfiguration(activity, false);
        FilePicker.openCameraForImage(activity, 0);
    }

    /**
     * Attaches callback for file picker.
     */
    public void handleActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        FilePicker.handleActivityResult(requestCode, resultCode, data, activity, new DefaultCallback() {
            @Override
            public void onImagePickerError(Exception e, FilePicker.ImageSource source, int type) {
                ViewUtil.showShortToast(activity, R.string.error_occurred_in_picking_images);
            }

            @Override
            public void onImagesPicked(@NonNull List<UploadableFile> imagesFiles, FilePicker.ImageSource source, int type) {
                Intent intent = handleImagesPicked(activity, imagesFiles, getSourceFromImageSource(source));
                activity.startActivity(intent);
            }
        });
    }

    public List<UploadableFile> handleExternalImagesPicked(Activity activity,
                                                           Intent data) {
        return FilePicker.handleExternalImagesPicked(data, activity);
    }

    /**
     * Returns intent to be passed to upload activity
     * Attaches place object for nearby uploads
     */
    private Intent handleImagesPicked(Context context,
                                      List<UploadableFile> imagesFiles,
                                      String source) {
        Intent shareIntent = new Intent(context, UploadActivity.class);
        shareIntent.setAction(ACTION_INTERNAL_UPLOADS);
        shareIntent.putExtra(EXTRA_SOURCE, source);
        shareIntent.putParcelableArrayListExtra(EXTRA_FILES, new ArrayList<>(imagesFiles));
        Place place = defaultKvStore.getJson(PLACE_OBJECT, Place.class);
        if (place != null) {
            shareIntent.putExtra(PLACE_OBJECT, place);
        }

        return shareIntent;
    }

    /**
     * Get image upload source
     */
    private String getSourceFromImageSource(FilePicker.ImageSource source) {
        if (source.equals(FilePicker.ImageSource.CAMERA_IMAGE)) {
            return SOURCE_CAMERA;
        }
        return SOURCE_GALLERY;
    }
}
