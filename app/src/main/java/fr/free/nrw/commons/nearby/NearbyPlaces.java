package fr.free.nrw.commons.nearby;

import android.net.Uri;
import android.os.StrictMode;

import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.location.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class NearbyPlaces {

    private static final int MIN_RESULTS = 40;
    private static final double INITIAL_RADIUS = 1.0;
    private static final double MAX_RADIUS = 300.0;
    private static final double RADIUS_MULTIPLIER = 1.618;
    private static final String WIKIDATA_QUERY_URL = "https://query.wikidata.org/sparql?query=${QUERY}";
    private static final String WIKIDATA_QUERY_TEMPLATE = "SELECT\n" +
                    "  (SAMPLE(?location) as ?location)\n" +
                    "  ?item\n" +
                    "  (SAMPLE(COALESCE(?item_label_preferred_language, ?item_label_any_language)) as ?label)\n" +
                    "  (SAMPLE(?classId) as ?class)\n" +
                    "  (SAMPLE(COALESCE(?class_label_preferred_language, ?class_label_any_language, \"?\")) as ?class_label)\n" +
                    "  (SAMPLE(COALESCE(?icon0, ?icon1)) as ?icon)\n" +
                    "  (SAMPLE(COALESCE(?emoji0, ?emoji1)) as ?emoji)\n" +
                    "WHERE {\n" +
                    "  # Around given location...\n" +
                    "  SERVICE wikibase:around {\n" +
                    "    ?item wdt:P625 ?location.\n" +
                    "    bd:serviceParam wikibase:center \"Point(${LONG} ${LAT})\"^^geo:wktLiteral. \n" +
                    "    bd:serviceParam wikibase:radius \"${RADIUS}\" . # Radius in kilometers.\n" +
                    "  }\n" +
                    "  \n" +
                    "  # ... and without an image.\n" +
                    "  MINUS {?item wdt:P18 []}\n" +
                    "  \n" +
                    "  # Get the label in the preferred language of the user, or any other language if no label is available in that language.\n" +
                    "  OPTIONAL {?item rdfs:label ?item_label_preferred_language. FILTER (lang(?item_label_preferred_language) = \"${LANG}\")}\n" +
                    "  OPTIONAL {?item rdfs:label ?item_label_any_language}\n" +
                    "  \n" +
                    "  # Get the class label in the preferred language of the user, or any other language if no label is available in that language.\n" +
                    "  OPTIONAL {\n" +
                    "    ?item p:P31/ps:P31 ?classId.\n" +
                    "    OPTIONAL {?classId rdfs:label ?class_label_preferred_language. FILTER (lang(?class_label_preferred_language) = \"${LANG}\")}\n" +
                    "    OPTIONAL {?classId rdfs:label ?class_label_any_language}\n" +
                    "\n" +
                    "    # Get icon\n" +
                    "    OPTIONAL { ?classId wdt:P2910 ?icon0. }\n" +
                    "    OPTIONAL { ?classId wdt:P279*/wdt:P2910 ?icon1. }\n" +
                    "    # Get emoji\n" +
                    "    OPTIONAL { ?classId wdt:P487 ?emoji0. }\n" +
                    "    OPTIONAL { ?classId wdt:P279*/wdt:P487 ?emoji1. }\n" +
                    "  }\n" +
                    "}\n" +
                    "GROUP BY ?item\n";
    private static NearbyPlaces singleton;
    private double radius = INITIAL_RADIUS;
    private List<Place> places;

    private NearbyPlaces(){
    }

    List<Place> getFromWikidataQuery(LatLng curLatLng, String lang) {
        List<Place> places = Collections.emptyList();

        try {
            // increase the radius gradually to find a satisfactory number of nearby places
            while (radius < MAX_RADIUS) {
                places = getFromWikidataQuery(curLatLng, lang, radius);
                Timber.d("%d results at radius: %f", places.size(), radius);
                if (places.size() >= MIN_RESULTS) {
                    break;
                } else {
                    radius *= RADIUS_MULTIPLIER;
                }
            }
        } catch (IOException e) {
            Timber.d(e.toString());
            // errors tend to be caused by too many results (and time out)
            // try a small radius next time
            Timber.d("back to initial radius: %f", radius);
            radius = INITIAL_RADIUS;
        }
        return places;
    }

    private List<Place> getFromWikidataQuery(LatLng cur, String lang, double radius)
            throws IOException {
        List<Place> places = new ArrayList<>();
        String query = WIKIDATA_QUERY_TEMPLATE.replace("${RADIUS}", "" + radius)
                .replace("${LAT}", "" + String.format(Locale.ROOT, "%.3f", cur.latitude))
                .replace("${LONG}", "" + String.format(Locale.ROOT, "%.3f", cur.longitude))
                .replace("${LANG}", "" + lang);
        query = URLEncoder.encode(query, "utf-8").replace("+", "%20");
        String url = WIKIDATA_QUERY_URL.replace("${QUERY}", query);
        Timber.d(url);
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
            String point = fields[0];
            String name = Utils.stripLocalizedString(fields[2]);
            String type = Utils.stripLocalizedString(fields[4]);
            String icon = fields[5];

            double latitude = 0;
            double longitude = 0;
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
                    type, // list
                    type, // details
                    Uri.parse(icon),
                    new LatLng(latitude, longitude)
            ));
        }
        in.close();

        return places;
    }

    List<Place> getFromWikiNeedsPictures() {
        if (places != null) {
            return places;
        } else {
            try {
                places = new ArrayList<>();
                StrictMode.ThreadPolicy policy
                        = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);

                URL file = new URL("https://tools.wmflabs.org/wiki-needs-pictures/data/data.csv");

                BufferedReader in = new BufferedReader(new InputStreamReader(file.openStream()));

                boolean firstLine = true;
                String line;
                Timber.d("Reading from CSV file...");

                while ((line = in.readLine()) != null) {

                    // Skip CSV header.
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }

                    String[] fields = line.split(",");
                    String name = Utils.stripLocalizedString(fields[0]);

                    double latitude;
                    double longitude;
                    try {
                        latitude = Double.parseDouble(fields[1]);
                    } catch (NumberFormatException e) {
                        latitude = 0;
                    }
                    try {
                        longitude = Double.parseDouble(fields[2]);
                    } catch (NumberFormatException e) {
                        longitude = 0;
                    }

                    String type = fields[3];

                    places.add(new Place(
                            name,
                            type, // list
                            type, // details
                            null,
                            new LatLng(latitude, longitude)
                    ));
                }
                in.close();

            } catch (IOException e) {
                Timber.d(e.toString());
            }
        }
        return places;
    }

    /**
     * Get the singleton instance of this class.
     * The instance is created upon the first invocation of this method, and then reused.
     *
     * @return The singleton instance
     */
    public static synchronized NearbyPlaces getInstance() {
        if (singleton == null) {
            singleton = new NearbyPlaces();
        }
        return singleton;
    }
}
