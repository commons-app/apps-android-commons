package fr.free.nrw.commons.utils;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class MediaDataExtractorUtil {

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
