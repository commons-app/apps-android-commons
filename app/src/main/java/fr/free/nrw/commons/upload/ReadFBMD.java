package fr.free.nrw.commons.upload;

import android.content.Context;
import android.net.Uri;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.iptc.IptcDirectory;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Single;

@Singleton
public class ReadFBMD {

    @Inject
    public ReadFBMD() {

    }

    public Single<Integer> processMetadata(Context context, Uri contentUri) throws IOException {
        Metadata readMetadata = null;
        try {
            readMetadata = ImageMetadataReader.readMetadata(context.getContentResolver().openInputStream(contentUri));
        } catch (ImageProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        IptcDirectory iptcDirectory = readMetadata != null ? readMetadata.getFirstDirectoryOfType(IptcDirectory.class) : null;
        if (iptcDirectory == null) {
            return Single.just(ImageUtils.IMAGE_OK);
        }
        /**
         * We parse through all the tags in the IPTC directory  if the tagname equals "Special Instructions".
         * And the description string starts with FBMD.
         * Then the source of image is facebook
         * */
        for (Tag tag : iptcDirectory.getTags()) {
            if (tag.getTagName().equals("Special Instructions") && tag.getDescription().substring(0, 4).equals("FBMD")) {
                return Single.just(ImageUtils.FILE_FBMD);
            }
        }
        return Single.just(ImageUtils.IMAGE_OK);
    }
}

