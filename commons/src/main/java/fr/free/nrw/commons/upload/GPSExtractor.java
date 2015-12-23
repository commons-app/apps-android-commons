package fr.free.nrw.commons.upload;

import android.media.ExifInterface;
import android.util.Log;

import java.io.IOException;


public class GPSExtractor {

    private String filePath;

    public GPSExtractor(String filePath){
        this.filePath = filePath;
    }

    //Extract GPS coords of image
    public String getCoords() {

        ExifInterface exif;
        String latitude = "";
        String longitude = "";
        String latitude_ref = "";
        String longitude_ref = "";
        String decimalCoords = "";

        try {
            exif = new ExifInterface(filePath);

        } catch (IOException e) {
            Log.w("Image", e);
            return null;
        }

        if (exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) == null) {
            Log.w("Image", "Picture has no GPS info");
            return null;
        }
        else {
            latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            latitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            longitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            Log.d("Image", "Latitude: " + latitude + " " + latitude_ref);
            Log.d("Image", "Longitude: " + longitude + " " + longitude_ref);

            decimalCoords = getDecimalCoords(latitude, latitude_ref, longitude, longitude_ref);
            return decimalCoords;
        }
    }

    //Converts format of coords into decimal coords as required by API for next step
    private String getDecimalCoords(String latitude, String latitude_ref, String longitude, String longitude_ref) {

        double decLatitude, decLongitude;

        if(latitude_ref.equals("N")){
            decLatitude = convertToDegree(latitude);
        }
        else{
            decLatitude = 0 - convertToDegree(latitude);
        }

        if(longitude_ref.equals("E")){
            decLongitude = convertToDegree(longitude);
        }
        else{
            decLongitude = 0 - convertToDegree(longitude);
        }

        return (String.valueOf(decLatitude) + "|" + String.valueOf(decLongitude));
    }

    private double convertToDegree(String stringDMS){
        double result;
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        double d0 = Double.parseDouble(stringD[0]);
        double d1 = Double.parseDouble(stringD[1]);
        double degrees = d0/d1;

        String[] stringM = DMS[1].split("/", 2);
        double m0 = Double.parseDouble(stringM[0]);
        double m1 = Double.parseDouble(stringM[1]);
        double minutes = m0/m1;

        String[] stringS = DMS[2].split("/", 2);
        double s0 = Double.parseDouble(stringS[0]);
        double s1 = Double.parseDouble(stringS[1]);
        double seconds = s0/s1;

        result = degrees + (minutes/60) + (seconds/3600);
        return result;
    }

}
