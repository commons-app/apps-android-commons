package fr.free.nrw.commons.filepicker

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(FileProvider::class)
class ShadowFileProvider {

    @Implementation
    fun query(
        uri: Uri?,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {

        if (uri == null) {
            return null
        }

        val columns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val values = arrayOf<Any>("dummy", 500)
        val cursor = MatrixCursor(columns, 1)

        if (uri != Uri.EMPTY) {
            cursor.addRow(values)
        }
        return cursor
    }
}
