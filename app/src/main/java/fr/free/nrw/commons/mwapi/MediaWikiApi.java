package fr.free.nrw.commons.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import io.reactivex.Single;

public interface MediaWikiApi {

    Single<String> parseWikicode(String source);

    @NonNull
    Single<MediaResult> fetchMediaByFilename(String filename);

    boolean isUserBlockedFromCommons();

    void logout();

//    Single<CampaignResponseDTO> getCampaigns();

    interface ProgressListener {
        void onProgress(long transferred, long total);
    }
}
