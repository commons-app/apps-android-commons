package fr.free.nrw.commons;

import android.net.Uri;
import android.support.annotation.NonNull;

public class PageTitle {
    private final String namespace;
    private final String titleKey;

    /**
     * Construct from a namespace-prefixed page name.
     * @param prefixedText namespace-prefixed page name
     */
    public PageTitle(@NonNull String prefixedText) {
        String[] segments = prefixedText.trim().replace(" ", "_").split(":", 2);

        // canonicalize and capitalize page title as done by MediaWiki
        if (segments.length == 2) {
            // TODO: canonicalize and capitalize namespace as well
            // see https://www.mediawiki.org/wiki/Manual:Title.php#Canonical_forms
            namespace = segments[0];
            titleKey = Utils.capitalize(segments[1]);
        } else {
            namespace = "";
            titleKey = Utils.capitalize(segments[0]);
        }
    }

    /**
     * Get the canonicalized title for displaying (such as "File:My example.jpg").
     *
     * @return canonical title
     */
    @NonNull
    public String getPrefixedText() {
        if (namespace.isEmpty()) {
            return titleKey;
        } else {
            return namespace + ":" + titleKey;
        }
    }

    /**
     * Get the canonical title for DB and URLs (such as "File:My_example.jpg").
     *
     * @return canonical title
     */
    @NonNull
    public String getDisplayText() {
        return getPrefixedText().replace("_", " ");
    }

    /**
     * Convert to a URI
     * (such as "https://commons.wikimedia.org/wiki/File:My_example.jpg").
     *
     * @return URI
     */
    @NonNull
    public Uri getCanonicalUri() {
        String uriStr = BuildConfig.HOME_URL + Uri.encode(getPrefixedText(), ":/");
        return Uri.parse(uriStr);
    }


    /**
     * Convert to a mobile URI
     * (such as "https://commons.m.wikimedia.org/wiki/File:My_example.jpg").
     *
     * @return URI
     */
    @NonNull
    public Uri getMobileUri() {
        String uriStr = BuildConfig.MOBILE_HOME_URL + Uri.encode(getPrefixedText(), ":/");
        return Uri.parse(uriStr);
    }

    /**
     * Get the canonical title without namespace.
     * @return title
     */
    @NonNull
    public String getText() {
        return titleKey;
    }

    @Override
    public String toString() {
        return getPrefixedText();
    }
}
