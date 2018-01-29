package fr.free.nrw.commons.contributions;

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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Date;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.TestCommonsApplication;
import fr.free.nrw.commons.Utils;

import static fr.free.nrw.commons.contributions.Contribution.SOURCE_CAMERA;
import static fr.free.nrw.commons.contributions.Contribution.SOURCE_GALLERY;
import static fr.free.nrw.commons.contributions.Contribution.STATE_COMPLETED;
import static fr.free.nrw.commons.contributions.Contribution.STATE_QUEUED;
import static fr.free.nrw.commons.contributions.ContributionDao.*;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.BASE_URI;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.uriForId;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = TestCommonsApplication.class)
public class ContributionDaoTest {

    private static final String LOCAL_URI = "http://example.com/";
    @Mock
    ContentProviderClient client;
    @Mock
    SQLiteDatabase database;
    @Captor
    ArgumentCaptor<ContentValues> captor;

    private Uri contentUri;
    private ContributionDao testObject;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        contentUri = uriForId(111);

        testObject = new ContributionDao(client);
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
    public void upgradeDatabase_v1_to_v2() {
        Table.onUpdate(database, 1, 2);

        InOrder inOrder = Mockito.inOrder(database);
        inOrder.verify(database).execSQL(Table.ADD_DESCRIPTION_FIELD);
        inOrder.verify(database).execSQL(Table.ADD_CREATOR_FIELD);
    }

    @Test
    public void upgradeDatabase_v2_to_v3() {
        Table.onUpdate(database, 2, 3);

        InOrder inOrder = Mockito.inOrder(database);
        inOrder.verify(database).execSQL(Table.ADD_MULTIPLE_FIELD);
        inOrder.verify(database).execSQL(Table.SET_DEFAULT_MULTIPLE);
    }

    @Test
    public void upgradeDatabase_v3_to_v4() {
        Table.onUpdate(database, 3, 4);

        // No changes
        verifyZeroInteractions(database);
    }

    @Test
    public void upgradeDatabase_v4_to_v5() {
        Table.onUpdate(database, 4, 5);

        // No changes
        verifyZeroInteractions(database);
    }

    @Test
    public void upgradeDatabase_v5_to_v6() {
        Table.onUpdate(database, 5, 6);

        InOrder inOrder = Mockito.inOrder(database);
        inOrder.verify(database).execSQL(Table.ADD_WIDTH_FIELD);
        inOrder.verify(database).execSQL(Table.SET_DEFAULT_WIDTH);
        inOrder.verify(database).execSQL(Table.ADD_HEIGHT_FIELD);
        inOrder.verify(database).execSQL(Table.SET_DEFAULT_HEIGHT);
        inOrder.verify(database).execSQL(Table.ADD_LICENSE_FIELD);
        inOrder.verify(database).execSQL(Table.SET_DEFAULT_LICENSE);
    }

    @Test
    public void saveNewContribution_nonNullFields() throws Exception {
        when(client.insert(isA(Uri.class), isA(ContentValues.class))).thenReturn(contentUri);
        Contribution contribution = createContribution(true, null, null, null, null);

        testObject.save(contribution);

        assertEquals(contentUri, contribution.getContentUri());
        verify(client).insert(eq(BASE_URI), captor.capture());
        ContentValues cv = captor.getValue();

        // Long fields
        assertEquals(222L, cv.getAsLong(Table.COLUMN_LENGTH).longValue());
        assertEquals(321L, cv.getAsLong(Table.COLUMN_TIMESTAMP).longValue());
        assertEquals(333L, cv.getAsLong(Table.COLUMN_TRANSFERRED).longValue());

        // Integer fields
        assertEquals(STATE_COMPLETED, cv.getAsInteger(Table.COLUMN_STATE).intValue());
        assertEquals(640, cv.getAsInteger(Table.COLUMN_WIDTH).intValue());
        assertEquals(480, cv.getAsInteger(Table.COLUMN_HEIGHT).intValue());

        // String fields
        assertEquals(SOURCE_CAMERA, cv.getAsString(Table.COLUMN_SOURCE));
        assertEquals("desc", cv.getAsString(Table.COLUMN_DESCRIPTION));
        assertEquals("create", cv.getAsString(Table.COLUMN_CREATOR));
        assertEquals("007", cv.getAsString(Table.COLUMN_LICENSE));
    }

    @Test
    public void saveNewContribution_nullableFieldsAreNull() throws Exception {
        when(client.insert(isA(Uri.class), isA(ContentValues.class))).thenReturn(contentUri);
        Contribution contribution = createContribution(true, null, null, null, null);

        testObject.save(contribution);

        assertEquals(contentUri, contribution.getContentUri());
        verify(client).insert(eq(BASE_URI), captor.capture());
        ContentValues cv = captor.getValue();

        // Nullable fields are absent if null
        assertFalse(cv.containsKey(Table.COLUMN_LOCAL_URI));
        assertFalse(cv.containsKey(Table.COLUMN_IMAGE_URL));
        assertFalse(cv.containsKey(Table.COLUMN_UPLOADED));
    }

    @Test
    public void saveNewContribution_nullableImageUrlUsesFileAsBackup() throws Exception {
        when(client.insert(isA(Uri.class), isA(ContentValues.class))).thenReturn(contentUri);
        Contribution contribution = createContribution(true, null, null, null, "file");

        testObject.save(contribution);

        assertEquals(contentUri, contribution.getContentUri());
        verify(client).insert(eq(BASE_URI), captor.capture());
        ContentValues cv = captor.getValue();

        // Nullable fields are absent if null
        assertFalse(cv.containsKey(Table.COLUMN_LOCAL_URI));
        assertFalse(cv.containsKey(Table.COLUMN_UPLOADED));

        assertEquals(Utils.makeThumbBaseUrl("file"), cv.getAsString(Table.COLUMN_IMAGE_URL));
    }

    @Test
    public void saveNewContribution_nullableFieldsAreNonNull() throws Exception {
        when(client.insert(isA(Uri.class), isA(ContentValues.class))).thenReturn(contentUri);
        Contribution contribution = createContribution(true, Uri.parse(LOCAL_URI),
                "image", new Date(456L), null);

        testObject.save(contribution);

        assertEquals(contentUri, contribution.getContentUri());
        verify(client).insert(eq(BASE_URI), captor.capture());
        ContentValues cv = captor.getValue();

        assertEquals(LOCAL_URI, cv.getAsString(Table.COLUMN_LOCAL_URI));
        assertEquals("image", cv.getAsString(Table.COLUMN_IMAGE_URL));
        assertEquals(456L, cv.getAsLong(Table.COLUMN_UPLOADED).longValue());
    }

    @Test
    public void saveNewContribution_booleanEncodesTrue() throws Exception {
        when(client.insert(isA(Uri.class), isA(ContentValues.class))).thenReturn(contentUri);
        Contribution contribution = createContribution(true, null, null, null, null);

        testObject.save(contribution);

        assertEquals(contentUri, contribution.getContentUri());
        verify(client).insert(eq(BASE_URI), captor.capture());
        ContentValues cv = captor.getValue();

        // Boolean true --> 1 for ths encoding scheme
        assertEquals("Boolean true should be encoded as 1", 1,
                cv.getAsInteger(Table.COLUMN_MULTIPLE).intValue());
    }

    @Test
    public void saveNewContribution_booleanEncodesFalse() throws Exception {
        when(client.insert(isA(Uri.class), isA(ContentValues.class))).thenReturn(contentUri);
        Contribution contribution = createContribution(false, null, null, null, null);

        testObject.save(contribution);

        assertEquals(contentUri, contribution.getContentUri());
        verify(client).insert(eq(BASE_URI), captor.capture());
        ContentValues cv = captor.getValue();

        // Boolean true --> 1 for ths encoding scheme
        assertEquals("Boolean false should be encoded as 0", 0,
                cv.getAsInteger(Table.COLUMN_MULTIPLE).intValue());
    }

    @Test
    public void saveExistingContribution() throws Exception {
        Contribution contribution = createContribution(false, null, null, null, null);
        contribution.setContentUri(contentUri);

        testObject.save(contribution);

        verify(client).update(eq(contentUri), isA(ContentValues.class), isNull(String.class), isNull(String[].class));
    }

    @Test(expected = RuntimeException.class)
    public void saveTranslatesExceptions() throws Exception {
        when(client.insert(isA(Uri.class), isA(ContentValues.class))).thenThrow(new RemoteException(""));

        testObject.save(createContribution(false, null, null, null, null));
    }

    @Test(expected = RuntimeException.class)
    public void deleteTranslatesExceptions() throws Exception {
        when(client.delete(isA(Uri.class), any(), any())).thenThrow(new RemoteException(""));

        Contribution contribution = createContribution(false, null, null, null, null);
        contribution.setContentUri(contentUri);
        testObject.delete(contribution);
    }

    @Test(expected = RuntimeException.class)
    public void exceptionThrownWhenAttemptingToDeleteUnsavedContribution() {
        testObject.delete(createContribution(false, null, null, null, null));
    }

    @Test
    public void deleteExistingContribution() throws Exception {
        Contribution contribution = createContribution(false, null, null, null, null);
        contribution.setContentUri(contentUri);

        testObject.delete(contribution);

        verify(client).delete(eq(contentUri), isNull(String.class), isNull(String[].class));
    }

    @Test
    public void createFromCursor() {
        long created = 321L;
        long uploaded = 456L;
        MatrixCursor mc = createCursor(created, uploaded, false, LOCAL_URI);

        Contribution c = ContributionDao.fromCursor(mc);

        assertEquals(uriForId(111), c.getContentUri());
        assertEquals("file", c.getFilename());
        assertEquals(LOCAL_URI, c.getLocalUri().toString());
        assertEquals("image", c.getImageUrl());
        assertEquals(created, c.getTimestamp().getTime());
        assertEquals(created, c.getDateCreated().getTime());
        assertEquals(STATE_QUEUED, c.getState());
        assertEquals(222L, c.getDataLength());
        assertEquals(uploaded, c.getDateUploaded().getTime());
        assertEquals(88L, c.getTransferred());
        assertEquals(SOURCE_GALLERY, c.getSource());
        assertEquals("desc", c.getDescription());
        assertEquals("create", c.getCreator());
        assertEquals(640, c.getWidth());
        assertEquals(480, c.getHeight());
        assertEquals("007", c.getLicense());
    }

    @Test
    public void createFromCursor_nullableTimestamps() {
        MatrixCursor mc = createCursor(0L, 0L, false, LOCAL_URI);

        Contribution c = ContributionDao.fromCursor(mc);

        assertNull(c.getTimestamp());
        assertNull(c.getDateCreated());
        assertNull(c.getDateUploaded());
    }

    @Test
    public void createFromCursor_nullableLocalUri() {
        MatrixCursor mc = createCursor(0L, 0L, false, "");

        Contribution c = ContributionDao.fromCursor(mc);

        assertNull(c.getLocalUri());
        assertNull(c.getDateCreated());
        assertNull(c.getDateUploaded());
    }

    @Test
    public void createFromCursor_booleanEncoding() {
        MatrixCursor mcFalse = createCursor(0L, 0L, false, LOCAL_URI);
        assertFalse(ContributionDao.fromCursor(mcFalse).getMultiple());

        MatrixCursor mcHammer = createCursor(0L, 0L, true, LOCAL_URI);
        assertTrue(ContributionDao.fromCursor(mcHammer).getMultiple());
    }

    @NonNull
    private MatrixCursor createCursor(long created, long uploaded, boolean multiple, String localUri) {
        MatrixCursor mc = new MatrixCursor(Table.ALL_FIELDS, 1);
        mc.addRow(Arrays.asList("111", "file", localUri, "image",
                created, STATE_QUEUED, 222L, uploaded, 88L, SOURCE_GALLERY, "desc",
                "create", multiple ? 1 : 0, 640, 480, "007"));
        mc.moveToFirst();
        return mc;
    }

    @NonNull
    private Contribution createContribution(boolean multiple, Uri localUri,
                                            String imageUrl, Date dateUploaded, String filename) {
        Contribution contribution = new Contribution(localUri, imageUrl, filename, "desc", 222L,
                new Date(321L), dateUploaded, "create", "edit", "coords");
        contribution.setState(STATE_COMPLETED);
        contribution.setTransferred(333L);
        contribution.setSource(SOURCE_CAMERA);
        contribution.setLicense("007");
        contribution.setMultiple(multiple);
        contribution.setTimestamp(new Date(321L));
        contribution.setWidth(640);
        contribution.setHeight(480);  // VGA should be enough for anyone, right?
        return contribution;
    }
}