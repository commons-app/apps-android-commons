package fr.free.nrw.commons.depictions.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Model class for object obtained while parsing depiction response
 * this class contains all the details of for the media object
 */

public class Search {

    @SerializedName("ns")
    @Expose
    private Integer ns;
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("pageid")
    @Expose
    private Integer pageid;
    @SerializedName("size")
    @Expose
    private Integer size;
    @SerializedName("wordcount")
    @Expose
    private Integer wordcount;
    @SerializedName("snippet")
    @Expose
    private String snippet;
    @SerializedName("timestamp")
    @Expose
    private String timestamp;

    /**
     * No args constructor for use in serialization
     *
     */
    public Search() {
    }

    /**
     *
     * @param timestamp
     * @param title
     * @param ns
     * @param snippet
     * @param wordcount
     * @param size
     * @param pageid
     */
    public Search(Integer ns, String title, Integer pageid, Integer size, Integer wordcount, String snippet, String timestamp) {
        super();
        this.ns = ns;
        this.title = title;
        this.pageid = pageid;
        this.size = size;
        this.wordcount = wordcount;
        this.snippet = snippet;
        this.timestamp = timestamp;
    }

    /**
     * returns ns int from Search object
     */
    public Integer getNs() {
        return ns;
    }

    public void setNs(Integer ns) {
        this.ns = ns;
    }

    /**
     * returns title string from Search object
     */
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * returns pageid int from Search object
     */
    public Integer getPageid() {
        return pageid;
    }

    public void setPageid(Integer pageid) {
        this.pageid = pageid;
    }

    /**
     * returns size int from Search object
     */
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * returns wordcount int from Search object
     */
    public Integer getWordcount() {
        return wordcount;
    }

    public void setWordcount(Integer wordcount) {
        this.wordcount = wordcount;
    }

    /**
     * returns snippet String from Search object
     */
    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    /**
     * returns ns int from Search object
     */
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}
