package fr.free.nrw.commons.media;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Random;

import android.support.annotation.NonNull;

public class RecentChangesImageUtils {

    private static final String[] imageExtensions = new String[]
            {".jpg", ".jpeg", ".png"};

    @NonNull
    public static ArrayList<String> findImagesInRecentChanges(NodeList childNodes) {
        ArrayList<String> imageTitles = new ArrayList<>();
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
            String imageTitle = e.getAttribute("title");

            for (String imageExtension : imageExtensions) {
                if (imageTitle.toLowerCase().endsWith(imageExtension)) {
                    imageTitles.add(imageTitle);
                }
            }
        }
        return imageTitles;
    }
}
