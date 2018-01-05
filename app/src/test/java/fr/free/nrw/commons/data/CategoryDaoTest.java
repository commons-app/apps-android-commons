package fr.free.nrw.commons.data;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.TestCommonsApplication;
import fr.free.nrw.commons.category.CategoryContentProvider;
import fr.free.nrw.commons.data.CategoryDao.Table;

import static fr.free.nrw.commons.category.CategoryContentProvider.BASE_URI;
import static fr.free.nrw.commons.category.CategoryContentProvider.uriForId;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = TestCommonsApplication.class)
public class CategoryDaoTest {

    @Mock
    private ContentProviderClient client;
    @Mock
    private SQLiteDatabase database;
    @Captor
    private ArgumentCaptor<ContentValues> captor;
    @Captor
    private ArgumentCaptor<String[]> queryCaptor;

    private CategoryDao testObject;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        testObject = new CategoryDao(() -> client);
    }

    @Test
    public void createTable() {
        Table.onCreate(database);
        verify(database).execSQL(Table.CREATE_TABLE_STATEMENT);
    }

    @Test
    public void deleteTable() {
        Table.onDelete(database);
        InOrder inOrder = Mockito.inOrder(database);
        inOrder.verify(database).execSQL(Table.DROP_TABLE_STATEMENT);
        inOrder.verify(database).execSQL(Table.CREATE_TABLE_STATEMENT);
    }

    @Test
    public void migrateTableVersionFrom_v1_to_v2() {
        Table.onUpdate(database, 1, 2);
        // Table didnt exist before v5
        verifyZeroInteractions(database);
    }

    @Test
    public void migrateTableVersionFrom_v2_to_v3() {
        Table.onUpdate(database, 2, 3);
        // Table didnt exist before v5
        verifyZeroInteractions(database);
    }

    @Test
    public void migrateTableVersionFrom_v3_to_v4() {
        Table.onUpdate(database, 3, 4);
        // Table didnt exist before v5
        verifyZeroInteractions(database);
    }

    @Test
    public void migrateTableVersionFrom_v4_to_v5() {
        Table.onUpdate(database, 4, 5);
        verify(database).execSQL(Table.CREATE_TABLE_STATEMENT);
    }

    @Test
    public void migrateTableVersionFrom_v5_to_v6() {
        Table.onUpdate(database, 5, 6);
        // Table didnt change in version 6
        verifyZeroInteractions(database);
    }

    @Test
    public void createFromCursor() {
        MatrixCursor cursor = createCursor(1);
        cursor.moveToFirst();
        Category category = testObject.fromCursor(cursor);

        assertEquals(uriForId(1), category.getContentUri());
        assertEquals("foo", category.getName());
        assertEquals(123, category.getLastUsed().getTime());
        assertEquals(2, category.getTimesUsed());
    }

    @Test
    public void saveExistingCategory() throws Exception {
        MatrixCursor cursor = createCursor(1);
        cursor.moveToFirst();
        Category category = testObject.fromCursor(cursor);

        testObject.save(category);

        verify(client).update(eq(category.getContentUri()), captor.capture(), isNull(String.class), isNull(String[].class));
        ContentValues cv = captor.getValue();
        assertEquals(3, cv.size());
        assertEquals(category.getName(), cv.getAsString(Table.COLUMN_NAME));
        assertEquals(category.getLastUsed().getTime(), cv.getAsLong(Table.COLUMN_LAST_USED).longValue());
        assertEquals(category.getTimesUsed(), cv.getAsInteger(Table.COLUMN_TIMES_USED).intValue());
    }

    @Test
    public void saveNewCategory() throws Exception {
        Uri contentUri = CategoryContentProvider.uriForId(111);
        when(client.insert(isA(Uri.class), isA(ContentValues.class))).thenReturn(contentUri);
        Category category = new Category(null, "foo", new Date(234L), 1);

        testObject.save(category);

        verify(client).insert(eq(BASE_URI), captor.capture());
        ContentValues cv = captor.getValue();
        assertEquals(3, cv.size());
        assertEquals(category.getName(), cv.getAsString(Table.COLUMN_NAME));
        assertEquals(category.getLastUsed().getTime(), cv.getAsLong(Table.COLUMN_LAST_USED).longValue());
        assertEquals(category.getTimesUsed(), cv.getAsInteger(Table.COLUMN_TIMES_USED).intValue());
        assertEquals(contentUri, category.getContentUri());
    }

    @Test(expected = RuntimeException.class)
    public void testSaveTranslatesRemoteExceptions() throws Exception {
        when(client.insert(isA(Uri.class), isA(ContentValues.class))).thenThrow(new RemoteException(""));
        testObject.save(new Category());
    }

    @Test
    public void whenTheresNoDataFindReturnsNull_nullCursor() throws Exception {
        when(client.query(any(), any(), anyString(), any(), anyString())).thenReturn(null);

        assertNull(testObject.find("foo"));
    }

    @Test
    public void whenTheresNoDataFindReturnsNull_emptyCursor() throws Exception {
        when(client.query(any(), any(), anyString(), any(), anyString())).thenReturn(createCursor(0));

        assertNull(testObject.find("foo"));
    }

    @Test
    public void cursorsAreClosedAfterUse() throws Exception {
        Cursor mockCursor = mock(Cursor.class);
        when(client.query(any(), any(), anyString(), any(), anyString())).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(false);

        testObject.find("foo");

        verify(mockCursor).close();
    }

    @Test
    public void findCategory() throws Exception {
        when(client.query(any(), any(), anyString(), any(), anyString())).thenReturn(createCursor(1));

        Category category = testObject.find("foo");

        assertEquals(uriForId(1), category.getContentUri());
        assertEquals("foo", category.getName());
        assertEquals(123, category.getLastUsed().getTime());
        assertEquals(2, category.getTimesUsed());

        verify(client).query(
                eq(BASE_URI),
                eq(Table.ALL_FIELDS),
                eq(Table.COLUMN_NAME + "=?"),
                queryCaptor.capture(),
                isNull(String.class)
        );
        assertEquals("foo", queryCaptor.getValue()[0]);
    }

    @Test(expected = RuntimeException.class)
    public void findCategoryTranslatesExceptions() throws Exception {
        when(client.query(any(), any(), anyString(), any(), anyString())).thenThrow(new RemoteException(""));
        testObject.find("foo");
    }

    @Test(expected = RuntimeException.class)
    public void recentCategoriesTranslatesExceptions() throws Exception {
        when(client.query(any(), any(), anyString(), any(), anyString())).thenThrow(new RemoteException(""));
        testObject.recentCategories(1);
    }

    @Test
    public void recentCategoriesReturnsEmptyList_nullCursor() throws Exception {
        when(client.query(any(), any(), anyString(), any(), anyString())).thenReturn(null);

        assertTrue(testObject.recentCategories(1).isEmpty());
    }

    @Test
    public void recentCategoriesReturnsEmptyList_emptyCursor() throws Exception {
        when(client.query(any(), any(), anyString(), any(), anyString())).thenReturn(createCursor(0));

        assertTrue(testObject.recentCategories(1).isEmpty());
    }

    @Test
    public void cursorsAreClosedAfterRecentCategoriesQuery() throws Exception {
        Cursor mockCursor = mock(Cursor.class);
        when(client.query(any(), any(), anyString(), any(), anyString())).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(false);

        testObject.recentCategories(1);

        verify(mockCursor).close();
    }

    @Test
    public void recentCategoriesReturnsLessThanLimit() throws Exception {
        when(client.query(any(), any(), anyString(), any(), anyString())).thenReturn(createCursor(1));

        List<String> result = testObject.recentCategories(10);

        assertEquals(1, result.size());
        assertEquals("foo", result.get(0));

        verify(client).query(
                eq(BASE_URI),
                eq(Table.ALL_FIELDS),
                isNull(String.class),
                queryCaptor.capture(),
                eq(Table.COLUMN_LAST_USED + " DESC")
        );
        assertEquals(0, queryCaptor.getValue().length);
    }

    @Test
    public void recentCategoriesHomorsLimit() throws Exception {
        when(client.query(any(), any(), anyString(), any(), anyString())).thenReturn(createCursor(10));

        List<String> result = testObject.recentCategories(5);

        assertEquals(5, result.size());
    }

    @NonNull
    private MatrixCursor createCursor(int rowCount) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Table.COLUMN_ID,
                Table.COLUMN_NAME,
                Table.COLUMN_LAST_USED,
                Table.COLUMN_TIMES_USED
        }, rowCount);

        for (int i = 0; i < rowCount; i++) {
            cursor.addRow(Arrays.asList("1", "foo", "123", "2"));
        }

        return cursor;
    }

}