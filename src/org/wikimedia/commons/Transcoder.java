package org.wikimedia.commons;

public class Transcoder {
    public interface TranscoderProgressCallback {
        public void transcodeProgressCb(int percent);
    }

    public static native int transcode(String infile, String outfile, String profile, TranscoderProgressCallback cb);

    static {
        System.loadLibrary("transcode");
    }
}
