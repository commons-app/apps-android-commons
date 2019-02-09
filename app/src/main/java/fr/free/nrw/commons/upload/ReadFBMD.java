package fr.free.nrw.commons.upload;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.iptc.IPTC;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Single;
import timber.log.Timber;

@Singleton
public class ReadFBMD {

    @Inject
    public ReadFBMD(){

    }
    public Single<Integer> processMetadata(String path) throws IOException {
        Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(path);
        Metadata metadata = metadataMap.get(MetadataType.IPTC);
        byte[] dataInBytes = new byte[0];
        try {
            dataInBytes = ((IPTC) metadata).getDataSets().get("SpecialInstructions").get(0).getData();
        } catch (NullPointerException e) {
            return Single.just(ImageUtils.IMAGE_OK);
        }
        /**
        * The byte array so obtained is used is tested to contain FBMD data
        * Note: Any image downloaded from Facebook contains the ASCII code of the letters 'FBMD' in this bytecode extracted from it's IPTC metadata
        * */
            if (dataInBytes[0] == 70 && dataInBytes[1] == 66 && dataInBytes[2] == 77 && dataInBytes[3] == 68) {
                Timber.d("Contains FBMD");
                return Single.just(ImageUtils.FILE_FBMD);
            }
        return Single.just(ImageUtils.IMAGE_OK);
    }
}

