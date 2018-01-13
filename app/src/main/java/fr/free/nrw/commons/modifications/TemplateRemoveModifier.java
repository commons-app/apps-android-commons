package fr.free.nrw.commons.modifications;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateRemoveModifier extends PageModifier {

    public static final String MODIFIER_NAME = "TemplateRemoverModifier";

    public static final String PARAM_TEMPLATE_NAME = "template";

    public static final Pattern PATTERN_TEMPLATE_OPEN = Pattern.compile("\\{\\{");
    public static final Pattern PATTERN_TEMPLATE_CLOSE = Pattern.compile("\\}\\}");

    public TemplateRemoveModifier(String templateName) {
        super(MODIFIER_NAME);
        try {
            params.putOpt(PARAM_TEMPLATE_NAME, templateName);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public TemplateRemoveModifier(JSONObject data) {
        super(MODIFIER_NAME);
        this.params = data;
    }

    @Override
    public String doModification(String pageName, String pageContents) {
        String templateRawName = params.optString(PARAM_TEMPLATE_NAME);
        // Wikitext title normalizing rules. Spaces and _ equivalent
        // They also 'condense' - any number of them reduce to just one (just like HTML)
        String templateNormalized = templateRawName.trim().replaceAll("(\\s|_)+", "(\\s|_)+");

        // Not supporting {{ inside <nowiki> and HTML comments yet
        // (Thanks to marktraceur for reminding me of the HTML comments exception)
        Pattern templateStartPattern = Pattern.compile("\\{\\{" + templateNormalized, Pattern.CASE_INSENSITIVE);
        Matcher matcher = templateStartPattern.matcher(pageContents);

        while (matcher.find()) {
            int braceCount = 1;
            int startIndex = matcher.start();
            int curIndex = matcher.end();
            Matcher openMatch = PATTERN_TEMPLATE_OPEN.matcher(pageContents);
            Matcher closeMatch = PATTERN_TEMPLATE_CLOSE.matcher(pageContents);

            while (curIndex < pageContents.length()) {
                boolean openFound = openMatch.find(curIndex);
                boolean closeFound = closeMatch.find(curIndex);

                if (openFound && (!closeFound || openMatch.start() < closeMatch.start())) {
                    braceCount++;
                    curIndex = openMatch.end();
                } else if (closeFound) {
                    braceCount--;
                    curIndex = closeMatch.end();
                } else if (braceCount > 0) {
                    // The template never closes, so...remove nothing
                    curIndex = startIndex;
                    break;
                }

                if (braceCount == 0) {
                    // The braces have all been closed!
                    break;
                }
            }

            // Strip trailing whitespace
            while (curIndex < pageContents.length()) {
                if (pageContents.charAt(curIndex) == ' ' || pageContents.charAt(curIndex) == '\n') {
                    curIndex++;
                } else {
                    break;
                }
            }

            // I am so going to hell for this, sigh
            pageContents = pageContents.substring(0, startIndex) + pageContents.substring(curIndex);
            matcher = templateStartPattern.matcher(pageContents);
        }

        return pageContents;
    }

    @Override
    public String getEditSumary() {
        return "Removed template " + params.optString(PARAM_TEMPLATE_NAME) + ".";
    }
}
