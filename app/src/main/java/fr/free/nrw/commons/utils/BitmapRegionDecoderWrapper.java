package fr.free.nrw.commons.utils;

import android.graphics.BitmapRegionDecoder;

import java.io.FileInputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BitmapRegionDecoderWrapper {

    @Inject
    public BitmapRegionDecoderWrapper() {

    }

    public BitmapRegionDecoder newInstance(FileInputStream file, boolean isSharable) throws IOException {
        return BitmapRegionDecoder.newInstance(file, isSharable);
    }
}
