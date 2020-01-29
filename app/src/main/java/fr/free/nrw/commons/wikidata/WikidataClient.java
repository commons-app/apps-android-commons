package fr.free.nrw.commons.wikidata;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.wikidata.model.AddEditTagResponse;
import fr.free.nrw.commons.wikidata.model.ComplexDataValue;
import fr.free.nrw.commons.wikidata.model.ComplexSnak;
import fr.free.nrw.commons.wikidata.model.Reference;
import fr.free.nrw.commons.wikidata.model.Claim;
import fr.free.nrw.commons.wikidata.model.SimpleDataValue;
import fr.free.nrw.commons.wikidata.model.SimpleSnak;
import fr.free.nrw.commons.wikidata.model.ValueWithLanguageCode;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import static fr.free.nrw.commons.wikidata.WikidataEditService.COMMONS_APP_TAG;

@Singleton
public class WikidataClient {


    private final WikidataInterface wikidataInterface;
    private final Gson gson;

    @Inject
    public WikidataClient(WikidataInterface wikidataInterface,
                          Gson gson) {
        this.wikidataInterface = wikidataInterface;
        this.gson = gson;
    }

    /**
     * Create wikidata claim to add P18 value
     * @param entityId wikidata entity ID
     * @param value value of the P18 edit
     * @return revisionID of the edit
     */
    Observable<Long> createClaim(String entityId, String value) {
        return getCsrfToken()
                .flatMap(csrfToken -> wikidataInterface.postCreateClaim(toRequestBody(entityId),
                        toRequestBody("value"),
                        toRequestBody("P18"),
                        toRequestBody(value),
                        toRequestBody("en"),
                        toRequestBody(csrfToken)))
                .map(mwPostResponse -> mwPostResponse.getPageinfo().getLastrevid());
    }

    /**
     * Create wikidata claim to add P18 value
     *
     * @param entityId wikidata entity ID
     * @param value    value of the P18 edit
     * @return revisionID of the edit
     */
    Observable<Long> setClaim(String entityId, String value, Map<String, String> mediaLegends) {
        return getCsrfToken()
                .flatMap(csrfToken -> wikidataInterface.postSetClaim(gson.toJson(getSetClaimRequestObject(entityId, value, mediaLegends)),
                        COMMONS_APP_TAG,
                        csrfToken))
                .map(mwPostResponse -> mwPostResponse.getPageinfo().getLastrevid());
    }

    private Claim getSetClaimRequestObject(String entityId,
                                           String value,
                                           Map<String, String> mediaLegends) {
        SimpleSnak mainSnak = new SimpleSnak("value", "P18", new SimpleDataValue("string", value));

        List<ComplexSnak> complexSnakList = new ArrayList<>();
        for (String languageCode : mediaLegends.keySet()) {
            complexSnakList.add(new ComplexSnak("snaktype",
                    "P2096",
                    new ComplexDataValue("monolingualtext",
                            new ValueWithLanguageCode(mediaLegends.get(languageCode), languageCode))));
        }

        Reference reference = new Reference(new HashMap<String, List<ComplexSnak>>() {{
            put("P2096", complexSnakList);
        }}, Collections.singletonList("P2096"));
        return new Claim("statement", mainSnak, entityId, Collections.singletonList(reference), "normal");
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
