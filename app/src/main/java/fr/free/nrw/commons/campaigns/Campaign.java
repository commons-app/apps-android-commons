package fr.free.nrw.commons.campaigns;

import com.google.gson.annotations.SerializedName;

/**
 * A data class to hold a campaign
 */
public class Campaign {

    @SerializedName("title") private String title;
    @SerializedName("description") private String description;
    @SerializedName("startDate") private String startDate;
    @SerializedName("endDate") private String endDate;
    @SerializedName("link") private String link;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
