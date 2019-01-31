package fr.free.nrw.commons.upload;

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

import retrofit2.http.Url;

public class ReadFBMD {
    Url url;
    ReadFBMD(Url url){
        this.url=url;
    }
    public void processMetadata() throws IOException {
        Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata((File) url);
        IPTC iptc = (IPTC)metadataMap.get(MetadataType.IPTC);

        if(iptc != null) {
            Iterator<MetadataEntry> iterator = (Iterator<MetadataEntry>) iptc.getDataSets();

            while(iterator.hasNext()) {
                MetadataEntry item = iterator.next();
                printMetadata(item, "", "     ");
            }
        }
    }
    private void printMetadata(MetadataEntry entry, String indent, String increment) {
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

