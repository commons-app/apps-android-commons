package fr.free.nrw.commons.media;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import fr.free.nrw.commons.mwapi.model.RecentChange;

public class RecentChangesImageUtils {

    private static final String[] imageExtensions = new String[]
            {".jpg", ".jpeg", ".png"};

    @Nullable
    public static String findImageInRecentChanges(List<RecentChange> recentChanges) {
        String imageTitle;
        Random r = new Random();
        int count = recentChanges.size();
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
            RecentChange recentChange = recentChanges.get(randomIndex);
            if (recentChange.getType().equals("log") && !recentChange.getOldRevisionId().equals("0")) {
                // For log entries, we only want ones where old_revid is zero, indicating a new file
                continue;
            }
            imageTitle = recentChange.getTitle();

            for (String imageExtension : imageExtensions) {
                if (imageTitle.toLowerCase().endsWith(imageExtension)) {
                    return imageTitle;
                }
            }
        }
        return null;
    }
}
