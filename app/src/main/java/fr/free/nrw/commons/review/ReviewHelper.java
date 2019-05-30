package fr.free.nrw.commons.review;

import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.RecentChange;

import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.Single;

@Singleton
public class ReviewHelper {
    private static final int MAX_RANDOM_TRIES = 5;

    private static final String[] imageExtensions = new String[]{".jpg", ".jpeg", ".png"};

    private final OkHttpJsonApiClient okHttpJsonApiClient;
    private final MediaWikiApi mediaWikiApi;

    @Inject
    public ReviewHelper(OkHttpJsonApiClient okHttpJsonApiClient, MediaWikiApi mediaWikiApi) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.mediaWikiApi = mediaWikiApi;
    }

    Single<Media> getRandomMedia() {
        return getRandomFileChange()
                .flatMap(fileName -> okHttpJsonApiClient.getMedia(fileName, false));
    }

    /**
     * Gets a random file change for review.
     * - Picks the most recent changes in the last 30 day window
     * - Picks a random file from those changes
     * - Checks if the file is nominated for deletion
     * - Retries upto 5 times for getting a file which is not nominated for deletion
     *
     * @return
     */
    private Single<String> getRandomFileChange() {
        return okHttpJsonApiClient.getRecentFileChanges()
                .map(this::findImageInRecentChanges)
                .flatMap(title -> mediaWikiApi.pageExists("Commons:Deletion_requests/" + title)
                        .map(pageExists -> new Pair<>(title, pageExists)))
                .map((Pair<String, Boolean> pair) -> {
                    if (!pair.second) {
                        return pair.first;
                    }
                    throw new Exception("Already nominated for deletion");
                }).retry(MAX_RANDOM_TRIES);
    }

    Single<MwQueryPage.Revision> getFirstRevisionOfFile(String fileName) {
        return okHttpJsonApiClient.getFirstRevisionOfFile(fileName);
    }

    @Nullable
    private String findImageInRecentChanges(List<RecentChange> recentChanges) {
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
            if (recentChange.getType().equals("log") && !(recentChange.getOldRevisionId() == 0)) {
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
