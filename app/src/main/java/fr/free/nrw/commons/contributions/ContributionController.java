package fr.free.nrw.commons.contributions;

import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.filepicker.DefaultCallback;
import fr.free.nrw.commons.filepicker.FilePicker;
import fr.free.nrw.commons.filepicker.FilePicker.ImageSource;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.nearby.models.Place;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
     * Initiate gallery picker
     */
    public void initiateGalleryPick(final Activity activity, final boolean allowMultipleUploads) {
        initiateGalleryUpload(activity, allowMultipleUploads);
    }

    /**
     * Initiate gallery picker with permission
     */
    public void initiateCustomGalleryPickWithPermission(final Activity activity) {
        setPickerConfiguration(activity,true);

        PermissionUtils.checkPermissionsAndPerformAction(activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            () -> FilePicker.openCustomSelector(activity, 0),
            R.string.storage_permission_title,
            R.string.write_storage_permission_rationale);
    }


    /**
     * Open chooser for gallery uploads
     */
    private void initiateGalleryUpload(final Activity activity, final boolean allowMultipleUploads) {
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
            public void onCanceled(final ImageSource source, final int type) {
                super.onCanceled(source, type);
                defaultKvStore.remove(PLACE_OBJECT);
            }

            @Override
            public void onImagePickerError(Exception e, FilePicker.ImageSource source, int type) {
                ViewUtil.showShortToast(activity, R.string.error_occurred_in_picking_images);
            }

            @Override
            public void onImagesPicked(@NonNull List<UploadableFile> imagesFiles, FilePicker.ImageSource source, int type) {
                Intent intent = handleImagesPicked(activity, imagesFiles);
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
        List<UploadableFile> imagesFiles) {
        Intent shareIntent = new Intent(context, UploadActivity.class);
        shareIntent.setAction(ACTION_INTERNAL_UPLOADS);
        shareIntent
            .putParcelableArrayListExtra(UploadActivity.EXTRA_FILES, new ArrayList<>(imagesFiles));
        Place place = defaultKvStore.getJson(PLACE_OBJECT, Place.class);

        if (place != null) {
            shareIntent.putExtra(PLACE_OBJECT, place);
        }

        return shareIntent;
    }

}
