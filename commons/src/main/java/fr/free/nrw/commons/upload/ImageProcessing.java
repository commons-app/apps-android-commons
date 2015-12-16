package fr.free.nrw.commons.upload;

import android.app.Activity;
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
public class ImageProcessing extends Activity{

    private Uri uri;
    private ExifInterface exif;

    public ImageProcessing(Uri uri){
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
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();

        Log.d("Image", "File path: " + filePath);
        return filePath;
    }

    public String getLatitude(String filePath) {
        String latitude = "";

        try {
            exif = new ExifInterface(filePath);
            latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            Log.d("Image", "Latitude: " + latitude);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return latitude;
    }

}
