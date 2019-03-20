package fr.free.nrw.commons.upload;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Single;
import timber.log.Timber;

/**
* We try to avoid copyright violations in commons app.
* For doing that we read EXIF data using the library metadata-reader
* If an image doesn't have any EXIF Directoris in it's metadata then the image is an
* internet download image(and not the one taken using phone's camera) */

@Singleton
public class EXIFReader {
    @Inject
    public EXIFReader() {
        //Empty
    }
    /**
    * The method takes in path of the image and reads metadata using the library metadata-extractor
     * And the checks for the presence of EXIF Directories in metadata object
     * */

    public static Single<Integer> processMetadata(String path) {
        Metadata readMetadata = null;
        try {
            readMetadata = ImageMetadataReader.readMetadata(new File(path));
        } catch (ImageProcessingException e) {
            Timber.d(e.toString());
        } catch (IOException e) {
            Timber.d(e.toString());
        }
        if (readMetadata != null) {
            for (Directory directory : readMetadata.getDirectories()) {
                // In case of internet downloaded image these three fields are not present
                if (directory.getName().equals("Exif IFD0") //Contains information about the device capturing the photo
                        || directory.getName().equals("Exif SubIFD") //contains information like date, time and pixels of the image
                        || directory.getName().equals("Exif Thumbnail")) //contains information about image thumbnail like compression and reolution
                {
                    Timber.d(directory.getName() + " Contains metadata");
                    return Single.just(ImageUtils.IMAGE_OK);
                }
            }
        }
        return Single.just(ImageUtils.FILE_NO_EXIF);
    }

}

