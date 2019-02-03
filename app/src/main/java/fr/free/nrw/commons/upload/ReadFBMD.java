package fr.free.nrw.commons.upload;

import android.net.Uri;

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

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import retrofit2.http.Url;

public class ReadFBMD {
    public static Single<Integer> processMetadata(Uri uri) throws IOException {
        Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(Objects.requireNonNull(uri.getPath()));
        IPTC iptc = (IPTC)metadataMap.get(MetadataType.IPTC);

        if(iptc != null) {
            Iterator<MetadataEntry> iterator = (Iterator<MetadataEntry>) iptc.getDataSets();

            while(iterator.hasNext()) {
                MetadataEntry item = iterator.next();
                printMetadata(item, "", "     ");
            }
        } return Single.just(1);
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

