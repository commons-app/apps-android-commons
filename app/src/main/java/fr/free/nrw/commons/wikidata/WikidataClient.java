package fr.free.nrw.commons.wikidata;

import static fr.free.nrw.commons.wikidata.WikidataEditService.COMMONS_APP_TAG;

import com.google.gson.Gson;
import fr.free.nrw.commons.upload.WikidataItem;
import fr.free.nrw.commons.wikidata.model.WikidataSetClaim;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.wikidata.model.AddEditTagResponse;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import okhttp3.MediaType;
import okhttp3.RequestBody;

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
     * @param entity wikidata entity ID
     * @param value value of the P18 edit
     * @return revisionID of the edit
     */
    Observable<Long> createImageClaim(WikidataItem entity, String value) {
        return getCsrfToken()
                .flatMap(csrfToken -> wikidataInterface.postCreateClaim(
                        toRequestBody(entity.getId()),
                        toRequestBody("value"),
                        toRequestBody(WikidataProperties.IMAGE.getPropertyName()),
                        toRequestBody(value),
                        toRequestBody("en"),
                        toRequestBody(csrfToken)))
                .map(mwPostResponse -> mwPostResponse.getPageinfo().getLastrevid());
    }

    /**
     * Create wikidata claim to add P18 value
     * @return revisionID of the edit
     */
    Observable<Long> setClaim(WikidataSetClaim claim, String tags) {
        return getCsrfToken()
            .flatMap(csrfToken -> wikidataInterface.postSetClaim(gson.toJson(claim), tags, csrfToken))
            .map(mwPostResponse -> mwPostResponse.getPageinfo().getLastrevid());
    }

    /**
     * Converts string value to RequestBody for multipart request
     */
    private RequestBody toRequestBody(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }

    /**
     * Get csrf token for wikidata edit
     */
    @NotNull
    private Observable<String> getCsrfToken() {
        return wikidataInterface.getCsrfToken().map(mwQueryResponse -> mwQueryResponse.query().csrfToken());
    }

    /**
     * Add edit tag for a given revision ID. The app currently uses this to tag P18 edits
     * @param revisionId revision ID of the page edited
     * @param tag to be added
     * @param reason to be mentioned
     */
    ObservableSource<AddEditTagResponse> addEditTag(Long revisionId, String tag, String reason) {
        return getCsrfToken()
                .flatMap(csrfToken -> wikidataInterface.addEditTag(String.valueOf(revisionId),
                        tag,
                        reason,
                        csrfToken));
    }
}
