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
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.depictions.models.Search;
import fr.free.nrw.commons.media.MediaInterface;
import fr.free.nrw.commons.upload.depicts.DepictsInterface;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import fr.free.nrw.commons.utils.CommonsDateUtil;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Depicts Client to handle custom calls to Commons Wikibase APIs
 */
@Singleton
public class DepictsClient {

    private final DepictsInterface depictsInterface;
    private final MediaInterface mediaInterface;
    private static final String NO_DEPICTED_IMAGE = "No Image for Depiction";

    @Inject
    public DepictsClient(DepictsInterface depictsInterface, MediaInterface mediaInterface) {
        this.depictsInterface = depictsInterface;
        this.mediaInterface = mediaInterface;
    }

    /**
     * Search for depictions using the search item
     * @return list of depicted items
     */
    public Observable<DepictedItem> searchForDepictions(String query, int limit, int offset) {
        return depictsInterface.searchForDepicts(
                query,
                String.valueOf(limit),
                Locale.getDefault().getLanguage(),
                Locale.getDefault().getLanguage(),
                String.valueOf(offset)
        )
                .flatMap(depictSearchResponse ->Observable.fromIterable(depictSearchResponse.getSearch()))
                .map(DepictedItem::new);
    }

    /**
     * Get URL for image using image name
     * Ex: title = Guion Bluford
     * Url = https://upload.wikimedia.org/wikipedia/commons/thumb/0/04/Guion_Bluford.jpg/70px-Guion_Bluford.jpg
     */
    private String getThumbnailUrl(String title) {
        String baseUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/";
        title = title.replace(" ", "_");
        String MD5Hash =  getMd5(title);
        /**
         * We use 70 pixels as the size of our Thumbnail (as it is the perfect fits our UI)
         */
        return baseUrl + MD5Hash.charAt(0) + '/' + MD5Hash.charAt(0) + MD5Hash.charAt(1) + '/' + title + "/70px-" + title;
    }

    /**
     * Ex: entityId = Q357458
     * value returned = Elgin Baylor Night program.jpeg
     */
    public Single<String> getP18ForItem(String entityId) {
        return depictsInterface.getImageForEntity(entityId)
                .map(commonsFilename -> {
                    String name;
                    try {
                        JsonObject claims = commonsFilename.getAsJsonObject("claims").getAsJsonObject();
                        JsonObject p18 = claims.get("P18").getAsJsonArray().get(0).getAsJsonObject();
                        JsonObject mainsnak = p18.get("mainsnak").getAsJsonObject();
                        JsonObject datavalue = mainsnak.get("datavalue").getAsJsonObject();
                        JsonPrimitive value = datavalue.get("value").getAsJsonPrimitive();
                        name = value.toString();
                        name = name.substring(1, name.length() - 1);
                    } catch (Exception e) {
                        name="";
                    }
                    if (!name.isEmpty()){
                        return getThumbnailUrl(name);
                    } else return NO_DEPICTED_IMAGE;
                })
                .singleOrError();
    }

    /**
     * @return list of images for a particular depict entity
     */
    public Observable<List<Media>> fetchImagesForDepictedItem(String query, int sroffset) {
        return mediaInterface.fetchImagesForDepictedItem("haswbstatement:" + BuildConfig.DEPICTS_PROPERTY + "=" + query, String.valueOf(sroffset))
                .map(mwQueryResponse -> {
                    List<Media> mediaList =  new ArrayList<>();
                    for (Search s: mwQueryResponse.getQuery().getSearch()) {
                        Media media = new Media(null,
                                getUrl(s.getTitle()),
                                s.getTitle(),
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
     * Ex: Tiger_Woods
     * Value: https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Tiger_Woods.jpg/70px-Tiger_Woods.jpg
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
