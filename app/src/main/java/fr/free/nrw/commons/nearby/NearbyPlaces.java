package fr.free.nrw.commons.nearby;

import android.net.Uri;
import android.os.StrictMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NearbyPlaces {

    static List<Place> places = null;

    public static synchronized List<Place> get() {
        if(places != null) {
            return places;
        }
        else {
            try {
                places = new ArrayList<Place>();
                // TODO Load in a different thread and show wait dialog
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);

                URL file = new URL("https://tools.wmflabs.org/wiki-needs-pictures/data/data.csv");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(file.openStream()));

                boolean firstLine = true;
                String line;
                while ((line = in.readLine()) != null) {

                    // Skip CSV header.
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }

                    System.out.println(line);
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
                    String image;

                    switch(type) {
                        case "event":
                            image = "https://upload.wikimedia.org/wikipedia/commons/c/ca/Anarchist_attack_on_the_King_of_Spain_Alfonso_XIII_%281906%29.jpg";
                            break;
                        case "edu":
                            image = "https://upload.wikimedia.org/wikipedia/commons/d/d4/Vrt%2C_pogled_na_glavni_ulaz.JPG";
                            break;
                        case "landmark":
                            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/38/20150902GrenspaalElst_03.JPG/767px-20150902GrenspaalElst_03.JPG";
                            break;
                        default:
                            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/20/Point_d_interrogation.jpg/120px-Point_d_interrogation.jpg";
                    }

                    places.add(new Attraction(
                            name,
                            type, // list
                            type, // details
                            Uri.parse(image),
                            null,
                            new LatLng(latitude, longitude),
                            CITY_SYDNEY
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
