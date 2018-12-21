package fr.free.nrw.commons.nearby;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.upload.FileUtils;
import timber.log.Timber;

/**
 * Handles the Wikidata query to obtain Places around search location
 */
public class NearbyPlaces {

    private static final double INITIAL_RADIUS = 1.0; // in kilometers
    private static final double RADIUS_MULTIPLIER = 1.618;
    private static final Uri WIKIDATA_QUERY_URL = Uri.parse("https://query.wikidata.org/sparql");
    private static final Uri WIKIDATA_QUERY_UI_URL = Uri.parse("https://query.wikidata.org/");
    private final String wikidataQuery;
    public double radius = INITIAL_RADIUS;

    /**
     * Reads Wikidata query to check nearby wikidata items which needs picture, with a circular
     * search. As a point is center of a circle with a radius will be set later.
     */
    public NearbyPlaces() {
        try {
            wikidataQuery = FileUtils.readFromResource("/queries/nearby_query.rq");
            Timber.v(wikidataQuery);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Expands the radius as needed for the Wikidata query
     * @param curLatLng coordinates of search location
     * @param lang user's language
     * @param returnClosestResult true if only the nearest point is desired
     * @return list of places obtained
     * @throws IOException if query fails
     */
    List<Place> radiusExpander(LatLng curLatLng, String lang, boolean returnClosestResult) throws IOException {

        int minResults;
        double maxRadius;

        List<Place> places = Collections.emptyList();

        // If returnClosestResult is true, then this means that we are trying to get closest point
        // to use in cardView in Contributions fragment
        if (returnClosestResult) {
            minResults = 1; // Return closest nearby place
            maxRadius = 5;  // Return places only in 5 km area
            radius = INITIAL_RADIUS; // refresh radius again, otherwise increased radius is grater than MAX_RADIUS, thus returns null
        } else {
            minResults = 40;
            maxRadius = 300.0; // in kilometers
            radius = INITIAL_RADIUS;
        }

            // Increase the radius gradually to find a satisfactory number of nearby places
            while (radius <= maxRadius) {
                try {
                    places = getFromWikidataQuery(curLatLng, lang, radius);
                } catch (InterruptedIOException e) {
                    Timber.d("exception in fetching nearby places", e.getLocalizedMessage());
                    return places;
                }
                Timber.d("%d results at radius: %f", places.size(), radius);
                if (places.size() >= minResults) {
                    break;
                } else {
                    radius *= RADIUS_MULTIPLIER;
                }
            }
        // make sure we will be able to send at least one request next time
        if (radius > maxRadius) {
            radius = maxRadius;
        }
        return places;
    }

    /**
     * Runs the Wikidata query to populate the Places around search location
     * @param cur coordinates of search location
     * @param lang user's language
     * @param radius radius for search, as determined by radiusExpander()
     * @return list of places obtained
     * @throws IOException if query fails
     */
    private List<Place> getFromWikidataQuery(LatLng cur, String lang, double radius) throws IOException {
        List<Place> places = new ArrayList<>();

        String query = wikidataQuery
                .replace("${RAD}", String.format(Locale.ROOT, "%.2f", radius))
                .replace("${LAT}", String.format(Locale.ROOT, "%.4f", cur.getLatitude()))
                .replace("${LONG}", String.format(Locale.ROOT, "%.4f", cur.getLongitude()))
                .replace("${LANG}", lang);

        Timber.v("# Wikidata query: \n" + query);

        // format as a URL
        Timber.d(WIKIDATA_QUERY_UI_URL.buildUpon().fragment(query).build().toString());
        String url = WIKIDATA_QUERY_URL.buildUpon().appendQueryParameter("query", query).build().toString();
        URLConnection conn = new URL(url).openConnection();
        conn.setRequestProperty("Accept", "text/tab-separated-values");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String line;
        Timber.d("Reading from query result...");
        while ((line = in.readLine()) != null) {
            Timber.v(line);
            line = line + "\n"; // to pad columns and make fields a fixed size
            if (!line.startsWith("\"Point")) {
                continue;
            }

            String[] fields = line.split("\t");
            Timber.v("Fields: " + Arrays.toString(fields));
            String point = fields[0];
            String wikiDataLink = Utils.stripLocalizedString(fields[1]);
            String name = Utils.stripLocalizedString(fields[2]);

            //getting icon link here
            String identifier = Utils.stripLocalizedString(fields[3]);
            //getting the ID which is at the end of link
            identifier = identifier.split("/")[Utils.stripLocalizedString(fields[3]).split("/").length-1];
            //replaced the extra > char from fields
            identifier = identifier.replace(">","");

            String type = Utils.stripLocalizedString(fields[4]);
            String icon = fields[5];
            String wikipediaSitelink = Utils.stripLocalizedString(fields[7]);
            String commonsSitelink = Utils.stripLocalizedString(fields[8]);
            String category = Utils.stripLocalizedString(fields[9]);
            Timber.v("Name: " + name + ", type: " + type + ", category: " + category + ", wikipediaSitelink: " + wikipediaSitelink + ", commonsSitelink: " + commonsSitelink);

            double latitude;
            double longitude;
            Matcher matcher = Pattern.compile("Point\\(([^ ]+) ([^ ]+)\\)").matcher(point);
            if (!matcher.find()) {
                continue;
            }
            try {
                longitude = Double.parseDouble(matcher.group(1));
                latitude = Double.parseDouble(matcher.group(2));
            } catch (NumberFormatException e) {
                throw new RuntimeException("LatLng parse error: " + point);
            }

            places.add(new Place(
                    name,
                    Place.Label.fromText(identifier), // list
                    type, // details
                    Uri.parse(icon),
                    new LatLng(latitude, longitude, 0),
                    category,
                    new Sitelinks.Builder()
                            .setWikipediaLink(wikipediaSitelink)
                            .setCommonsLink(commonsSitelink)
                            .setWikidataLink(wikiDataLink)
                            .build()
            ));
        }
        in.close();

        return places;
    }
}
