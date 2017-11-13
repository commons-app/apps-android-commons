package fr.free.nrw.commons;

import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spanned;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Node;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import fr.free.nrw.commons.settings.Prefs;
import timber.log.Timber;

public class Utils {

    // Get SHA1 of file from input stream
    public static String getSHA1(InputStream is) {

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            Timber.e(e, "Exception while getting Digest");
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
            Timber.i("File SHA1: %s", output);

            return output;
        } catch (IOException e) {
            Timber.e(e, "IO Exception");
            return "";
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Timber.e(e, "Exception on closing MD5 input stream");
            }
        }
    }

    /**
     * Fix Html.fromHtml is deprecated problem
     *
     * @param source provided Html string
     * @return returned Spanned of appropriate method according to version check
     */
    public static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            return Html.fromHtml(source);
        }
    }

    /**
     * Strips localization symbols from a string.
     * Removes the suffix after "@" and quotes.
     *
     * @param s string possibly containing localization symbols
     * @return stripped string
     */
    public static String stripLocalizedString(String s) {
        Matcher matcher = Pattern.compile("\\\"(.*)\\\"(@\\w+)?").matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return s;
        }
    }

    public static Date parseMWDate(String mwDate) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH); // Assuming MW always gives me UTC
        try {
            return isoFormat.parse(mwDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toMWDate(Date date) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH); // Assuming MW always gives me UTC
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return isoFormat.format(date);
    }

    public static String makeThumbBaseUrl(@NonNull String filename) {
        String name = new PageTitle(filename).getPrefixedText();
        String sha = new String(Hex.encodeHex(DigestUtils.md5(name)));
        return String.format("%s/%s/%s/%s", BuildConfig.IMAGE_URL_BASE, sha.substring(0, 1), sha.substring(0, 2), urlEncode(name));
    }

    public static String getStringFromDOM(Node dom) {
        Transformer transformer = null;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        StringWriter outputStream = new StringWriter();
        DOMSource domSource = new DOMSource(dom);
        StreamResult strResult = new StreamResult(outputStream);

        try {
            transformer.transform(domSource, strResult);
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return outputStream.toString();
    }

    public static String urlEncode(String url) {
        try {
            return URLEncoder.encode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static long countBytes(InputStream stream) throws IOException {
        long count = 0;
        BufferedInputStream bis = new BufferedInputStream(stream);
        while (bis.read() != -1) {
            count++;
        }
        return count;
    }

    public static String capitalize(String string) {
        return string.substring(0, 1).toUpperCase(Locale.getDefault()) + string.substring(1);
    }

    public static String licenseTemplateFor(String license) {
        switch (license) {
            case Prefs.Licenses.CC_BY_3:
                return "{{self|cc-by-3.0}}";
            case Prefs.Licenses.CC_BY_4:
                return "{{self|cc-by-4.0}}";
            case Prefs.Licenses.CC_BY_SA_3:
                return "{{self|cc-by-sa-3.0}}";
            case Prefs.Licenses.CC_BY_SA_4:
                return "{{self|cc-by-sa-4.0}}";
            case Prefs.Licenses.CC0:
                return "{{self|cc-zero}}";
            case Prefs.Licenses.CC_BY:
                return "{{self|cc-by-3.0}}";
            case Prefs.Licenses.CC_BY_SA:
                return "{{self|cc-by-sa-3.0}}";
        }
        throw new RuntimeException("Unrecognized license value: " + license);
    }

    public static int licenseNameFor(String license) {
        switch (license) {
            case Prefs.Licenses.CC_BY_3:
                return R.string.license_name_cc_by;
            case Prefs.Licenses.CC_BY_4:
                return R.string.license_name_cc_by_four;
            case Prefs.Licenses.CC_BY_SA_3:
                return R.string.license_name_cc_by_sa;
            case Prefs.Licenses.CC_BY_SA_4:
                return R.string.license_name_cc_by_sa_four;
            case Prefs.Licenses.CC0:
                return R.string.license_name_cc0;
            case Prefs.Licenses.CC_BY:  // for backward compatibility to v2.1
                return R.string.license_name_cc_by_3_0;
            case Prefs.Licenses.CC_BY_SA:  // for backward compatibility to v2.1
                return R.string.license_name_cc_by_sa_3_0;
        }
        throw new RuntimeException("Unrecognized license value: " + license);
    }

    public static String licenseUrlFor(String license) {
        switch (license) {
            case Prefs.Licenses.CC_BY_3:
                return "https://creativecommons.org/licenses/by/3.0/";
            case Prefs.Licenses.CC_BY_4:
                return "https://creativecommons.org/licenses/by/4.0/";
            case Prefs.Licenses.CC_BY_SA_3:
                return "https://creativecommons.org/licenses/by-sa/3.0/";
            case Prefs.Licenses.CC_BY_SA_4:
                return "https://creativecommons.org/licenses/by-sa/4.0/";
            case Prefs.Licenses.CC0:
                return "https://creativecommons.org/publicdomain/zero/1.0/";
        }
        throw new RuntimeException("Unrecognized license value: " + license);
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
                if (parser.getEventType() == XmlPullParser.START_TAG
                        && parser.getNamespace().equals(namespace)
                        && parser.getName().equals(element)) {
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
        if (extension != null && extension.toLowerCase(Locale.ENGLISH).equals("jpeg")) {
            extension = "jpg";
        }
        title = jpegPattern.matcher(title).replaceFirst(".jpg");
        if (extension != null && !title.toLowerCase(Locale.getDefault())
                .endsWith("." + extension.toLowerCase(Locale.ENGLISH))) {
            title += "." + extension;
        }
        return title;
    }

    public static boolean isNullOrWhiteSpace(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isDarkTheme(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("theme", false);
    }

    /**
     * Will be used to fetch the logs generated by the app ever since the beginning of times....
     * i.e. since the time the app started.
     *
     * @return String containing all the logs since the time the app started
     */
    public static String getAppLogs() {
        final String processId = Integer.toString(android.os.Process.myPid());

        StringBuilder stringBuilder = new StringBuilder();

        try {
            String[] command = new String[] {"logcat","-d","-v","threadtime"};

            Process process = Runtime.getRuntime().exec(command);

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(processId)) {
                    stringBuilder.append(line);
                }
            }
        } catch (IOException ioe) {
            Timber.e("getAppLogs failed", ioe);
        }

        return stringBuilder.toString();
    }
}
