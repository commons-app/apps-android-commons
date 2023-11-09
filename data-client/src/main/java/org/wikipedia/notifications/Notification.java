package org.wikipedia.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.util.DateUtil;

import java.text.ParseException;
import java.util.Date;
import timber.log.Timber;

public class Notification {
    @Nullable private String wiki;
     private long id;
    @Nullable private String type;
    @Nullable private String category;

    @Nullable private Title title;
    @Nullable private Timestamp timestamp;
    @SerializedName("*") @Nullable private Contents contents;

    @NonNull public String wiki() {
        return StringUtils.defaultString(wiki);
    }

    public long id() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public long key() {
        return id + wiki().hashCode();
    }

    @NonNull public String type() {
        return StringUtils.defaultString(type);
    }

    @Nullable public Title title() {
        return title;
    }

    @Nullable public Contents getContents() {
        return contents;
    }

    public void setContents(@Nullable final Contents contents) {
        this.contents = contents;
    }

    @NonNull public Date getTimestamp() {
        return timestamp != null ? timestamp.date() : new Date();
    }

    public void setTimestamp(@Nullable final Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @NonNull String getUtcIso8601() {
        return StringUtils.defaultString(timestamp != null ? timestamp.utciso8601 : null);
    }

    public boolean isFromWikidata() {
        return wiki().equals("wikidatawiki");
    }

    @Override public String toString() {
        return Long.toString(id);
    }

    public static class Title {
        @Nullable private String full;
        @Nullable private String text;

        @NonNull public String text() {
            return StringUtils.defaultString(text);
        }

        @NonNull public String full() {
            return StringUtils.defaultString(full);
        }
    }

    public static class Timestamp {
        @Nullable private String utciso8601;

        public void setUtciso8601(@Nullable final String utciso8601) {
            this.utciso8601 = utciso8601;
        }

        public Date date() {
            try {
                return DateUtil.iso8601DateParse(utciso8601);
            } catch (ParseException e) {
                Timber.e(e);
                return new Date();
            }
        }
    }

    public static class Link {
        @Nullable private String url;
        @Nullable private String label;
        @Nullable private String tooltip;
        @Nullable private String description;
        @Nullable private String icon;

        @NonNull public String getUrl() {
            return StringUtils.defaultString(url);
        }

        public void setUrl(@Nullable final String url) {
            this.url = url;
        }

        @NonNull public String getTooltip() {
            return StringUtils.defaultString(tooltip);
        }

        @NonNull public String getLabel() {
            return StringUtils.defaultString(label);
        }

        @NonNull public String getIcon() {
            return StringUtils.defaultString(icon);
        }
    }

    public static class Links {
        @Nullable private JsonElement primary;
        private Link primaryLink;

        public void setPrimary(@Nullable final JsonElement primary) {
            this.primary = primary;
        }

        @Nullable public Link getPrimary() {
            if (primary == null) {
                return null;
            }
            if (primaryLink == null && primary instanceof JsonObject) {
                primaryLink = GsonUtil.getDefaultGson().fromJson(primary, Link.class);
            }
            return primaryLink;
        }

    }

    public static class Contents {
        @Nullable private String header;
        @Nullable private String compactHeader;
        @Nullable private String body;
        @Nullable private String icon;
        @Nullable private Links links;

        @NonNull public String getHeader() {
            return StringUtils.defaultString(header);
        }

        @NonNull public String getCompactHeader() {
            return StringUtils.defaultString(compactHeader);
        }

        public void setCompactHeader(@Nullable final String compactHeader) {
            this.compactHeader = compactHeader;
        }

        @NonNull public String getBody() {
            return StringUtils.defaultString(body);
        }

        @Nullable public Links getLinks() {
            return links;
        }

        public void setLinks(@Nullable final Links links) {
            this.links = links;
        }
    }

}
