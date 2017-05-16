package fr.free.nrw.commons.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import timber.log.Timber;

public class FileUtils {
    public static String readFromFile(Context context, String fileName) {
        String stringBuilder = "";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(fileName)));
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                stringBuilder += mLine + "\n";
            }
        } catch (IOException e) {
            Timber.e("File not found exception", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
        return stringBuilder;
    }
}
