package fr.free.nrw.commons.notification;

import android.support.annotation.Nullable;

public class MarkReadResponse {
    @SuppressWarnings("unused") @Nullable
    private String result;

    public String result() {
        return result;
    }

    public static class QueryMarkReadResponse {
        @SuppressWarnings("unused") @Nullable private MarkReadResponse echomarkread;
    }
}
