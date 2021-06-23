package fr.free.nrw.commons.wikidata;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.wikidata.model.AddEditTagResponse;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import org.wikipedia.wikidata.Statement_partial;

@Singleton
public class WikidataClient {


    private final WikidataInterface wikidataInterface;
    private final Gson gson;

    @Inject
    public WikidataClient(WikidataInterface wikidataInterface, final Gson gson) {
        this.wikidataInterface = wikidataInterface;
        this.gson = gson;
    }

    /**
     * Create wikidata claim to add P18 value
     *
     * @return revisionID of the edit
     */
    Observable<Long> setClaim(Statement_partial claim, String tags) {
        return getCsrfToken()
            .flatMap(
                csrfToken -> wikidataInterface.postSetClaim(gson.toJson(claim), tags, csrfToken))
            .map(mwPostResponse -> mwPostResponse.getPageinfo().getLastrevid());
    }

    /**
     * Get csrf token for wikidata edit
     */
    @NotNull
    private Observable<String> getCsrfToken() {
        return wikidataInterface.getCsrfToken()
            .map(mwQueryResponse -> mwQueryResponse.query().csrfToken());
    }
}
