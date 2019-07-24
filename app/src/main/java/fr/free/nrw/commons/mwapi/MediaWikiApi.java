package fr.free.nrw.commons.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import io.reactivex.Single;

public interface MediaWikiApi {

    Single<String> parseWikicode(String source);

    @NonNull
    Single<MediaResult> fetchMediaByFilename(String filename);

    @NonNull
    LogEventResult logEvents(String user, String lastModified, String queryContinue, int limit) throws IOException;

    boolean isUserBlockedFromCommons();

    void logout();

    interface ProgressListener {
        void onProgress(long transferred, long total);
    }
}
