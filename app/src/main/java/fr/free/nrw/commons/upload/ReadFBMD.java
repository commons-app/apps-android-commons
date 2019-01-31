package fr.free.nrw.commons.upload;

import com.icafe4j.image.meta.Metadata;
//import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.iptc.IPTC;
import com.icafe4j.string.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import java.io.IOException;
import java.util.Map;

import retrofit2.http.Url;

public class ReadFBMD {
    /*public static void main(String[] args) throws IOException {
        Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(args[0]);
        IPTC iptc = (IPTC)metadataMap.get(MetadataType.IPTC);

        if(iptc != null) {
            Map<String, > = iptc.getDataSets();

            while(iterator.hasNext()) {
                MetadataEntry item = iterator.next();
                printMetadata(item, "", "     ");
            }
        }
    }
    private void printMetadata(MetadataEntry entry, String indent, String increment) {
        logger.info(indent + entry.getKey() (StringUtils.isNullOrEmpty(entry.getValue())? "" : ": " + entry.getValue()));
        if(entry.isMetadataEntryGroup()) {
            indent += increment;
            Collection<MetadataEntry> entries = entry.getMetadataEntries();
            for(MetadataEntry e : entries) {
                printMetadata(e, indent, increment);
            }
        }
    }*/

}

