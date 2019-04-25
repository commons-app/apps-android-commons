package fr.free.nrw.commons.bookmarks.pictures;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.bookmarks.Bookmark;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.Single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for bookmark pictures controller
 */
public class BookmarkPicturesControllerTest {

    @Mock
    OkHttpJsonApiClient okHttpJsonApiClient;
    @Mock
    BookmarkPicturesDao bookmarkDao;

    @InjectMocks
    BookmarkPicturesController bookmarkPicturesController;


    /**
     * Init mocks
     */
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Media mockMedia = getMockMedia();
        when(bookmarkDao.getAllBookmarks())
                .thenReturn(getMockBookmarkList());
        when(okHttpJsonApiClient.getMedia(anyString(), anyBoolean()))
                .thenReturn(Single.just(mockMedia));
    }

    /**
     * Get mock bookmark list
     * @return
     */
    private List<Bookmark> getMockBookmarkList() {
        ArrayList<Bookmark> list = new ArrayList<>();
        list.add(new Bookmark("File:Test1.jpg", "Maskaravivek", Uri.EMPTY));
        list.add(new Bookmark("File:Test2.jpg", "Maskaravivek", Uri.EMPTY));
        return list;
    }

    /**
     * Test case where all bookmark pictures are fetched and media is found against it
     */
    @Test
    public void loadBookmarkedPictures() {
        List<Media> bookmarkedPictures = bookmarkPicturesController.loadBookmarkedPictures().blockingGet();
        assertEquals(2, bookmarkedPictures.size());
    }

    /**
     * Test case where all bookmark pictures are fetched and only one media is found
     */
    @Test
    public void loadBookmarkedPicturesForNullMedia() {
        when(okHttpJsonApiClient.getMedia("File:Test1.jpg", false))
                .thenReturn(Single.error(new NullPointerException("Error occurred")));
        when(okHttpJsonApiClient.getMedia("File:Test2.jpg", false))
                .thenReturn(Single.just(getMockMedia()));
        List<Media> bookmarkedPictures = bookmarkPicturesController.loadBookmarkedPictures().blockingGet();
        assertEquals(1, bookmarkedPictures.size());
    }

    private Media getMockMedia() {
        return new Media("File:Test.jpg");
    }

    /**
     * Test case where current bookmarks don't match the bookmarks in DB
     */
    @Test
    public void needRefreshBookmarkedPictures() {
        boolean needRefreshBookmarkedPictures = bookmarkPicturesController.needRefreshBookmarkedPictures();
        assertTrue(needRefreshBookmarkedPictures);
    }

    /**
     * Test case where the DB is up to date with the bookmarks loaded in the list
     */
    @Test
    public void doNotNeedRefreshBookmarkedPictures() {
        List<Media> bookmarkedPictures = bookmarkPicturesController.loadBookmarkedPictures().blockingGet();
        assertEquals(2, bookmarkedPictures.size());
        boolean needRefreshBookmarkedPictures = bookmarkPicturesController.needRefreshBookmarkedPictures();
        assertFalse(needRefreshBookmarkedPictures);
    }
}