package fr.free.nrw.commons.modifications;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CategoryModifier extends PageModifier {

    public static String PARAM_CATEGORIES = "categories";

    public static String MODIFIER_NAME = "CategoriesModifier";

    public CategoryModifier(String... categories) {
        super(MODIFIER_NAME);
        JSONArray categoriesArray = new JSONArray();
        for (String category: categories) {
            categoriesArray.put(category);
        }
        try {
            params.putOpt(PARAM_CATEGORIES, categoriesArray);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public CategoryModifier(JSONObject data) {
        super(MODIFIER_NAME);
        this.params = data;
    }

    @Override
    public String doModification(String pageName, String pageContents) {
        JSONArray categories;
        categories = params.optJSONArray(PARAM_CATEGORIES);

        StringBuilder categoriesString = new StringBuilder();
        for (int i = 0; i < categories.length(); i++) {
            String category = categories.optString(i);
            categoriesString.append("\n[[Category:").append(category).append("]]");
        }
        return pageContents + categoriesString.toString();
    }

    @Override
    public String getEditSumary() {
        return "Added " + params.optJSONArray(PARAM_CATEGORIES).length() + " categories.";
    }
}
