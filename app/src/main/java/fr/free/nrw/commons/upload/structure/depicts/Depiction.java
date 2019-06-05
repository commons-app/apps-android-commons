package fr.free.nrw.commons.upload.structure.depicts;

import android.net.Uri;

import java.util.Date;

public class Depiction {
    private Uri contentUri;
    private String name;
    private Date lastUsed;
    private int timesUsed;

    public Depiction() {
    }

    public Depiction(Uri contentUri, String name, Date lastUsed, int timesUsed) {
        this.contentUri = contentUri;
        this.name = name;
        this.lastUsed = lastUsed;
        this.timesUsed = timesUsed;
    }

    public Uri getContentUri() {
        return contentUri;
    }

    public void setContentUri(Uri contentUri) {
        this.contentUri = contentUri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }

    public int getTimesUsed() {
        return timesUsed;
    }

    public void setTimesUsed(int timesUsed) {
        this.timesUsed = timesUsed;
    }

    public void incTimesUsed() {
        timesUsed++;
        touch();
    }

    private void touch() {
        lastUsed = new Date();
    }
}
