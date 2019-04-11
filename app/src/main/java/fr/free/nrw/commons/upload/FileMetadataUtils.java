package fr.free.nrw.commons.upload;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import timber.log.Timber;

import static androidx.exifinterface.media.ExifInterface.*;

public class FileMetadataUtils {

    public static Observable<String> getTagsFromPref(String pref) {
        Timber.d("Retuning tags for pref:%s", pref);
        switch (pref) {
            case "Author":
                return Observable.fromArray(TAG_ARTIST, TAG_CAMARA_OWNER_NAME);
            case "Copyright":
                return Observable.fromArray(TAG_COPYRIGHT);
            case "Location":
                return Observable.fromArray(TAG_GPS_LATITUDE, TAG_GPS_ALTITUDE_REF,
                        TAG_GPS_LONGITUDE, TAG_GPS_LONGITUDE_REF);
            case "Camera Model":
                return Observable.fromArray(TAG_MAKE, TAG_MODEL);
            case "Lens Model":
                return Observable.fromArray(TAG_LENS_MAKE, TAG_LENS_MODEL, TAG_LENS_SPECIFICATION);
            case "Serial Numbers":
                return Observable.fromArray(TAG_BODY_SERIAL_NUMBER, TAG_LENS_SERIAL_NUMBER);
            case "Software":
                return Observable.fromArray(TAG_SOFTWARE);
            default:
                return null;
        }
    }

    /**
     * Removes all XMP data from the input file and writes the rest of the image to a new file.
     *
     * This works by black magic. Please read the JPEG section of the XMP Specification Part 3 before making changes.
     * https://wwwimages2.adobe.com/content/dam/acom/en/devnet/xmp/pdfs/XMP%20SDK%20Release%20cc-2016-08/XMPSpecificationPart3.pdf
     *
     * @param inputPath the path of the input file
     * @param outputPath the path of the new file
     */
    public static void removeXmpAndWriteToFile(String inputPath, String outputPath) {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(inputPath));
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputPath))) {
            int next = 0;
            while (next != -1) {
                next = is.read();
                //Detect first byte of FF E1, the code of APP1 marker
                if (next == 0xFF) {
                    next = is.read();
                    if (next == 0xE1) {
                        Timber.i("Found FF E1");
                        //2 bytes that contain the length of the APP1 section.
                        byte Lp1 = (byte) is.read();
                        byte Lp2 = (byte) is.read();
                        Timber.i(Integer.toHexString(Lp1));
                        Timber.i(Integer.toHexString(Lp2));
                        //The identifier of the APP1 section, we find out if this section contains XMP data or not.
                        byte[] namespace = new byte[28];
                        if (is.read(namespace, 0, 28) != 28)
                            throw new IOException("Wrong amount of bytes read.");
                        Timber.i(new String(namespace, "UTF-8"));
                        if (new String(namespace, "UTF-8").equals("http://ns.adobe.com/xap/1.0/")) {
                            Timber.i("Found XMP marker");
                            while (next != 0xFF) {
                                if (next == -1)
                                    throw new IOException("Unexpected end of file.");
                                next = is.read();
                            }
                            //FF means the start of the next marker.
                            // This means the XMP section is finished and that we should resume copying.
                            outputStream.write(0xFF);
                        } else {
                            //Write everything back to the output file as we want to leave non-XMP APP1 sections as-is.
                            Timber.i("Not XMP marker");
                            outputStream.write(0xFF);
                            outputStream.write(0xE1);
                            outputStream.write(Lp1);
                            outputStream.write(Lp2);
                            outputStream.write(namespace);
                        }
                    } else {
                        outputStream.write(0xFF);
                        outputStream.write(next);
                    }
                } else {
                    outputStream.write(next);
                }
            }
        } catch (IOException e) {
            Timber.e(e);
        }
    }

}
