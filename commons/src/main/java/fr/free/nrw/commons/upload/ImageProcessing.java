package fr.free.nrw.commons.upload;

import android.content.Context;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;


public class ImageProcessing {

    private Uri uri;
    private Context context;

    public ImageProcessing(Context context, Uri uri){
        this.context = context;
        this.uri = uri;
    }
    /**
    * Gets file path of image from its Uri
    * May return null
    */
    public String getFilePath(){
        String filePath ="";
        // Will return "image:x*"
        String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];
        String[] column = { MediaStore.Images.Media.DATA };

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();

        Log.d("Image", "File path: " + filePath);
        return filePath;
    }

    //Extract GPS coords of image
    public String getCoords(String filePath) {

        ExifInterface exif;
        String latitude = "";
        String longitude = "";
        String latitude_ref = "";
        String longitude_ref = "";
        String decimalCoords = "";

        try {
            exif = new ExifInterface(filePath);
            latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            latitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            longitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            Log.d("Image", "Latitude: " + latitude + " " + latitude_ref);
            Log.d("Image", "Longitude: " + longitude + " " + longitude_ref);

            decimalCoords = getDecimalCoords(latitude, latitude_ref, longitude, longitude_ref);

        } catch (IOException e) {
            Log.w("Image", e);
        }
        return decimalCoords;
    }

    //Converts format of coords into decimal coords as required by API for next step
    private String getDecimalCoords(String latitude, String latitude_ref, String longitude, String longitude_ref) {

        Float decLatitude, decLongitude;

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

        return (String.valueOf(decLatitude) + ", " + String.valueOf(decLongitude));
    }

    private Float convertToDegree(String stringDMS){
        Float result;
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        Double D0 = new Double(stringD[0]);
        Double D1 = new Double(stringD[1]);
        Double FloatD = D0/D1;

        String[] stringM = DMS[1].split("/", 2);
        Double M0 = new Double(stringM[0]);
        Double M1 = new Double(stringM[1]);
        Double FloatM = M0/M1;

        String[] stringS = DMS[2].split("/", 2);
        Double S0 = new Double(stringS[0]);
        Double S1 = new Double(stringS[1]);
        Double FloatS = S0/S1;

        result = new Float(FloatD + (FloatM/60) + (FloatS/3600));

        return result;
    }

}
