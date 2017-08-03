package fr.free.nrw.commons.category;

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.RemoteException;

import java.util.Date;

class CategoryCountUpdater extends AsyncTask<Void, Void, Void> {

    private final String name;
    private final ContentProviderClient client;

    CategoryCountUpdater(String name, ContentProviderClient client) {
        this.name = name;
        this.client = client;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Category cat = lookupCategory(name);
        cat.incTimesUsed();

        cat.setContentProviderClient(client);
        cat.save();

        return null; // Make the compiler happy.
    }

    private Category lookupCategory(String name) {
        Cursor cursor = null;
        try {
            cursor = client.query(
                    CategoryContentProvider.BASE_URI,
                    Category.Table.ALL_FIELDS,
                    Category.Table.COLUMN_NAME + "=?",
                    new String[]{name},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                return Category.fromCursor(cursor);
            }
        } catch (RemoteException e) {
            // This feels lazy, but to hell with checked exceptions. :)
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Newly used category...
        Category cat = new Category();
        cat.setName(name);
        cat.setLastUsed(new Date());
        cat.setTimesUsed(0);
        return cat;
    }
}
