package fr.free.nrw.commons.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface MediaWikiApi {
    String getAuthCookie();

    void setAuthCookie(String authCookie);

    String login(String username, String password) throws IOException;

    String login(String username, String password, String twoFactorCode) throws IOException;

    boolean validateLogin() throws IOException;

    String getEditToken() throws IOException;

    boolean fileExistsWithName(String fileName) throws IOException;

    String findThumbnailByFilename(String filename) throws IOException;

    boolean logEvents(LogBuilder[] logBuilders);

    @NonNull
    UploadResult uploadFile(String filename, InputStream file, long dataLength, String pageContents, String editSummary, ProgressListener progressListener) throws IOException;

    @Nullable
    String edit(String editToken, String processedPageContent, String filename, String summary) throws IOException;

    @NonNull
    MediaResult fetchMediaByFilename(String filename) throws IOException;

    @NonNull
    List<String> searchCategories(int searchCatsLimit, String filterValue) throws IOException;

    @NonNull
    List<String> allCategories(int searchCatsLimit, String filter) throws IOException;

    @NonNull
    List<String> searchTitles(int searchCatsLimit, String title) throws IOException;

    @Nullable
    String revisionsByFilename(String filename) throws IOException;

    Observable<Boolean> existingFile(String fileSha1);

    @NonNull
    LogEventResult logEvents(String user, String lastModified, String queryContinue, int limit) throws IOException;

    Single<Integer> getUploadCount(String username);

    interface ProgressListener {
        void onProgress(long transferred, long total);
    }
}
