package fr.free.nrw.commons.upload;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

public class FilePathConverter {

    private Uri uri;
    private Context context;

    public FilePathConverter(Context context, Uri uri) {
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
}
