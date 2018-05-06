package fr.free.nrw.commons.mwapi.request;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.annotation.Nullable;

import fr.free.nrw.commons.mwapi.request.RequestBuilder.InputStreamDescriptor;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

class PostBuilder<T> extends AbstractBuilder<T> {
    PostBuilder(OkHttpClient okHttpClient, Gson gsonParser, HttpUrl parsedApiEndpoint, Class<T> returnClass) {
        super(okHttpClient, gsonParser, parsedApiEndpoint, returnClass);
    }

    @Override
    protected Response getResponse() throws IOException {
        return okHttpClient.newCall(
                new Request.Builder()
                        .url(parsedApiEndpoint)
                        .post(buildMultipartBody())
                        .build()
        ).execute();
    }

    private MultipartBody buildMultipartBody() {
        MultipartBody.Builder body = new MultipartBody.Builder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                body.addFormDataPart(entry.getKey(), (String) value);
            } else if (value instanceof InputStreamDescriptor) {
                InputStreamDescriptor descriptor = (InputStreamDescriptor) value;
                InputStreamRequestBody inputStreamRequestBody = new InputStreamRequestBody(
                        MediaType.parse(descriptor.mediaType), descriptor.inputStream,
                        descriptor.totalSize);
                body.addFormDataPart(entry.getKey(), descriptor.filename, inputStreamRequestBody);
            }
        }
        body.setType(MultipartBody.FORM);
        return body.build();
    }

    private class InputStreamRequestBody extends RequestBody {
        private final MediaType mediaType;
        private final InputStream inputStream;
        private long totalSize;

        InputStreamRequestBody(MediaType mediaType, InputStream inputStream, long totalSize) {
            this.mediaType = mediaType;
            this.inputStream = inputStream;
            this.totalSize = totalSize;
        }

        @Override
        public long contentLength() throws IOException {
            return totalSize;
        }

        @Nullable
        @Override
        public MediaType contentType() {
            return mediaType;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            Source source = null;
            try {
                source = Okio.source(inputStream);
                long total = 0;
                long read;

                while ((read = source.read(sink.buffer(), 2048)) != -1) {
                    total += read;
                    sink.flush();
                    listener.onProgress(total, totalSize);

                }
            } finally {
                Util.closeQuietly(source);
            }
        }
//        @Override
//        public void writeTo(@NonNull BufferedSink sink) throws IOException {
//            Source source = null;
//            try {
//                source = Okio.source(new WrappedInputStream(inputStream, totalSize));
//                sink.writeAll(source);
//            } finally {
//                Util.closeQuietly(source);
//            }
//        }
    }

//    private class WrappedInputStream extends InputStream {
//        private long sent = 0;
//        private long total = 0;
//        private InputStream inputStream;
//
//        WrappedInputStream(InputStream inputStream, long total) {
//            this.inputStream = inputStream;
//            this.total = total;
//        }
//
//        @Override
//        public int read(@NonNull byte[] b, int off, int len) throws IOException {
//            int read = inputStream.read(b, off, len);
//            if (read > -1) {
//                sent += read;
//            }
//            Log.e("MW", "### Upload " + sent + " / " + total);
//            listener.onProgress(sent, total);
//            return read;
//        }
//
//        @Override
//        public void close() throws IOException {
//            inputStream.close();
//        }
//
//        @Override
//        public int read() throws IOException {
//            return 0;
//        }
//    }
}
