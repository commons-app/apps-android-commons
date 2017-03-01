package fr.free.nrw.commons;

import android.app.Activity;
import android.content.res.Resources;

import org.xmlpull.v1.XmlPullParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LicenseList {
    Map<String, fr.free.nrw.commons.License> licenses = new HashMap<>();
    Resources res;

    private static String XMLNS_LICENSE = "https://www.mediawiki.org/wiki/Extension:UploadWizard/xmlns/licenses";

    public LicenseList(Activity activity) {
        res = activity.getResources();
        XmlPullParser parser = res.getXml(R.xml.wikimedia_licenses);
        while (fr.free.nrw.commons.Utils.xmlFastForward(parser, XMLNS_LICENSE, "license")) {
            String id = parser.getAttributeValue(null, "id");
            String template = parser.getAttributeValue(null, "template");
            String url = parser.getAttributeValue(null, "url");
            String name = nameForTemplate(template);
            fr.free.nrw.commons.License license = new fr.free.nrw.commons.License(id, template, url, name);
            licenses.put(id, license);
        }
    }

    public Set<String> keySet() {
        return licenses.keySet();
    }

    public Collection<fr.free.nrw.commons.License> values() {
        return licenses.values();
    }

    public fr.free.nrw.commons.License get(String key) {
        return licenses.get(key);
    }

    public fr.free.nrw.commons.License licenseForTemplate(String template) {
        String ucTemplate = fr.free.nrw.commons.Utils.capitalize(template);
        for (fr.free.nrw.commons.License license : values()) {
            if (ucTemplate.equals(fr.free.nrw.commons.Utils.capitalize(license.getTemplate()))) {
                return license;
            }
        }
        return null;
    }

    public String nameIdForTemplate(String template) {
        // hack :D (converts dashes and periods to underscores)
        // cc-by-sa-3.0 -> cc_by_sa_3_0
        return "license_name_" + template.toLowerCase().replace("-", "_").replace(".", "_");
    }

    private int stringIdByName(String stringId) {
        return res.getIdentifier("fr.free.nrw.commons:string/" + stringId, null, null);
    }

    public String nameForTemplate(String template) {
        //Log.d("Commons", "LicenseList.nameForTemplate: template: " + template);
        String stringId = nameIdForTemplate(template);
        //Log.d("Commons", "LicenseList.nameForTemplate: stringId: " + stringId);
        int nameId = stringIdByName(stringId);
        //Log.d("Commons", "LicenseList.nameForTemplate: nameId: " + nameId);
        if(nameId != 0) {
            String name = res.getString(nameId);
            //Log.d("Commons", "LicenseList.nameForTemplate: name: " + name);
            return name;
        }
        return template;
    }
}