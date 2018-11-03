package fr.free.nrw.commons;

import android.net.Uri;
import android.support.annotation.NonNull;

import fr.free.nrw.commons.utils.Utils;

public class PageTitle {
    private final String namespace;
    private final String key;

    /**
     * Construct from a namespace-prefixed page name.
     * @param prefixedText namespace-prefixed page name
     */
    public PageTitle(@NonNull String prefixedText) {
        // Split into namespace and key
        String[] segments = prefixedText
                .trim()
                .replace(" ", "_")
                .split(":", 2); // 2 section limit so filenames with colons aren't affected

        // canonicalize and capitalize page title as done by MediaWiki
        if (segments.length == 2) {
            // TODO: canonicalize namespace
            // see https://www.mediawiki.org/wiki/Manual:Title.php#Canonical_forms
            namespace = Utils.capitalize(segments[0]);
            key = Utils.capitalize(segments[1]);
        } else {
            namespace = "";
            key = Utils.capitalize(segments[0]);
        }
    }

    /**
     * Get the canonical title for DB and URLs
     * Example: "File:My_example.jpg"
     *
     * @return canonical title for DB and URLs (e.g. "File:My_example.jpg")
     */
    @NonNull
    public String getPrefixedTitle() {
        if (namespace.isEmpty()) {
            return key;
        } else {
            return namespace + ":" + key;
        }
    }

    /**
     * Get the canonical title for displaying
     * Example: "File:My example.jpg"
     *
     * @return canonical title for displaying (e.g. "File:My example.jpg")
     */
    @NonNull
    public String getDisplayTitle() {
        return getPrefixedTitle().replace("_", " ");
    }

    /**
     * Convert to a URI
     * Example: "https://commons.wikimedia.org/wiki/File:My_example.jpg").
     *
     * @return URI to the file on Commons
     */
    @NonNull
    public Uri getCommonsURI() {
        String uriStr = BuildConfig.HOME_URL + Uri.encode(getPrefixedTitle(), ":/");
        return Uri.parse(uriStr);
    }

    /**
     * Convert to a mobile URI
     * Example: "https://commons.m.wikimedia.org/wiki/File:My_example.jpg"
     *
     * @return URI to the file on mobile Commons
     */
    @NonNull
    public Uri getMobileCommonsUri() {
        String uriStr = BuildConfig.MOBILE_HOME_URL + Uri.encode(getPrefixedTitle(), ":/");
        return Uri.parse(uriStr);
    }

    /**
     * Get the canonical title without namespace for displaying
     * Example: "My example.jpg"
     * @return canonical title without namespace (e.g. "My example.jpg")
     */
    @NonNull
    public String getDisplayKey() {
        return getKey().replace("_", " ");
    }

    /**
     * Get the canonical title without namespace for DB and URLs
     * Example: "My_example.jpg"
     * @return canonical title without namespace (e.g. "My_example.jpg")
     */
    @NonNull
    public String getKey() {
        return key;
    }

    /**
     * Gets the canonical title for displaying
     * Essentially equivalent to getDisplayTitle
     * Example: "File:My example.jpg"
     *
     * @return canonical title for displaying as a String (e.g. "File:My example.jpg")
     */
    @Override
    @NonNull
    public String toString() {
        return getDisplayTitle();
    }
}
