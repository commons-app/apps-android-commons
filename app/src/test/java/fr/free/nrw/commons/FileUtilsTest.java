package fr.free.nrw.commons;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import fr.free.nrw.commons.upload.FileUtils;

import static org.hamcrest.CoreMatchers.is;

public class FileUtilsTest {
    @Test public void copiedFileIsIdenticalToSource() throws IOException {
        File source = File.createTempFile("temp", "");
        File dest = File.createTempFile("temp", "");
        writeToFile(source, "Hello, World");
        FileUtils.copy(new FileInputStream(source), new FileOutputStream(dest));
        Assert.assertThat(getString(dest), is(getString(source)));
    }

    private static void writeToFile(File file, String s) throws IOException {
        BufferedOutputStream buf = new BufferedOutputStream(new FileOutputStream(file));
        buf.write(s.getBytes());
        buf.close();
    }

    private static String getString(File file) throws IOException {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
        buf.read(bytes, 0, bytes.length);
        buf.close();
        return new String(bytes);
    }
}
