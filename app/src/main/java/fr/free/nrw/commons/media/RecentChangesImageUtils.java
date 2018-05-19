package fr.free.nrw.commons.media;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;

public class RecentChangesImageUtils {

    private static final String[] imageExtensions = new String[]
            {".jpg", ".jpeg", ".png"};

    @Nullable
    public static String findImageInRecentChanges(NodeList childNodes) {
        String imageTitle;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Element e = (Element)childNodes.item(i);
            if (e.getAttribute("type").equals("log") && !e.getAttribute("old_revid").equals("0")) {
                // For log entries, we only want ones where old_revid is zero, indicating a new file
                continue;
            }
            imageTitle = ((Element)childNodes.item(i)).getAttribute("title");

            for (String imageExtension : imageExtensions) {
                if (imageTitle.toLowerCase().endsWith(imageExtension)) {
                    return imageTitle;
                }
            }
        }
        return null;
    }
}
