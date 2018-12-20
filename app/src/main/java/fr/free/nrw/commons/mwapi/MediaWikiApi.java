package fr.free.nrw.commons.mwapi;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import fr.free.nrw.commons.campaigns.CampaignResponseDTO;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.achievements.FeedbackResponse;
import fr.free.nrw.commons.notification.Notification;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface MediaWikiApi {
    String getUserAgent();

    String getAuthCookie();

    void setAuthCookie(String authCookie);

    String login(String username, String password) throws IOException;

    String login(String username, String password, String twoFactorCode) throws IOException;

    boolean validateLogin() throws IOException;

    String getEditToken() throws IOException;

    String getWikidataCsrfToken() throws IOException;

    String getCentralAuthToken() throws IOException;

    boolean fileExistsWithName(String fileName) throws IOException;

    boolean pageExists(String pageName) throws IOException;

    String findThumbnailByFilename(String filename) throws IOException;

    boolean logEvents(LogBuilder[] logBuilders);

    List<Media> getCategoryImages(String categoryName);

    List<String> getSubCategoryList(String categoryName);

    List<String> getParentCategoryList(String categoryName);

    @NonNull
    List<Media> searchImages(String title, int offset);

    @NonNull
    List<String> searchCategory(String title, int offset);

    @NonNull
    UploadResult uploadFile(String filename, InputStream file, long dataLength, String pageContents, String editSummary, Uri fileUri, Uri contentProviderUri, ProgressListener progressListener) throws IOException;

    @Nullable
    String edit(String editToken, String processedPageContent, String filename, String summary) throws IOException;

    @Nullable
    String prependEdit(String editToken, String processedPageContent, String filename, String summary) throws IOException;

    @Nullable
    String appendEdit(String editToken, String processedPageContent, String filename, String summary) throws IOException;

    @Nullable
    String wikidatCreateClaim(String entityId, String property, String snaktype, String value) throws IOException;

    @Nullable
    boolean addWikidataEditTag(String revisionId) throws IOException;

    @NonNull
    MediaResult fetchMediaByFilename(String filename) throws IOException;

    @NonNull
    Observable<String> searchCategories(String filterValue, int searchCatsLimit);

    @NonNull
    Observable<String> allCategories(String filter, int searchCatsLimit);

    @NonNull
    List<Notification> getNotifications() throws IOException;

    @NonNull
    Observable<String> searchTitles(String title, int searchCatsLimit);

    @Nullable
    String revisionsByFilename(String filename) throws IOException;

    boolean existingFile(String fileSha1) throws IOException;

    @NonNull
    LogEventResult logEvents(String user, String lastModified, String queryContinue, int limit) throws IOException;

    @NonNull
    Single<Integer> getUploadCount(String userName);

    boolean isUserBlockedFromCommons();

    Single<FeedbackResponse> getAchievements(String userName);

    Single<Media> getPictureOfTheDay();

    void logout();

    Single<CampaignResponseDTO> getCampaigns();

    interface ProgressListener {
        void onProgress(long transferred, long total);
    }
}
