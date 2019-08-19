package fr.free.nrw.commons.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface MediaWikiApi {

    /**
     * @return File entity id obtained after uploading the image
     *
     * @param fileName title of the image uploaded
     */
    String getFileEntityId(String fileName) throws IOException;

    Single<String> parseWikicode(String source);

    @NonNull
    Single<MediaResult> fetchMediaByFilename(String filename);

    @Nullable
    String revisionsByFilename(String filename) throws IOException;

    @NonNull
    LogEventResult logEvents(String user, String lastModified, String queryContinue, int limit) throws IOException;

    boolean isUserBlockedFromCommons();

    void logout();

//    Single<CampaignResponseDTO> getCampaigns();

    interface ProgressListener {
        void onProgress(long transferred, long total);
    }
}
