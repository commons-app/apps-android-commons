package fr.free.nrw.commons.utils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by root on 19.05.2018.
 */

public class MediaDataExtractorUtil {

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
