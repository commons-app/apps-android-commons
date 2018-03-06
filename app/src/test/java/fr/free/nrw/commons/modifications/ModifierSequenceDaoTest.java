package fr.free.nrw.commons.modifications;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.TestCommonsApplication;

import static fr.free.nrw.commons.modifications.ModificationsContentProvider.BASE_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = TestCommonsApplication.class)
public class ModifierSequenceDaoTest {

    private static final String EXPECTED_MEDIA_URI = "http://example.com/";

    @Mock
    ContentProviderClient client;
    @Mock
    SQLiteDatabase database;
    @Captor
    ArgumentCaptor<ContentValues> contentValuesCaptor;

    private ModifierSequenceDao testObject;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        testObject = new ModifierSequenceDao(() -> client);
    }

    @Test
    public void createFromCursorWithEmptyModifiers() {
        MatrixCursor cursor = createCursor("");

        ModifierSequence seq = testObject.fromCursor(cursor);

        assertEquals(EXPECTED_MEDIA_URI, seq.getMediaUri().toString());
        assertEquals(BASE_URI.buildUpon().appendPath("1").toString(), seq.getContentUri().toString());
        assertTrue(seq.getModifiers().isEmpty());
    }

    @Test
    public void createFromCursorWtihCategoryModifier() {
        MatrixCursor cursor = createCursor("{\"name\": \"CategoriesModifier\", \"data\": {}}");

        ModifierSequence seq = testObject.fromCursor(cursor);

        assertEquals(1, seq.getModifiers().size());
        assertTrue(seq.getModifiers().get(0) instanceof CategoryModifier);
    }

    @Test
    public void createFromCursorWithRemoveModifier() {
        MatrixCursor cursor = createCursor("{\"name\": \"TemplateRemoverModifier\", \"data\": {}}");

        ModifierSequence seq = testObject.fromCursor(cursor);

        assertEquals(1, seq.getModifiers().size());
        assertTrue(seq.getModifiers().get(0) instanceof TemplateRemoveModifier);
    }

    @Test
    public void deleteSequence() throws Exception {
        when(client.delete(isA(Uri.class), isNull(String.class), isNull(String[].class))).thenReturn(1);
        ModifierSequence seq = testObject.fromCursor(createCursor(""));

        testObject.delete(seq);

        verify(client).delete(eq(seq.getContentUri()), isNull(String.class), isNull(String[].class));
    }

    @Test(expected = RuntimeException.class)
    public void deleteTranslatesRemoteExceptions() throws Exception {
        when(client.delete(isA(Uri.class), isNull(String.class), isNull(String[].class))).thenThrow(new RemoteException(""));
        ModifierSequence seq = testObject.fromCursor(createCursor(""));

        testObject.delete(seq);
    }

    @Test
    public void saveExistingSequence() throws Exception {
        String modifierJson = "{\"name\":\"CategoriesModifier\",\"data\":{}}";
        String expectedData = "{\"modifiers\":[" + modifierJson + "]}";
        MatrixCursor cursor = createCursor(modifierJson);

        testObject.save(testObject.fromCursor(cursor));

        verify(client).update(eq(testObject.fromCursor(cursor).getContentUri()), contentValuesCaptor.capture(), isNull(String.class), isNull(String[].class));
        ContentValues cv = contentValuesCaptor.getValue();
        assertEquals(2, cv.size());
        assertEquals(EXPECTED_MEDIA_URI, cv.get(ModifierSequenceDao.Table.COLUMN_MEDIA_URI));
        assertEquals(expectedData, cv.get(ModifierSequenceDao.Table.COLUMN_DATA));
    }

    @Test
    public void saveNewSequence() throws Exception {
        Uri expectedContentUri = BASE_URI.buildUpon().appendPath("1").build();
        when(client.insert(isA(Uri.class), isA(ContentValues.class))).thenReturn(expectedContentUri);

        ModifierSequence seq = new ModifierSequence(Uri.parse(EXPECTED_MEDIA_URI));
        testObject.save(seq);

        verify(client).insert(eq(ModificationsContentProvider.BASE_URI), contentValuesCaptor.capture());
        ContentValues cv = contentValuesCaptor.getValue();
        assertEquals(2, cv.size());
        assertEquals(EXPECTED_MEDIA_URI, cv.get(ModifierSequenceDao.Table.COLUMN_MEDIA_URI));
        assertEquals("{\"modifiers\":[]}", cv.get(ModifierSequenceDao.Table.COLUMN_DATA));
        assertEquals(expectedContentUri.toString(), seq.getContentUri().toString());
    }

    @Test(expected = RuntimeException.class)
    public void saveTranslatesRemoteExceptions() throws Exception {
        when(client.insert(isA(Uri.class), isA(ContentValues.class))).thenThrow(new RemoteException(""));

        testObject.save(new ModifierSequence(Uri.parse(EXPECTED_MEDIA_URI)));
    }

    @Test
    public void createTable() {
        ModifierSequenceDao.Table.onCreate(database);

        verify(database).execSQL(ModifierSequenceDao.Table.CREATE_TABLE_STATEMENT);
    }

    @Test
    public void updateTable() {
        ModifierSequenceDao.Table.onUpdate(database, 1, 2);

        InOrder inOrder = inOrder(database);
        inOrder.verify(database).execSQL(ModifierSequenceDao.Table.DROP_TABLE_STATEMENT);
        inOrder.verify(database).execSQL(ModifierSequenceDao.Table.CREATE_TABLE_STATEMENT);
    }

    @Test
    public void deleteTable() {
        ModifierSequenceDao.Table.onDelete(database);

        InOrder inOrder = inOrder(database);
        inOrder.verify(database).execSQL(ModifierSequenceDao.Table.DROP_TABLE_STATEMENT);
        inOrder.verify(database).execSQL(ModifierSequenceDao.Table.CREATE_TABLE_STATEMENT);
    }

    @NonNull
    private MatrixCursor createCursor(String modifierJson) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                ModifierSequenceDao.Table.COLUMN_ID,
                ModifierSequenceDao.Table.COLUMN_MEDIA_URI,
                ModifierSequenceDao.Table.COLUMN_DATA
        }, 1);
        cursor.addRow(Arrays.asList("1", EXPECTED_MEDIA_URI, "{\"modifiers\": [" + modifierJson + "]}"));
        cursor.moveToFirst();
        return cursor;
    }
}