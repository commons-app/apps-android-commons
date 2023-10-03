package fr.free.nrw.commons.filepicker;

import static fr.free.nrw.commons.filepicker.PickedFiles.singleFileList;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import fr.free.nrw.commons.customselector.model.Image;
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class FilePicker implements Constants {

    private static final String KEY_PHOTO_URI = "photo_uri";
    private static final String KEY_VIDEO_URI = "video_uri";
    private static final String KEY_LAST_CAMERA_PHOTO = "last_photo";
    private static final String KEY_LAST_CAMERA_VIDEO = "last_video";
    private static final String KEY_TYPE = "type";

    /**
     * Returns the uri of the clicked image so that it can be put in MediaStore
     */
    private static Uri createCameraPictureFile(@NonNull Context context) throws IOException {
        File imagePath = PickedFiles.getCameraPicturesLocation(context);
        Uri uri = PickedFiles.getUriToFile(context, imagePath);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PHOTO_URI, uri.toString());
        editor.putString(KEY_LAST_CAMERA_PHOTO, imagePath.toString());
        editor.apply();
        return uri;
    }

    private static Intent createGalleryIntent(@NonNull Context context, int type,
                                              boolean openDocumentIntentPreferred) {
        // storing picked image type to shared preferences
        storeType(context, type);
        return plainGalleryPickerIntent(openDocumentIntentPreferred)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, configuration(context).allowsMultiplePickingInGallery());
    }

    /**
     * CreateCustomSectorIntent, creates intent for custom selector activity.
     * @param context
     * @param type
     * @return Custom selector intent
     */
    private static Intent createCustomSelectorIntent(@NonNull Context context, int type) {
        storeType(context, type);
        return new Intent(context, CustomSelectorActivity.class);
    }

    private static Intent createCameraForImageIntent(@NonNull Context context, int type) {
        storeType(context, type);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            Uri capturedImageUri = createCameraPictureFile(context);
            //We have to explicitly grant the write permission since Intent.setFlag works only on API Level >=20
            grantWritePermission(context, intent, capturedImageUri);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return intent;
    }

    private static void revokeWritePermission(@NonNull Context context, Uri uri) {
        context.revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static void grantWritePermission(@NonNull Context context, Intent intent, Uri uri) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    private static void storeType(@NonNull Context context, int type) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(KEY_TYPE, type).apply();
    }

    private static int restoreType(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_TYPE, 0);
    }

    /**
     * Opens default galery or a available galleries picker if there is no default
     *
     * @param type Custom type of your choice, which will be returned with the images
     */
    public static void openGallery(Activity activity, int type, boolean openDocumentIntentPreferred) {
        Intent intent = createGalleryIntent(activity, type, openDocumentIntentPreferred);
        activity.startActivityForResult(intent, RequestCodes.PICK_PICTURE_FROM_GALLERY);
    }

    /**
     * Opens Custom Selector
     */
    public static void openCustomSelector(Activity activity, int type) {
        Intent intent = createCustomSelectorIntent(activity, type);
        activity.startActivityForResult(intent, RequestCodes.PICK_PICTURE_FROM_CUSTOM_SELECTOR);
    }

    /**
     * Opens the camera app to pick image clicked by user 
     */
    public static void openCameraForImage(Activity activity, int type) {
        Intent intent = createCameraForImageIntent(activity, type);
        activity.startActivityForResult(intent, RequestCodes.TAKE_PICTURE);
    }

    @Nullable
    private static UploadableFile takenCameraPicture(Context context) throws URISyntaxException {
        String lastCameraPhoto = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_LAST_CAMERA_PHOTO, null);
        if (lastCameraPhoto != null) {
            return new UploadableFile(new File(lastCameraPhoto));
        } else {
            return null;
        }
    }

    @Nullable
    private static UploadableFile takenCameraVideo(Context context) throws URISyntaxException {
        String lastCameraPhoto = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_LAST_CAMERA_VIDEO, null);
        if (lastCameraPhoto != null) {
            return new UploadableFile(new File(lastCameraPhoto));
        } else {
            return null;
        }
    }

    /**
     * Any activity can use this method to attach their callback to the file picker
     */
    public static void handleActivityResult(int requestCode, int resultCode, Intent data, Activity activity, @NonNull FilePicker.Callbacks callbacks) {
        boolean isHandledPickedFile = (requestCode & RequestCodes.FILE_PICKER_IMAGE_IDENTIFICATOR) > 0;
        if (isHandledPickedFile) {
            requestCode &= ~RequestCodes.SOURCE_CHOOSER;
            if (requestCode == RequestCodes.PICK_PICTURE_FROM_GALLERY ||
                    requestCode == RequestCodes.TAKE_PICTURE ||
                    requestCode == RequestCodes.CAPTURE_VIDEO ||
                    requestCode == RequestCodes.PICK_PICTURE_FROM_DOCUMENTS ||
                    requestCode == RequestCodes.PICK_PICTURE_FROM_CUSTOM_SELECTOR) {
                if (resultCode == Activity.RESULT_OK) {
                    if (requestCode == RequestCodes.PICK_PICTURE_FROM_DOCUMENTS && !isPhoto(data)) {
                        onPictureReturnedFromDocuments(data, activity, callbacks);
                    } else if (requestCode == RequestCodes.PICK_PICTURE_FROM_GALLERY && !isPhoto(data)) {
                        onPictureReturnedFromGallery(data, activity, callbacks);
                    } else if (requestCode == RequestCodes.PICK_PICTURE_FROM_CUSTOM_SELECTOR) {
                        onPictureReturnedFromCustomSelector(data, activity, callbacks);
                    } else if (requestCode == RequestCodes.TAKE_PICTURE) {
                        onPictureReturnedFromCamera(activity, callbacks);
                    } else if (requestCode == RequestCodes.CAPTURE_VIDEO) {
                        onVideoReturnedFromCamera(activity, callbacks);
                    } else if (isPhoto(data)) {
                        onPictureReturnedFromCamera(activity, callbacks);
                    } else {
                        onPictureReturnedFromDocuments(data, activity, callbacks);
                    }
                } else {
                    if (requestCode == RequestCodes.PICK_PICTURE_FROM_DOCUMENTS) {
                        callbacks.onCanceled(FilePicker.ImageSource.DOCUMENTS, restoreType(activity));
                    } else if (requestCode == RequestCodes.PICK_PICTURE_FROM_GALLERY) {
                        callbacks.onCanceled(FilePicker.ImageSource.GALLERY, restoreType(activity));
                    } else {
                        callbacks.onCanceled(FilePicker.ImageSource.CAMERA_IMAGE, restoreType(activity));
                    }
                }
            }
        }
    }

    public static List<UploadableFile> handleExternalImagesPicked(Intent data, Activity activity) {
        try {
            return getFilesFromGalleryPictures(data, activity);
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private static boolean isPhoto(Intent data) {
        return data == null || (data.getData() == null && data.getClipData() == null);
    }

    private static Intent plainGalleryPickerIntent(boolean openDocumentIntentPreferred) {
        /*
         * Asking for ACCESS_MEDIA_LOCATION at runtime solved the location-loss issue
         * in the custom selector in Contributions fragment.
         * Detailed discussion: https://github.com/commons-app/apps-android-commons/issues/5015
         *
         * This permission check, however, was insufficient to fix location-loss in
         * the regular selector in Contributions fragment and Nearby fragment,
         * especially on some devices running Android 13 that use the new Photo Picker by default.
         *
         * New Photo Picker: https://developer.android.com/training/data-storage/shared/photopicker
         *
         * The new Photo Picker introduced by Android redacts location tags from EXIF metadata.
         * Reported on the Google Issue Tracker: https://issuetracker.google.com/issues/243294058
         * Status: Won't fix (Intended behaviour)
         *
         * Switched intent from ACTION_GET_CONTENT to ACTION_OPEN_DOCUMENT (by default; can
         * be changed through the Setting page) as:
         *
         * ACTION_GET_CONTENT opens the 'best application' for choosing that kind of data
         * The best application is the new Photo Picker that redacts the location tags
         *
         * ACTION_OPEN_DOCUMENT, however,  displays the various DocumentsProvider instances
         * installed on the device, letting the user interactively navigate through them.
         *
         * So, this allows us to use the traditional file picker that does not redact location tags
         * from EXIF.
         *
         */
        Intent intent;
        if (openDocumentIntentPreferred) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.setType("image/*");
        return intent;
    }

    private static void onPictureReturnedFromDocuments(Intent data, Activity activity, @NonNull FilePicker.Callbacks callbacks) {
        try {
            Uri photoPath = data.getData();
            UploadableFile photoFile = PickedFiles.pickedExistingPicture(activity, photoPath);
            callbacks.onImagesPicked(singleFileList(photoFile), FilePicker.ImageSource.DOCUMENTS, restoreType(activity));

            if (configuration(activity).shouldCopyPickedImagesToPublicGalleryAppFolder()) {
                PickedFiles.copyFilesInSeparateThread(activity, singleFileList(photoFile));
            }
        } catch (Exception e) {
            e.printStackTrace();
            callbacks.onImagePickerError(e, FilePicker.ImageSource.DOCUMENTS, restoreType(activity));
        }
    }

    /**
     * onPictureReturnedFromCustomSelector.
     * Retrieve and forward the images to upload wizard through callback.
     */
    private static void onPictureReturnedFromCustomSelector(Intent data, Activity activity, @NonNull FilePicker.Callbacks callbacks) {
        try {
            List<UploadableFile> files = getFilesFromCustomSelector(data, activity);
            callbacks.onImagesPicked(files, ImageSource.CUSTOM_SELECTOR, restoreType(activity));
        } catch (Exception e) {
            e.printStackTrace();
            callbacks.onImagePickerError(e, ImageSource.CUSTOM_SELECTOR, restoreType(activity));
        }
    }

    /**
     * Get files from custom selector
     * Retrieve and process the selected images from the custom selector.
     */
    private static List<UploadableFile> getFilesFromCustomSelector(Intent data, Activity activity) throws  IOException, SecurityException {
        List<UploadableFile> files = new ArrayList<>();
        ArrayList<Image> images = data.getParcelableArrayListExtra("Images");
        for(Image image : images) {
            Uri uri = image.getUri();
            UploadableFile file = PickedFiles.pickedExistingPicture(activity, uri);
            files.add(file);
        }

        if (configuration(activity).shouldCopyPickedImagesToPublicGalleryAppFolder()) {
            PickedFiles.copyFilesInSeparateThread(activity, files);
        }

        return files;
    }

    private static void onPictureReturnedFromGallery(Intent data, Activity activity, @NonNull FilePicker.Callbacks callbacks) {
        try {
            List<UploadableFile> files = getFilesFromGalleryPictures(data, activity);
            callbacks.onImagesPicked(files, FilePicker.ImageSource.GALLERY, restoreType(activity));
        } catch (Exception e) {
            e.printStackTrace();
            callbacks.onImagePickerError(e, FilePicker.ImageSource.GALLERY, restoreType(activity));
        }
    }

    private static List<UploadableFile> getFilesFromGalleryPictures(Intent data, Activity activity) throws IOException, SecurityException {
        List<UploadableFile> files = new ArrayList<>();
        ClipData clipData = data.getClipData();
        if (clipData == null) {
            Uri uri = data.getData();
            UploadableFile file = PickedFiles.pickedExistingPicture(activity, uri);
            files.add(file);
        } else {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                UploadableFile file = PickedFiles.pickedExistingPicture(activity, uri);
                files.add(file);
            }
        }

        if (configuration(activity).shouldCopyPickedImagesToPublicGalleryAppFolder()) {
            PickedFiles.copyFilesInSeparateThread(activity, files);
        }

        return files;
    }

    private static void onPictureReturnedFromCamera(Activity activity, @NonNull FilePicker.Callbacks callbacks) {
        try {
            String lastImageUri = PreferenceManager.getDefaultSharedPreferences(activity).getString(KEY_PHOTO_URI, null);
            if (!TextUtils.isEmpty(lastImageUri)) {
                revokeWritePermission(activity, Uri.parse(lastImageUri));
            }

            UploadableFile photoFile = FilePicker.takenCameraPicture(activity);
            List<UploadableFile> files = new ArrayList<>();
            files.add(photoFile);

            if (photoFile == null) {
                Exception e = new IllegalStateException("Unable to get the picture returned from camera");
                callbacks.onImagePickerError(e, FilePicker.ImageSource.CAMERA_IMAGE, restoreType(activity));
            } else {
                if (configuration(activity).shouldCopyTakenPhotosToPublicGalleryAppFolder()) {
                    PickedFiles.copyFilesInSeparateThread(activity, singleFileList(photoFile));
                }

                callbacks.onImagesPicked(files, FilePicker.ImageSource.CAMERA_IMAGE, restoreType(activity));
            }

            PreferenceManager.getDefaultSharedPreferences(activity)
                    .edit()
                    .remove(KEY_LAST_CAMERA_PHOTO)
                    .remove(KEY_PHOTO_URI)
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
            callbacks.onImagePickerError(e, FilePicker.ImageSource.CAMERA_IMAGE, restoreType(activity));
        }
    }

    private static void onVideoReturnedFromCamera(Activity activity, @NonNull FilePicker.Callbacks callbacks) {
        try {
            String lastVideoUri = PreferenceManager.getDefaultSharedPreferences(activity).getString(KEY_VIDEO_URI, null);
            if (!TextUtils.isEmpty(lastVideoUri)) {
                revokeWritePermission(activity, Uri.parse(lastVideoUri));
            }

            UploadableFile photoFile = FilePicker.takenCameraVideo(activity);
            List<UploadableFile> files = new ArrayList<>();
            files.add(photoFile);

            if (photoFile == null) {
                Exception e = new IllegalStateException("Unable to get the video returned from camera");
                callbacks.onImagePickerError(e, FilePicker.ImageSource.CAMERA_VIDEO, restoreType(activity));
            } else {
                if (configuration(activity).shouldCopyTakenPhotosToPublicGalleryAppFolder()) {
                    PickedFiles.copyFilesInSeparateThread(activity, singleFileList(photoFile));
                }

                callbacks.onImagesPicked(files, FilePicker.ImageSource.CAMERA_VIDEO, restoreType(activity));
            }

            PreferenceManager.getDefaultSharedPreferences(activity)
                    .edit()
                    .remove(KEY_LAST_CAMERA_VIDEO)
                    .remove(KEY_VIDEO_URI)
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
            callbacks.onImagePickerError(e, FilePicker.ImageSource.CAMERA_VIDEO, restoreType(activity));
        }
    }

    public static FilePickerConfiguration configuration(@NonNull Context context) {
        return new FilePickerConfiguration(context);
    }


    public enum ImageSource {
        GALLERY, DOCUMENTS, CAMERA_IMAGE, CAMERA_VIDEO, CUSTOM_SELECTOR
    }

    public interface Callbacks {
        void onImagePickerError(Exception e, FilePicker.ImageSource source, int type);

        void onImagesPicked(@NonNull List<UploadableFile> imageFiles, FilePicker.ImageSource source, int type);

        void onCanceled(FilePicker.ImageSource source, int type);
    }
}