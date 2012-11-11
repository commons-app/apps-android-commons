package org.wikimedia.commons;

public class Transcoder {
    public static native int transcode(String infile, String outfile,
            String profile);

    static {
        System.loadLibrary("transcode");
    }
}
