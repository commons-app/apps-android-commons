package fr.free.nrw.commons.explore.depictions;

import androidx.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.util.StringUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.depictions.models.Search;
import fr.free.nrw.commons.media.MediaInterface;
import fr.free.nrw.commons.upload.depicts.DepictsInterface;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import fr.free.nrw.commons.utils.CommonsDateUtil;
import io.reactivex.Observable;
import io.reactivex.Single;

@Singleton
public class DepictsClient {

    private final DepictsInterface depictsInterface;
    private final MediaInterface mediaInterface;
    private Map<String, Map<String, String>> continuationStore;

    @Inject
    public DepictsClient(DepictsInterface depictsInterface, MediaInterface mediaInterface) {
        this.depictsInterface = depictsInterface;
        this.mediaInterface = mediaInterface;
        this.continuationStore = new HashMap<>();
    }

    public Observable<DepictedItem> searchForDepictions(String query, int limit) {

        return depictsInterface.searchForDepicts(query, String.valueOf(limit))
                .flatMap(depictSearchResponse -> Observable.fromIterable(depictSearchResponse.getSearch()))
                .map(depictSearchItem -> new DepictedItem(depictSearchItem.getLabel(), depictSearchItem.getDescription(), null, false, depictSearchItem.getId()));
    }

    public Observable<List<Media>> fetchListofDepictions(String query, int limit) {
        return mediaInterface.fetchListofDepictions("haswbstatement:P180="+query)
                .map(mwQueryResponse -> {
                    List<Media> mediaList =  new ArrayList<>();
                    for (Search s: mwQueryResponse.getQuery().getSearch()) {
                        Media media = new Media(null,
                                "",
                                s.getTitle(),
                                new HashMap<>(),
                                "",
                                0,
                                safeParseDate(s.getTimestamp()),
                                safeParseDate(s.getTimestamp()),
                                ""
                        );
                        mediaList.add(media);
                    }
                    return mediaList;
                });

    }

    private Single<List<Media>> responseToMediaList(Observable<MwQueryResponse> response, String key) {
        return response.flatMap(mwQueryResponse -> {
            if (null == mwQueryResponse
                    || null == mwQueryResponse.query()
                    || null == mwQueryResponse.query().pages()) {
                return Observable.empty();
            }
            continuationStore.put(key, mwQueryResponse.continuation());
            return Observable.fromIterable(mwQueryResponse.query().pages());
        })
                .map(Media::from)
                .collect(ArrayList<Media>::new, List::add);
    }

    @Nullable
    private static Date safeParseDate(String dateStr) {
        try {
            return CommonsDateUtil.getIso8601DateFormatShort().parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }
}
