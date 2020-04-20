package org.wikipedia.dataclient.restbase.page;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.json.annotations.Required;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.PageTitle;

/**
 * A standardized page summary object constructed by RESTBase, used for link previews and as the
 * base class for various feed content (see the FeedPageSummary class).
 *
 * N.B.: The "title" field here sent by RESTBase is the *normalized* page title.  However, in the
 * FeedPageSummary subclass, "title" becomes the un-normalized, raw title, and the normalized title
 * is sent as "normalizedtitle".
 */
@SuppressWarnings("unused")
public class RbPageSummary implements PageSummary {
    @Nullable private String type;
    @SuppressWarnings("NullableProblems") @Required @NonNull private String title;
    @Nullable private String normalizedtitle;
    @SuppressWarnings("NullableProblems") @NonNull private String displaytitle;
    @Nullable private NamespaceContainer namespace;
    @Nullable private String extract;
    @Nullable @SerializedName("extract_html") private String extractHtml;
    @Nullable private String description;
    @Nullable private Thumbnail thumbnail;
    @Nullable @SerializedName("originalimage") private Thumbnail originalImage;
    @Nullable private String lang;
    private int pageid;
    @Nullable @SerializedName("wikibase_item") private String wikiBaseItem;

    @Override @NonNull
    public String getTitle() {
        return title;
    }

    @Override @NonNull
    public String getDisplayTitle() {
        return displaytitle;
    }

    @Override @NonNull
    public String getConvertedTitle() {
        return title;
    }

    @Override @NonNull
    public Namespace getNamespace() {
        return namespace == null ? Namespace.MAIN : Namespace.of(namespace.id());
    }

    @Override @NonNull
    public String getType() {
        return TextUtils.isEmpty(type) ? TYPE_STANDARD : type;
    }

    @Override @Nullable
    public String getExtract() {
        return extract;
    }

    @Override @Nullable
    public String getExtractHtml() {
        return extractHtml;
    }

    @Override @Nullable
    public String getThumbnailUrl() {
        return thumbnail == null ? null : thumbnail.getUrl();
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getNormalizedTitle() {
        return normalizedtitle == null ? title : normalizedtitle;
    }

    @Nullable
    public String getOriginalImageUrl() {
        return originalImage == null ? null : originalImage.getUrl();
    }

    @Nullable
    public String getWikiBaseItem() {
        return wikiBaseItem;
    }

    @NonNull
    public PageTitle getPageTitle(@NonNull WikiSite wiki) {
        return new PageTitle(getTitle(), wiki, getThumbnailUrl(), getDescription());
    }

    public int getPageId() {
        return pageid;
    }

    public String getLang() {
        return lang;
    }

    /**
     * For the thumbnail URL of the page
     */
    private static class Thumbnail {
        @SuppressWarnings("unused") private String source;

        public String getUrl() {
            return source;
        }
    }

    private static class NamespaceContainer {
        @SuppressWarnings("unused") private int id;
        @SuppressWarnings("unused") @Nullable private String text;

        public int id() {
            return id;
        }
    }

    @Override public String toString() {
        return getTitle();
    }
}
