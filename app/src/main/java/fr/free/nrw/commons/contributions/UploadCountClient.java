package fr.free.nrw.commons.contributions;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import fr.free.nrw.commons.PageTitle;
import fr.free.nrw.commons.concurrency.BackgroundPoolExceptionHandler;
import fr.free.nrw.commons.concurrency.ThreadPoolExecutorService;
import timber.log.Timber;

class UploadCountClient {
    private ThreadPoolExecutorService threadPoolExecutor;

    UploadCountClient() {
        threadPoolExecutor = new ThreadPoolExecutorService.Builder("bg-pool")
                .setPoolSize(Runtime.getRuntime().availableProcessors())
                .setExceptionHandler(new BackgroundPoolExceptionHandler())
                .build();
    }

    private static final String UPLOAD_COUNT_URL_TEMPLATE =
            "https://tools.wmflabs.org/urbanecmbot/uploadsbyuser/uploadsbyuser.py?user=%s";

    ListenableFuture<Integer> getUploadCount(final String userName) {
        final SettableFuture<Integer> future = SettableFuture.create();
        threadPoolExecutor.schedule(() -> {
            URL url;
            try {
                url = new URL(String.format(Locale.ENGLISH, UPLOAD_COUNT_URL_TEMPLATE,
                        new PageTitle(userName).getText()));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new
                            InputStreamReader(urlConnection.getInputStream()));
                    String uploadCount = bufferedReader.readLine();
                    bufferedReader.close();
                    future.set(Integer.parseInt(uploadCount));
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Timber.e("Error getting upload count Error", e);
                future.setException(e);
            }
        });
        return future;
    }
}
