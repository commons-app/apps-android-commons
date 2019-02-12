package fr.free.nrw.commons.contributions;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

import fr.free.nrw.commons.upload.FileUtils;

public class UploadableFile implements Parcelable {
    public static final Creator<UploadableFile> CREATOR = new Creator<UploadableFile>() {
        @Override
        public UploadableFile createFromParcel(Parcel in) {
            return new UploadableFile(in);
        }

        @Override
        public UploadableFile[] newArray(int size) {
            return new UploadableFile[size];
        }
    };
    private final File file;

    public UploadableFile(File file) {
        this.file = file;
    }

    public UploadableFile(Parcel in) {
        file = (File) in.readSerializable();
    }

    public String getFilePath() {
        return file.getPath();
    }

    public Uri getMediaUri() {
        return Uri.parse(getFilePath());
    }

    public String getMimeType(Context context) {
        return FileUtils.getMimeType(context, getMediaUri());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(file);
    }

    /**
     * Get filePath creation date from uri from all possible content providers
     *
     * @return
     */
    public long getFileCreatedDate(Context context) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(getMediaUri(), null, null, null, null);
            if (cursor == null) {
                return -1;//Could not fetch last_modified
            }
            //Content provider contracts for opening gallery from the app and that by sharing from gallery from outside are different and we need to handle both the cases
            int lastModifiedColumnIndex = cursor.getColumnIndex("last_modified");//If gallery is opened from in app
            if (lastModifiedColumnIndex == -1) {
                lastModifiedColumnIndex = cursor.getColumnIndex("datetaken");
            }
            //If both the content providers do not give the data, lets leave it to Jesus
            if (lastModifiedColumnIndex == -1) {
                return -1L;
            }
            cursor.moveToFirst();
            return cursor.getLong(lastModifiedColumnIndex);
        } catch (Exception e) {
            return -1;////Could not fetch last_modified
        }
    }
}
