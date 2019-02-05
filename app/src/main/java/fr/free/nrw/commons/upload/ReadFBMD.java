package fr.free.nrw.commons.upload;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.iptc.IPTC;

import java.io.IOException;
import java.util.Map;

import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Single;
import timber.log.Timber;

public class ReadFBMD {
    public static Single<Integer> processMetadata(String path) throws IOException {
        Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(path);
        Metadata metadata = metadataMap.get(MetadataType.IPTC);
        byte[] b = new byte[0];
        try {
            b = ((IPTC) metadata).getDataSets().get("SpecialInstructions").get(0).getData();
        } catch (NullPointerException e) {
            return Single.just(ImageUtils.IMAGE_OK);
        }
        for (int i = 0; i < b.length; i++) {
            if (b[i] == 70 && b[i + 1] == 66 && b[i + 2] == 77 && b[i + 3] == 68) {
                Timber.d("Contains FBMD");
                return Single.just(ImageUtils.FILE_FBMD);
            }
        }
        return Single.just(ImageUtils.IMAGE_OK);
    }
}

