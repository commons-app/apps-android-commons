package fr.free.nrw.commons.upload;

import android.content.Context;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Single;

@Singleton
public class ReadEXIF {
    @Inject
    public ReadEXIF(){

    }
    public Single<Integer> processMetadata(String path) throws IOException{
        Metadata readMetadata = null;
        try {
            readMetadata = ImageMetadataReader.readMetadata(new File(path));
        } catch (ImageProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Single.just(ImageUtils.IMAGE_OK);
    }

}

