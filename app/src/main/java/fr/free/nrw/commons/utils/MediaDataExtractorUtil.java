package fr.free.nrw.commons.utils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaDataExtractorUtil {

    /**
     * We could fetch all category links from API, but we actually only want the ones
     * directly in the page source so they're editable. In the future this may change.
     *
     * @param source wikitext source code
     */
    public static ArrayList<String> extractCategories(String source) {
        ArrayList<String> categories = new ArrayList<>();
        Pattern regex = Pattern.compile("\\[\\[\\s*Category\\s*:([^]]*)\\s*\\]\\]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(source);
        while (matcher.find()) {
            String cat = matcher.group(1).trim();
            categories.add(cat);
        }

        return categories;
    }


}
