package fr.free.nrw.commons.filepicker;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.core.content.FileProvider;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(FileProvider.class)
public class ShadowFileProvider {

    @Implementation
    public Cursor query(final Uri uri, final String[] projection, final String selection,
        final String[] selectionArgs,
        final String sortOrder) {

        if (uri == null) {
            return null;
        }

        final String[] columns = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        final Object[] values = {"dummy", 500};
        final MatrixCursor cursor = new MatrixCursor(columns, 1);

        if (!uri.equals(Uri.EMPTY)) {
            cursor.addRow(values);
        }
        return cursor;
    }
}
