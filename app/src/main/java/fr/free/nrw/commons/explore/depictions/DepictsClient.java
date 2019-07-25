package fr.free.nrw.commons.explore.depictions;

import android.text.TextUtils;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.upload.depicts.DepictsInterface;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import fr.free.nrw.commons.wikidata.model.DepictSearchItem;
import io.reactivex.Observable;

@Singleton
public class DepictsClient {

    private final DepictsInterface depictsInterface;

    @Inject
    public DepictsClient(DepictsInterface depictsInterface) {
        this.depictsInterface = depictsInterface;
    }

    public Observable<DepictedItem> searchForDepictions(String query, int limit) {

        return depictsInterface.searchForDepicts(query, String.valueOf(limit))
                .flatMap(depictSearchResponse -> Observable.fromIterable(depictSearchResponse.getSearch()))
                .map(depictSearchItem -> new DepictedItem(depictSearchItem.getLabel(), depictSearchItem.getDescription(), null, false, depictSearchItem.getId()));
    }
}
