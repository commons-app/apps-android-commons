package fr.free.nrw.commons.explore.depictions;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    /**
     * Search for depictions using the search item
     * @return list of depicted items
     */

    public Observable<DepictedItem> searchForDepictions(String query, int limit, int offset) {

        return depictsInterface.searchForDepicts(query, String.valueOf(limit), Locale.getDefault().getLanguage(), Locale.getDefault().getLanguage(), String.valueOf(offset))
                .flatMap(depictSearchResponse -> Observable.fromIterable(depictSearchResponse.getSearch()))
                .map(depictSearchItem -> new DepictedItem(depictSearchItem.getLabel(), depictSearchItem.getDescription(), getImageUrl(depictSearchItem.getLabel()), false, depictSearchItem.getId()));
    }

    /**
     *Get url for image usig image name
     */

    private String getImageUrl(String title) {
        String baseUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/";
        title = title.replace(" ", "_");
        if (!title.endsWith(".jpg")){
            title+=".jpg";
        }
        String MD5Hash =  getMd5(title);
        return baseUrl + MD5Hash.charAt(0) + '/' + MD5Hash.charAt(0) + MD5Hash.charAt(1) + '/' + title + "/70px-" + title;
    }

    public Single<String> getP18ForItem(String entityId) {
        return depictsInterface.getLabelForEntity(entityId)
                .map(response -> {
                    String name;
                    try {
                        JsonObject claims = response.getAsJsonObject("claims").getAsJsonObject();
                        JsonObject P18 = claims.get("P18").getAsJsonArray().get(0).getAsJsonObject();
                        JsonObject mainsnak = P18.get("mainsnak").getAsJsonObject();
                        JsonObject datavalue = mainsnak.get("datavalue").getAsJsonObject();
                        JsonPrimitive value = datavalue.get("value").getAsJsonPrimitive();
                        name = value.toString();
                        name = name.substring(1, name.length() - 1);
                    } catch (Exception e) {
                        name="";
                    }
                    if (!name.isEmpty()){
                        return getImageUrl(name);
                    } else return null;
                })
                .singleOrError();
    }

    /**
     * @return list of images for a particular depict entity
     */

    public Observable<List<Media>> fetchImagesForDepictedItem(String query, int limit, int sroffset) {
        return mediaInterface.fetchImagesForDepictedItem("haswbstatement:P180="+query, String.valueOf(sroffset))
                .map(mwQueryResponse -> {
                    List<Media> mediaList =  new ArrayList<>();
                    for (Search s: mwQueryResponse.getQuery().getSearch()) {
                        Media media = new Media(null,
                                getUrl(s.getTitle()),
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

    /**
     * Get url for the image from media of depictions
     */

    private String getUrl(String title) {
        String baseUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/";
        title = title.substring(title.indexOf(':')+1);
        title = title.replace(" ", "_");
        String MD5Hash = getMd5(title);
        return baseUrl + MD5Hash.charAt(0) + '/' + MD5Hash.charAt(0) + MD5Hash.charAt(1) + '/' + title + "/640px-" + title;
    }

    /**
     * Generates MD5 hash for the filename
     */

    public String getMd5(String input)
    {
        try {

            // Static getInstance method is called with hashing MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // digest() method is called to calculate message digest
            //  of an input digest() return array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse the date string into the required format
     * @param dateStr
     * @return date in the required format
     */

    @Nullable
    private static Date safeParseDate(String dateStr) {
        try {
            return CommonsDateUtil.getIso8601DateFormatShort().parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }
}
