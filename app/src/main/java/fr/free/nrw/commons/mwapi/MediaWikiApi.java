package fr.free.nrw.commons.mwapi;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.notification.Notification;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface MediaWikiApi {

    String getAuthCookie();

    void setAuthCookie(String authCookie);

    String login(String username, String password) throws IOException;

    String login(String username, String password, String twoFactorCode) throws IOException;

    boolean validateLogin() throws IOException;

    String getEditToken() throws IOException;

    String getWikidataCsrfToken() throws IOException;

    String getCentralAuthToken() throws IOException;

    boolean fileExistsWithName(String fileName) throws IOException;

    Single<Boolean> pageExists(String pageName);

    List<String> getSubCategoryList(String categoryName);

    List<String> getParentCategoryList(String categoryName);

    @NonNull
    List<String> searchCategory(String title, int offset);

    @NonNull
    Single<UploadStash> uploadFile(String filename, InputStream file,
                                   long dataLength, Uri fileUri, Uri contentProviderUri,
                                   final ProgressListener progressListener);

    @NonNull
    Single<UploadResult> uploadFileFinalize(String filename, String filekey,
                                            String pageContents, String editSummary) throws IOException;
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

    Single<String> parseWikicode(String source);

    @NonNull
    Single<MediaResult> fetchMediaByFilename(String filename);

    @NonNull
    Observable<String> searchCategories(String filterValue, int searchCatsLimit);

    @NonNull
    Observable<String> allCategories(String filter, int searchCatsLimit);

    @NonNull
    List<Notification> getNotifications(boolean archived) throws IOException;

    @NonNull
    boolean markNotificationAsRead(Notification notification) throws IOException;

    @NonNull
    Observable<String> searchTitles(String title, int searchCatsLimit);

    @Nullable
    String revisionsByFilename(String filename) throws IOException;

    boolean existingFile(String fileSha1) throws IOException;

    @NonNull
    LogEventResult logEvents(String user, String lastModified, String queryContinue, int limit) throws IOException;

    boolean isUserBlockedFromCommons();

    void logout();

//    Single<CampaignResponseDTO> getCampaigns();

    boolean thank(String editToken, long revision) throws IOException;

    interface ProgressListener {
        void onProgress(long transferred, long total);
    }
}
