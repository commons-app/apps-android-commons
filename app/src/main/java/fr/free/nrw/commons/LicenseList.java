package fr.free.nrw.commons;

import android.app.Activity;
import android.content.res.Resources;
import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a list of Licenses
 */
public class LicenseList {
    private Map<String, License> licenses = new HashMap<>();
    private Resources res;

    /**
     * Constructs new instance of LicenceList
     *
     * @param activity License activity
     */
    public LicenseList(Activity activity) {
        res = activity.getResources();
        XmlPullParser parser = res.getXml(R.xml.wikimedia_licenses);
        String namespace = "https://www.mediawiki.org/wiki/Extension:UploadWizard/xmlns/licenses";
        while (xmlFastForward(parser, namespace, "license")) {
            String id = parser.getAttributeValue(null, "id");
            String template = parser.getAttributeValue(null, "template");
            String url = parser.getAttributeValue(null, "url");
            String name = nameForTemplate(template);
            License license = new License(id, template, url, name);
            licenses.put(id, license);
        }
    }

    /**
     * Gets a collection of licenses
     * @return License values
     */
    public Collection<License> values() {
        return licenses.values();
    }

    /**
     * Gets license
     * @param key License key
     * @return License that matches key
     */
    public License get(String key) {
        return licenses.get(key);
    }

    /**
     * Creates a license from template
     * @param template License template
     * @return null
     */
    @Nullable
    License licenseForTemplate(String template) {
        for (License license : values()) {
            if (template.compareToIgnoreCase(license.getTemplate()) == 0) {
                return license;
            }
        }
        return null;
    }

    /**
     * Gets template name id
     * @param template License template
     * @return name id of template
     */
    private String nameIdForTemplate(String template) {
        // hack :D (converts dashes and periods to underscores)
        // cc-by-sa-3.0 -> cc_by_sa_3_0
        return "license_name_" + template.toLowerCase(Locale.ENGLISH).replace("-",
                "_").replace(".", "_");
    }

    /**
     * Gets name of given template
     * @param template License template
     * @return name of template
     */
    private String nameForTemplate(String template) {
        int nameId = res.getIdentifier("fr.free.nrw.commons:string/"
                + nameIdForTemplate(template), null, null);
        return (nameId != 0) ? res.getString(nameId) : template;
    }

    /**
     * Fast-forward an XmlPullParser to the next instance of the given element
     * in the input stream (namespaced).
     *
     * @param parser
     * @param namespace
     * @param element
     * @return true on match, false on failure
     */
    private boolean xmlFastForward(XmlPullParser parser, String namespace, String element) {
        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG &&
                        parser.getNamespace().equals(namespace) &&
                        parser.getName().equals(element)) {
                    // We found it!
                    return true;
                }
            }
            return false;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
