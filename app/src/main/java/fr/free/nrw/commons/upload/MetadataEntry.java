package fr.free.nrw.commons.upload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * MetadataEntry.java
 */
public class MetadataEntry {

    private String key;
    private String value;
    private boolean isMetadataEntryGroup;

    private Collection<MetadataEntry> entries = new ArrayList<MetadataEntry>();

    public MetadataEntry(String key, String value) {
        this(key, value, false);
    }

    public MetadataEntry(String key, String value, boolean isMetadataEntryGroup) {
        this.key = key;
        this.value = value;
        this.isMetadataEntryGroup = isMetadataEntryGroup;
    }

    public void addEntry(MetadataEntry entry) {
        entries.add(entry);
    }

    public void addEntries(Collection<MetadataEntry> newEntries) {
        entries.addAll(newEntries);
    }

    public String getKey() {
        return key;
    }

    public boolean isMetadataEntryGroup()  {
        return isMetadataEntryGroup;
    }

    public Collection<MetadataEntry> getMetadataEntries() {
        return Collections.unmodifiableCollection(entries);
    }

    public String getValue() {
        return value;
    }
}

