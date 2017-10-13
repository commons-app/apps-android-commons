package fr.free.nrw.commons.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

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
    Observable<String> searchCategories(String filterValue, int searchCatsLimit);

    @NonNull
    Observable<String> allCategories(String filter, int searchCatsLimit);

    @NonNull
    Observable<String> searchTitles(String title, int searchCatsLimit);

    @Nullable
    String revisionsByFilename(String filename) throws IOException;

    boolean existingFile(String fileSha1) throws IOException;

    @NonNull
    LogEventResult logEvents(String user, String lastModified, String queryContinue, int limit) throws IOException;

    @NonNull
    Single<Integer> getUploadCount(String userName);

    interface ProgressListener {
        void onProgress(long transferred, long total);
    }
}
