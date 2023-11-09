package org.wikipedia.dataclient.okhttp;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Response;

public class HttpStatusException extends IOException {
    private final int code;
    private final String url;
    public HttpStatusException(@NonNull Response rsp) {
        this.code = rsp.code();
        this.url = rsp.request().url().uri().toString();
        try {
            if (rsp.body() != null && rsp.body().contentType() != null
                    && rsp.body().contentType().toString().contains("json")) {
            }
        } catch (Exception e) {
            // Log?
        }
    }

    public int code() {
        return code;
    }

    @Override
    public String getMessage() {
        String str = "Code: " + code + ", URL: " + url;
        return str;
    }
}
