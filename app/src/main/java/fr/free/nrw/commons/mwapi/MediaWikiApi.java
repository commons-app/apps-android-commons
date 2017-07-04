package fr.free.nrw.commons.mwapi;

import org.mediawiki.api.ApiResult;

import java.io.IOException;
import java.io.InputStream;

import in.yuvi.http.fluent.ProgressListener;

public interface MediaWikiApi {
    String getAuthCookie();

    void setAuthCookie(String authCookie);

    String login(String username, String password) throws IOException;

    String login(String username, String password, String twoFactorCode) throws IOException;

    boolean validateLogin() throws IOException;

    String getEditToken() throws IOException;

    ApiResult upload(String filename, InputStream file, long dataLength, String pageContents, String editSummary, ProgressListener progressListener) throws IOException;

    boolean fileExistsWithName(String fileName) throws IOException;

    ApiResult edit(String editToken, String processedPageContent, String filename, String summary) throws IOException;

    String findThumbnailByFilename(String filename) throws IOException;

    ApiResult fetchMediaByFilename(String filename) throws IOException;

    ApiResult searchCategories(int searchCatsLimit, String filterValue) throws IOException;

    ApiResult allCategories(int searchCatsLimit, String filter) throws IOException;

    ApiResult searchTitles(int searchCatsLimit, String title) throws IOException;

    ApiResult logEvents(String user, String lastModified, String queryContinue, int limit) throws IOException;

    ApiResult revisionsByFilename(String filename) throws IOException;

    ApiResult existingFile(String fileSha1) throws IOException;

    boolean logEvents(LogBuilder[] logBuilders);
}
