package fr.free.nrw.commons;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.net.URLCodec;
import org.w3c.dom.Node;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import fr.free.nrw.commons.upload.ShareActivity;

public class Utils {

    private static final String TAG = Utils.class.getName();

    // Get SHA1 of file from input stream
    public static String getSHA1(InputStream is) {

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Exception while getting Digest", e);
            return "";
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 40 chars
            output = String.format("%40s", output).replace(' ', '0');
            Log.i(TAG, "File SHA1: " + output);

            return output;
        } catch (IOException e) {
            Log.e(TAG, "IO Exception", e);
            return "";
        }  finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception on closing MD5 input stream", e);
            }
        }
    }

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
        return String.format("%s/%s/%s/%s", CommonsApplication.IMAGE_URL_BASE, sha.substring(0, 1), sha.substring(0, 2), urlEncode(name));
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

    static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, Executor executor,
                                            T... params) {
        // FIXME: We're simply ignoring the executor on older androids
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(executor, params);
        }
        else {
            task.execute(params);
        }
    }

    private static DisplayImageOptions.Builder defaultImageOptionsBuilder;
    public static DisplayImageOptions.Builder getGenericDisplayOptions() {
        if(defaultImageOptionsBuilder == null) {
            defaultImageOptionsBuilder = new DisplayImageOptions.Builder().cacheInMemory()
                    .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                // List views flicker badly during data updates on Android 2.3; we
                // haven't quite figured out why but cells seem to be rearranged oddly.
                // Disable the fade-in on 2.3 to reduce the effect.
                defaultImageOptionsBuilder = defaultImageOptionsBuilder
                        .displayer(new FadeInBitmapDisplayer(300));
            }
            defaultImageOptionsBuilder = defaultImageOptionsBuilder
                    .cacheInMemory()
                    .resetViewBeforeLoading();
        }
        return defaultImageOptionsBuilder;
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
        if(imageUrl.endsWith("webm")) {
            return imageUrl.replaceFirst("test/", "test/thumb/").replace("commons/", "commons/thumb/") + "/" + width + "px--" + filename.replaceAll("File:", "").replaceAll(" ", "_") + ".jpg";
        } else {
            String thumbUrl = imageUrl.replaceFirst("test/", "test/thumb/").replace("commons/", "commons/thumb/") + "/" + width + "px-" + filename.replaceAll("File:", "").replaceAll(" ", "_");
            if(thumbUrl.endsWith("jpg") || thumbUrl.endsWith("png") || thumbUrl.endsWith("jpeg")) {
                return thumbUrl;
            } else {
                return thumbUrl + ".png";
            }
        }
    }

    public static String capitalize(String string) {
        return string.substring(0,1).toUpperCase() + string.substring(1);
    }

    public static String licenseTemplateFor(String license) {
        if(license.equals(Prefs.Licenses.CC_BY)) {
            return "{{self|cc-by-3.0}}";
        } else if(license.equals(Prefs.Licenses.CC_BY_SA)) {
            return "{{self|cc-by-sa-3.0}}";
        } else if(license.equals(Prefs.Licenses.CC0)) {
            return "{{self|cc-zero}}";
        }
        throw new RuntimeException("Unrecognized license value");
    }

    public static int licenseNameFor(String license) {
        if(license.equals(Prefs.Licenses.CC_BY)) {
            return R.string.license_name_cc_by;
        } else if(license.equals(Prefs.Licenses.CC_BY_SA)) {
            return R.string.license_name_cc_by_sa;
        } else if(license.equals(Prefs.Licenses.CC0)) {
            return R.string.license_name_cc0;
        }
        throw new RuntimeException("Unrecognized license value");
    }

    public static String licenseUrlFor(String license) {
        if(license.equals(Prefs.Licenses.CC_BY)) {
            return "https://creativecommons.org/licenses/by/3.0/";
        } else if(license.equals(Prefs.Licenses.CC_BY_SA)) {
            return "https://creativecommons.org/licenses/by-sa/3.0/";
        } else if(license.equals(Prefs.Licenses.CC0)) {
            return "https://creativecommons.org/publicdomain/zero/1.0/";
        }
        throw new RuntimeException("Unrecognized license value");
    }

    public static String implode(String glue, Iterable<String> pieces) {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for (String piece : pieces) {
            if (first) {
                first = false;
            } else {
                buffer.append(glue);
            }
            buffer.append(pieces);
        }
        return buffer.toString();
    }

    public static Uri uriForWikiPage(String name) {
        String underscored = name.trim().replace(" ", "_");
        String uriStr = CommonsApplication.HOME_URL + urlEncode(underscored);
        return Uri.parse(uriStr);
    }

    /**
     * Fast-forward an XmlPullParser to the next instance of the given element
     * in the input stream (namespaced).
     *
     * @param parser
     * @param namespace
     * @param element
     * @return true on match, false on failure
     */
    public static boolean xmlFastForward(XmlPullParser parser, String namespace, String element) {
        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG &&
                        parser.getNamespace().equals(namespace) &&
                        parser.getName().equals(element)) {
                    // We found it!
                    return true;
                }
            }
            return false;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String fixExtension(String title, String extension) {
        Pattern jpegPattern = Pattern.compile("\\.jpeg$", Pattern.CASE_INSENSITIVE);

        // People are used to ".jpg" more than ".jpeg" which the system gives us.
        if (extension != null && extension.toLowerCase().equals("jpeg")) {
            extension = "jpg";
        }
        title = jpegPattern.matcher(title).replaceFirst(".jpg");
        if (extension != null && !title.toLowerCase().endsWith("." + extension.toLowerCase())) {
            title += "." + extension;
        }
        return title;
    }

    private static int badCode(int x) { return (x * 2);
        // DELETE ME
    }
}
