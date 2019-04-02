package fr.free.nrw.commons.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
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


    /**
     * Extracts a list of categories from | separated category string
     *
     * @param source
     * @return
     */
    public static List<String> extractCategoriesFromList(String source) {
        if (StringUtils.isBlank(source)) {
            return new ArrayList<>();
        }
        String[] cats = source.split("\\|");
        List<String> categories = new ArrayList<>();
        for (String category : cats) {
            if (!StringUtils.isBlank(category.trim())) {
                categories.add(category);
            }
        }
        return categories;
    }

}
