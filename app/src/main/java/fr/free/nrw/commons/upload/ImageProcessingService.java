package fr.free.nrw.commons.upload;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.BitmapRegionDecoderWrapper;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.ImageUtilsWrapper;
import fr.free.nrw.commons.utils.StringUtils;
import io.reactivex.Single;

@Singleton
public class ImageProcessingService {
    private final FileUtilsWrapper fileUtilsWrapper;
    private final BitmapRegionDecoderWrapper bitmapRegionDecoderWrapper;
    private final ImageUtilsWrapper imageUtilsWrapper;
    private final MediaWikiApi mwApi;

    @Inject
    public ImageProcessingService(FileUtilsWrapper fileUtilsWrapper,
                                  BitmapRegionDecoderWrapper bitmapRegionDecoderWrapper,
                                  ImageUtilsWrapper imageUtilsWrapper,
                                  MediaWikiApi mwApi) {
        this.fileUtilsWrapper = fileUtilsWrapper;
        this.bitmapRegionDecoderWrapper = bitmapRegionDecoderWrapper;

        this.imageUtilsWrapper = imageUtilsWrapper;
        this.mwApi = mwApi;
    }

    public Single<Integer> checkImageQuality(String filePath) {
        return checkImageQuality(null, null, filePath);
    }

    public Single<Integer> checkImageQuality(String wikidataEntityIdPref, String wikidataItemLocation, String filePath) {
        return Single.zip(
                checkDuplicateImage(filePath),
                checkImageGeoLocation(wikidataEntityIdPref, wikidataItemLocation, filePath),
                checkDarkImage(filePath), //Returns IMAGE_DARK or IMAGE_OK
                (dupe, wrongGeo, dark) -> dupe | wrongGeo | dark);
    }

    private Single<Integer> checkDuplicateImage(String filePath) {
        return Single.fromCallable(() ->
                fileUtilsWrapper.getFileInputStream(filePath))
                .map(fileUtilsWrapper::getSHA1)
                .map(mwApi::existingFile)
                .map(b -> b ? ImageUtils.IMAGE_DUPLICATE : ImageUtils.IMAGE_OK);
    }

    private Single<Integer> checkDarkImage(String filePath) {
        return Single.fromCallable(() ->
                fileUtilsWrapper.getFileInputStream(filePath))
                .map(file -> bitmapRegionDecoderWrapper.newInstance(file, false))
                .flatMap(imageUtilsWrapper::checkIfImageIsTooDark);
    }

    private Single<Integer> checkImageGeoLocation(String wikidataEntityId, String wikidataItemLocation, String filePath) {
        if (StringUtils.isNullOrWhiteSpace(wikidataEntityId)) {
            return Single.just(ImageUtils.IMAGE_OK);
        }
        return Single.fromCallable(() -> filePath)
                .map(fileUtilsWrapper::getGeolocationOfFile)
                .flatMap(geoLocation -> imageUtilsWrapper.checkImageGeolocationIsDifferent(geoLocation, wikidataItemLocation));
    }
}
