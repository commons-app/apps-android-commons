package fr.free.nrw.commons.media;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Random;

import javax.annotation.Nullable;

public class RecentChangesImageUtils {

    private static final String[] imageExtensions = new String[]
            {".jpg", ".jpeg", ".png"};

    @Nullable
    public static String findImageInRecentChanges(NodeList childNodes) {
        String imageTitle;
        Random r = new Random();
        int count = childNodes.getLength();
        // Build a range array
        int[] randomIndexes = new int[count];
        for (int i = 0; i < count; i++) {
            randomIndexes[i] = i;
        }
        // Then shuffle it
        for (int i = 0; i < count; i++) {
            int swapIndex = r.nextInt(count);
            int temp = randomIndexes[i];
            randomIndexes[i] = randomIndexes[swapIndex];
            randomIndexes[swapIndex] = temp;
        }
        for (int i = 0; i < count; i++) {
            int randomIndex = randomIndexes[i];
            Element e = (Element) childNodes.item(randomIndex);
            if (e.getAttribute("type").equals("log") && !e.getAttribute("old_revid").equals("0")) {
                // For log entries, we only want ones where old_revid is zero, indicating a new file
                continue;
            }
            imageTitle = e.getAttribute("title");

            for (String imageExtension : imageExtensions) {
                if (imageTitle.toLowerCase().endsWith(imageExtension)) {
                    return imageTitle;
                }
            }
        }
        return null;
    }
}
