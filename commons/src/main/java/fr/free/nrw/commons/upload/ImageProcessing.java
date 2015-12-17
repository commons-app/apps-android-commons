package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;


/**
 * Created by misao on 16-Dec-15.
 */
//Needs to extend Activity in order to call getContentResolver(). Might not be the best way?
public class ImageProcessing {

    private Uri uri;
    private ExifInterface exif;
    private Context context;

    public ImageProcessing(Context context, Uri uri){
        this.context = context;
        this.uri = uri;
    }

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

    public String getCoords(String filePath) {
        String latitude = "";
        String longitude = "";
        String latitude_ref = "";
        String longitude_ref = "";

        try {
            exif = new ExifInterface(filePath);
            latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            latitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            longitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            Log.d("Image", "Latitude: " + latitude + " " + latitude_ref);
            Log.d("Image", "Longitude: " + longitude + " " + longitude_ref);

        } catch (IOException e) {
            Log.w("Image", e);
        }
        return latitude;
    }

}
