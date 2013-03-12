package org.wikimedia.commons;

import android.os.AsyncTask;
import android.os.Build;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.net.URLCodec;
import org.w3c.dom.Node;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static Date parseMWDate(String mwDate) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Assuming MW always gives me UTC
        try {
            return isoFormat.parse(mwDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toMWDate(Date date) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Assuming MW always gives me UTC
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return isoFormat.format(date);
    }

    public static String makeThumbBaseUrl(String filename) {
        String name = filename.replaceFirst("File:", "").replace(" ", "_");
        String sha = new String(Hex.encodeHex(DigestUtils.md5(name)));
        return String.format("%s/%s/%s/%s", CommonsApplication.IMAGE_URL_BASE, sha.substring(0, 1), sha.substring(0, 2), name);
    }


    public static String getStringFromDOM(Node dom) {
        javax.xml.transform.Transformer transformer = null;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        StringWriter outputStream = new StringWriter();
        javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(dom);
        javax.xml.transform.stream.StreamResult strResult = new javax.xml.transform.stream.StreamResult(outputStream);

        try {
            transformer.transform(domSource, strResult);
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return outputStream.toString();
    }

    static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task,
                                            T... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        }
        else {
            task.execute(params);
        }
    }

    private static final URLCodec urlCodec = new URLCodec();

    public static String urlEncode(String url) {
        try {
            return urlCodec.encode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static long countBytes(InputStream stream) throws IOException {
        long count = 0;
        BufferedInputStream bis = new BufferedInputStream(stream);
        while(bis.read() != -1) {
            count++;
        }
        return count;
    }

    public static String makeThumbUrl(String imageUrl, String filename, int width) {
        // Ugly Hack!
        // Update: OH DEAR GOD WHAT A HORRIBLE HACK I AM SO SORRY
        String thumbUrl = imageUrl.replaceFirst("test/", "test/thumb/").replace("commons/", "commons/thumb/") + "/" + width + "px-" + filename.replaceAll("File:", "").replaceAll(" ", "_");
        if(thumbUrl.endsWith("jpg") || thumbUrl.endsWith("png") || thumbUrl.endsWith("jpeg")) {
            return thumbUrl;
        } else {
            return thumbUrl + ".png";
        }
    }

    public static String capitalize(String string) {
        return string.substring(0,1).toUpperCase() + string.substring(1);
    }

}
