package fr.free.nrw.commons.contributions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import fr.free.nrw.commons.PageTitle;
import io.reactivex.Single;

class RxJava2Tasks {

    private static final String UPLOAD_COUNT_URL_TEMPLATE =
            "https://tools.wmflabs.org/urbanecmbot/uploadsbyuser/uploadsbyuser.py?user=%s";

    static Single<Integer> getUploadCount(String userName) {
        return Single.fromCallable(() -> {
            URL url = new URL(String.format(Locale.ENGLISH, UPLOAD_COUNT_URL_TEMPLATE,
                    new PageTitle(userName).getText()));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new
                    InputStreamReader(urlConnection.getInputStream()));
            String uploadCount = bufferedReader.readLine();
            bufferedReader.close();
            urlConnection.disconnect();
            return Integer.parseInt(uploadCount);
        });
    }
}
