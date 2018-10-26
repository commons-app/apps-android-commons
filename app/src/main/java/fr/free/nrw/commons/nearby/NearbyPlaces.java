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

public class NearbyPlaces {

    private static final int MIN_RESULTS = 40;
    private static final double INITIAL_RADIUS = 1.0; // in kilometers
    private static final double MAX_RADIUS = 300.0; // in kilometers
    private static final double RADIUS_MULTIPLIER = 1.618;
    private static final Uri WIKIDATA_QUERY_URL = Uri.parse("https://query.wikidata.org/sparql");
    private static final Uri WIKIDATA_QUERY_UI_URL = Uri.parse("https://query.wikidata.org/");
    private final String wikidataQuery;
    private double radius = INITIAL_RADIUS;

    public NearbyPlaces() {
        try {
            wikidataQuery = FileUtils.readFromResource("/queries/nearby_query.rq");
            Timber.v(wikidataQuery);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    List<Place> getFromWikidataQuery(LatLng curLatLng, String lang) throws IOException {
        List<Place> places = Collections.emptyList();

            // increase the radius gradually to find a satisfactory number of nearby places
            while (radius <= MAX_RADIUS) {
                try {
                    places = getFromWikidataQuery(curLatLng, lang, radius);
                } catch (InterruptedIOException e) {
                    Timber.d("exception in fetching nearby places", e.getLocalizedMessage());
                    return places;
                }
                Timber.d("%d results at radius: %f", places.size(), radius);
                if (places.size() >= MIN_RESULTS) {
                    break;
                } else {
                    radius *= RADIUS_MULTIPLIER;
                }
            }
        // make sure we will be able to send at least one request next time
        if (radius > MAX_RADIUS) {
            radius = MAX_RADIUS;
        }

        return places;
    }

    private List<Place> getFromWikidataQuery(LatLng cur,
                                             String lang,
                                             double radius)
            throws IOException {
        List<Place> places = new ArrayList<>();

        String query = wikidataQuery
                .replace("${RAD}", String.format(Locale.ROOT, "%.2f", radius))
                .replace("${LAT}", String.format(Locale.ROOT, "%.4f", cur.getLatitude()))
                .replace("${LONG}", String.format(Locale.ROOT, "%.4f", cur.getLongitude()))
                .replace("${LANG}", lang);

        Timber.v("# Wikidata query: \n" + query);

        // format as a URL
        Timber.d(WIKIDATA_QUERY_UI_URL.buildUpon().fragment(query).build().toString());
        String url = WIKIDATA_QUERY_URL.buildUpon()
                .appendQueryParameter("query", query).build().toString();
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
            String type = Utils.stripLocalizedString(fields[4]);
            String icon = fields[5];
            String wikipediaSitelink = Utils.stripLocalizedString(fields[7]);
            String commonsSitelink = Utils.stripLocalizedString(fields[8]);
            String category = Utils.stripLocalizedString(fields[9]);

            Timber.v("Name: " + name + ", type: " + type + ", category: " + category + ", wikipediaSitelink: " + wikipediaSitelink + ", commonsSitelink: " + commonsSitelink);

            double latitude;
            double longitude;
            Matcher matcher =
                    Pattern.compile("Point\\(([^ ]+) ([^ ]+)\\)").matcher(point);
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
                    Place.Label.fromText(type), // list
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
