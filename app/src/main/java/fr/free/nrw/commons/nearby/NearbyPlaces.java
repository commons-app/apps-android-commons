package fr.free.nrw.commons.nearby;

import android.net.Uri;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NearbyPlaces {

    private static final String TAG = "NearbyPlaces";
    static List<Place> places = null;

    public static List<Place> get() {
        if(places != null) {
            return places;
        }
        else {
            try {
                places = new ArrayList<Place>();
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);


                URL file = new URL("https://tools.wmflabs.org/wiki-needs-pictures/data/data.csv");

                BufferedReader in = new BufferedReader(new InputStreamReader(file.openStream()));

                boolean firstLine = true;
                String line;
                Log.d(TAG, "Reading from CSV file...");

                while ((line = in.readLine()) != null) {

                    // Skip CSV header.
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }

                    String[] fields = line.split(",");
                    String name = fields[0];

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
                e.printStackTrace();
            }
        }
        return places;
    }

}
