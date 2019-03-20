package fr.free.nrw.commons;

import android.text.Html;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.Single;

/**
 * Fetch additional media data from the network that we don't store locally.
 *
 * This includes things like category lists and multilingual descriptions,
 * which are not intrinsic to the media and may change due to editing.
 */
@Singleton
public class MediaDataExtractor {
    private final MediaWikiApi mediaWikiApi;
    private final OkHttpJsonApiClient okHttpJsonApiClient;

    @Inject
    public MediaDataExtractor(MediaWikiApi mwApi,
                              OkHttpJsonApiClient okHttpJsonApiClient) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.mediaWikiApi = mwApi;
    }

    public Single<Media> fetchMediaDetails(String filename) {
        Single<Media> mediaSingle = okHttpJsonApiClient.getMedia(filename, false);
        Single<Boolean> pageExistsSingle = mediaWikiApi.pageExists("Commons:Deletion_requests/" + filename);
        Single<String> discussionSingle = getDiscussion(filename);
        return Single.zip(mediaSingle, pageExistsSingle, discussionSingle, (media, deletionStatus, discussion) -> {
            media.setDiscussion(discussion);
            if (deletionStatus) {
                media.setRequestedDeletion();
            }
            return media;
        });
    }

    private Single<String> getDiscussion(String filename) {
        return mediaWikiApi.fetchMediaByFilename(filename.replace("File", "File talk"))
                .flatMap(mediaResult -> mediaWikiApi.parseWikicode(mediaResult.getWikiSource()))
                .map(discussion -> Html.fromHtml(discussion).toString());
    }
}
