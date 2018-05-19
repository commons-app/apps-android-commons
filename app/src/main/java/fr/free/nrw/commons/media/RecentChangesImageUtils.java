package fr.free.nrw.commons.media;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;

public class RecentChangesImageUtils {

    private static final String[] imageExtensions = new String[]
            {".jpg", ".png"};

    @Nullable
    public static String findImageInRecentChanges(NodeList childNodes) {
        String imageTitle;
        for (int i = 0; i < childNodes.getLength(); i++) {
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
