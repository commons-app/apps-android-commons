package fr.free.nrw.commons.upload;

import android.net.Uri;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import com.icafe4j.image.meta.*;
import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataReader;
import com.icafe4j.image.meta.iptc.IPTC;
import com.icafe4j.string.StringUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.iptc.IPTC;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import retrofit2.http.Url;
import timber.log.Timber;

public class ReadFBMD {
    public static Single<Integer> processMetadata(String path) throws IOException {
        Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(path);
        Metadata metadata = metadataMap.get(MetadataType.IPTC);
        byte [] b = new byte[0];
        try {
            b=((IPTC) metadata).getDataSets().get("SpecialInstructions").get(0).getData();
        } catch (NullPointerException e){
            return Single.just(ImageUtils.IMAGE_OK);
        }
        for (int i=0;i<b.length;i++){
            if (b[i]==70 && b[i+1]==66 && b[i+2]==77 && b[i+3]==68){
                Timber.d("Contains FBMD");
                return Single.just(ImageUtils.FILE_FBMD);
            }
        }
        return Single.just(ImageUtils.IMAGE_OK);
    }
    private static void printMetadata(MetadataEntry entry, String indent, String increment) {
        //logger.info(indent + entry.getKey() (StringUtils.isNullOrEmpty(entry.getValue())? "" : ": " + entry.getValue()));
        if(entry.isMetadataEntryGroup()) {
            indent += increment;
            Collection<MetadataEntry> entries = entry.getMetadataEntries();
            for(MetadataEntry e : entries) {
                printMetadata(e, indent, increment);
            }
        }
    }

}

