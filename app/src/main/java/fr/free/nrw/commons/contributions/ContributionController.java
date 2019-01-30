package fr.free.nrw.commons.contributions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.filepicker.DefaultCallback;
import fr.free.nrw.commons.filepicker.FilePicker;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;

import static android.content.Intent.ACTION_SEND_MULTIPLE;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_CAMERA;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_EXTERNAL;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_GALLERY;
import static fr.free.nrw.commons.upload.UploadService.EXTRA_FILES;
import static fr.free.nrw.commons.upload.UploadService.EXTRA_SOURCE;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

@Singleton
public class ContributionController {

    private final BasicKvStore defaultKvStore;
    private final JsonKvStore directKvStore;

    @Inject
    public ContributionController(@Named("default_preferences") BasicKvStore defaultKvStore,
                                  @Named("direct_nearby_upload_prefs") JsonKvStore directKvStore) {
        this.defaultKvStore = defaultKvStore;
        this.directKvStore = directKvStore;
    }

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

    public void initiateGalleryPick(Activity activity) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            initiateGalleryUpload(activity);
        } else {
            PermissionUtils.checkPermissionsAndPerformAction(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    () -> initiateGalleryUpload(activity),
                    R.string.storage_permission_title,
                    R.string.read_storage_permission_rationale);
        }
    }

    private void initiateGalleryUpload(Activity activity) {
        setPickerConfiguration(activity);
        FilePicker.openChooserWithGallery(activity, "Choose Images to upload", 0);
    }

    private void setPickerConfiguration(Activity activity) {
        FilePicker.configuration(activity)
                .setAllowMultiplePickInGallery(true)
                .setCopyTakenPhotosToPublicGalleryAppFolder(true)
                .setCopyPickedImagesToPublicGalleryAppFolder(true);
    }

    private void initiateCameraUpload(Activity activity) {
        setPickerConfiguration(activity);
        FilePicker.openCameraForImage(activity, 0);
    }

    public void handleActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        FilePicker.handleActivityResult(requestCode, resultCode, data, activity, new DefaultCallback() {
            @Override
            public void onImagePickerError(Exception e, FilePicker.ImageSource source, int type) {
                ViewUtil.showShortToast(activity, "Error occurred while picking images");
            }

            @Override
            public void onImagesPicked(@NonNull List<File> imagesFiles, FilePicker.ImageSource source, int type) {
                Intent intent = handleImagesPicked(activity, imagesFiles, getSourceFromRequestCode(source));
                activity.startActivity(intent);
            }
        });
    }

    private Intent handleImagesPicked(Context context,
                                      List<File> imagesFiles,
                                      String source) {
        ArrayList<UploadableFile> uploadableFiles = new ArrayList<>();
        for (File file : imagesFiles) {
            uploadableFiles.add(new UploadableFile(file));
        }
        Intent shareIntent = new Intent(context, UploadActivity.class);
        shareIntent.setAction(ACTION_SEND_MULTIPLE);
        shareIntent.putExtra(EXTRA_SOURCE, source);
        shareIntent.putParcelableArrayListExtra(EXTRA_FILES, uploadableFiles);
        Place place = directKvStore.getJson(PLACE_OBJECT, Place.class);
        if (place != null) {
            shareIntent.putExtra(PLACE_OBJECT, place);
        }

        return shareIntent;
    }

    private String getSourceFromRequestCode(FilePicker.ImageSource source) {
        if (source.equals(FilePicker.ImageSource.CAMERA_IMAGE)) {
            return SOURCE_CAMERA;
        } else if (source.equals(FilePicker.ImageSource.GALLERY)) {
            return SOURCE_GALLERY;
        }
        return SOURCE_EXTERNAL;
    }
}
