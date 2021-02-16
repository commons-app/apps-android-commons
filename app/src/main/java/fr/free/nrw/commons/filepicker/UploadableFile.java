package fr.free.nrw.commons.filepicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;

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

    private final Uri contentUri;
    private final File file;

    public UploadableFile(Uri contentUri, File file) {
        this.contentUri = contentUri;
        this.file = file;
    }

    public UploadableFile(File file) {
        this.file = file;
        this.contentUri = Uri.parse(file.getAbsolutePath());
    }

    public UploadableFile(Parcel in) {
        this.contentUri = in.readParcelable(Uri.class.getClassLoader());
        file = (File) in.readSerializable();
    }

    public Uri getContentUri() {
        return contentUri;
    }

    public File getFile() {
        return file;
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


    /**
     * First try to get the file creation date from EXIF else fall back to CP
     * @param context
     * @return
     */
    @Nullable
    public DateTimeWithSource getFileCreatedDate(Context context) {
        DateTimeWithSource dateTime = getDateTime();
        if (dateTime == null) {
            return getFileCreatedDateFromCP(context);
        } else {
            return dateTime;
        }
    }

    /**
     * Get filePath creation date from uri from all possible content providers
     *
     * @return
     */
    private DateTimeWithSource getFileCreatedDateFromCP(Context context) {
        try {
            Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null);
            if (cursor == null) {
                return null;//Could not fetch last_modified
            }
            //Content provider contracts for opening gallery from the app and that by sharing from gallery from outside are different and we need to handle both the cases
            int lastModifiedColumnIndex = cursor.getColumnIndex("last_modified");//If gallery is opened from in app
            if (lastModifiedColumnIndex == -1) {
                lastModifiedColumnIndex = cursor.getColumnIndex("datetaken");
            }
            //If both the content providers do not give the data, lets leave it to Jesus
            if (lastModifiedColumnIndex == -1) {
                cursor.close();
                return null;
            }
            cursor.moveToFirst();
            return new DateTimeWithSource(cursor.getLong(lastModifiedColumnIndex), DateTimeWithSource.CP_SOURCE);
        } catch (Exception e) {
            return null;////Could not fetch last_modified
        }
    }

    /**
     * Get filePath creation date from uri
     *
     * @return
     */
    private DateTimeWithSource getDateTime() {
        try {
            /**
             * fetch the last modified date of the selected file.
             */
                Date lastModDate= new Date(file.lastModified());
            /**
             * if android version is greater than 8.0 then
             * we can get the creation Time of the file.
             */
                if(android.os.Build.VERSION.SDK_INT >= VERSION_CODES.O){
                    Path path= Paths.get(file.getAbsolutePath());
                    BasicFileAttributes attr= Files.readAttributes(path,BasicFileAttributes.class);
                    FileTime last=attr.creationTime();
                    if(last!=null) {
                        return new DateTimeWithSource(last.toMillis(),
                            DateTimeWithSource.EXIF_SOURCE);
                    }
                }
                if(lastModDate!=null){
                        return new DateTimeWithSource(lastModDate.getTime(),
                            DateTimeWithSource.EXIF_SOURCE);
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(contentUri, 0);
        parcel.writeSerializable(file);
    }

    /**
     * This class contains the epochDate along with the source from which it was extracted
     */
    public class DateTimeWithSource {
        public static final String CP_SOURCE = "contentProvider";
        public static final String EXIF_SOURCE = "exif";

        private final long epochDate;
        private final String source;

        public DateTimeWithSource(long epochDate, String source) {
            this.epochDate = epochDate;
            this.source = source;
        }

        public DateTimeWithSource(Date date, String source) {
            this.epochDate = date.getTime();
            this.source = source;
        }

        public long getEpochDate() {
            return epochDate;
        }

        public String getSource() {
            return source;
        }
    }
}
