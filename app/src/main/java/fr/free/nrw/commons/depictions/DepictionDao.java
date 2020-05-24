package fr.free.nrw.commons.depictions;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public class DepictionDao {

    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public DepictionDao(@Named("depiction") Provider<ContentProviderClient> clientProvider) {
      this.clientProvider = clientProvider;
    }

    public void save(Depiction depiction) {
      ContentProviderClient db = clientProvider.get();
      try {
        if (depiction.getContentUri() == null) {
          depiction.setContentUri(db.insert(DepictionContentProvider.BASE_URI, toContentValues(depiction)));
        } else {
          db.update(depiction.getContentUri(), toContentValues(depiction), null, null);
        }
      } catch (RemoteException e) {
        throw new RuntimeException(e);
      } finally {
        db.release();
      }
    }

  /**
   * Find persisted depiction in database, based on its name.
   *
   * @param name Depiction's name
   * @return depiction from database, or null if not found
   */
  @Nullable
  Depiction find(String name) {
    Cursor cursor = null;
    ContentProviderClient db = clientProvider.get();
    try {
      cursor = db.query(
          DepictionContentProvider.BASE_URI,
          Table.ALL_FIELDS,
          Table.COLUMN_NAME + "=?",
          new String[]{name},
          null);
      if (cursor != null && cursor.moveToFirst()) {
        return fromCursor(cursor);
      }
    } catch (RemoteException e) {
      // This feels lazy, but to hell with checked exceptions. :)
      throw new RuntimeException(e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      db.release();
    }
    return null;
  }

  /**
   * Retrieve recently-used depictions, ordered by descending date.
   *
   * @return a list containing recent depictions
   */
  @NonNull
  List<String> recentDepictions(int limit) {
    List<String> items = new ArrayList<>();
    Cursor cursor = null;
    ContentProviderClient db = clientProvider.get();
    try {
      cursor = db.query(
          DepictionContentProvider.BASE_URI,
          Table.ALL_FIELDS,
          null,
          new String[]{},
          Table.COLUMN_LAST_USED + " DESC");
      // fixme add a limit on the original query instead of falling out of the loop?
      while (cursor != null && cursor.moveToNext()
          && cursor.getPosition() < limit) {
        items.add(fromCursor(cursor).getName());
      }
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      db.release();
    }
    return items;
  }

  @NonNull
  Depiction fromCursor(Cursor cursor) {
    // Hardcoding column positions!
    return new Depiction(
        DepictionContentProvider.uriForId(cursor.getInt(cursor.getColumnIndex(Table.COLUMN_ID))),
        cursor.getString(cursor.getColumnIndex(Table.COLUMN_NAME)),
        new Date(cursor.getLong(cursor.getColumnIndex(Table.COLUMN_LAST_USED))),
        cursor.getInt(cursor.getColumnIndex(Table.COLUMN_TIMES_USED))
    );
  }

  private ContentValues toContentValues(Depiction depiction) {
    ContentValues cv = new ContentValues();
    cv.put(DepictionDao.Table.COLUMN_NAME, depiction.getName());
    cv.put(DepictionDao.Table.COLUMN_LAST_USED, depiction.getLastUsed().getTime());
    cv.put(DepictionDao.Table.COLUMN_TIMES_USED, depiction.getTimesUsed());
    return cv;
  }


  public static class Table {
    public static final String TABLE_NAME = "depictions";

    public static final String COLUMN_ID = "_id";
    static final String COLUMN_NAME = "name";
    static final String COLUMN_LAST_USED = "last_used";
    static final String COLUMN_TIMES_USED = "times_used";

    // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
    public static final String[] ALL_FIELDS = {
        COLUMN_ID,
        COLUMN_NAME,
        COLUMN_LAST_USED,
        COLUMN_TIMES_USED
    };

    static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;

    static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
        + COLUMN_ID + " INTEGER PRIMARY KEY,"
        + COLUMN_NAME + " STRING,"
        + COLUMN_LAST_USED + " INTEGER,"
        + COLUMN_TIMES_USED + " INTEGER"
        + ");";

    public static void onCreate(SQLiteDatabase db) {
      db.execSQL(CREATE_TABLE_STATEMENT);
    }

    public static void onDelete(SQLiteDatabase db) {
      db.execSQL(DROP_TABLE_STATEMENT);
      onCreate(db);
    }

    public static void onUpdate(SQLiteDatabase db, int from, int to) {
      if (from == to) {
        return;
      }
      if (from < 4) {
        // doesn't exist yet
        from++;
        onUpdate(db, from, to);
        return;
      }
      if (from == 4) {
        // table added in version 5
        onCreate(db);
        from++;
        onUpdate(db, from, to);
        return;
      }
      if (from == 5) {
        from++;
        onUpdate(db, from, to);
        return;
      }
    }
  }
}
