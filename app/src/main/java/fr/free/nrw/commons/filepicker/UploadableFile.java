package fr.free.nrw.commons.filepicker;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.File;
import java.io.IOException;
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
        DateTimeWithSource dateTimeFromExif = getDateTimeFromExif();
        if (dateTimeFromExif == null) {
            return getFileCreatedDateFromCP(context);
        } else {
            return dateTimeFromExif;
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
     * Get filePath creation date from uri from EXIF
     *
     * @return
     */
    private DateTimeWithSource getDateTimeFromExif() {
        Metadata metadata;
        try {
            metadata = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory!=null && directory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                return new DateTimeWithSource(date, DateTimeWithSource.EXIF_SOURCE);
            }
        } catch (ImageProcessingException e) {
            e.printStackTrace();
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
