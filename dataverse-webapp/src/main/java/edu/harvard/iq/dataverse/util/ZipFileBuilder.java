package edu.harvard.iq.dataverse.util;

import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Convenience class to create a zip file, used by ShapefileHandler
 */
public class ZipFileBuilder implements Closeable {

    private final ZipOutputStream zipOutputStream;

    // -------------------- CONSTRUCTOR --------------------

    public ZipFileBuilder(Path outputZipFilename) throws IOException {
        zipOutputStream = new ZipOutputStream(Files.newOutputStream(outputZipFilename));
    }

    // -------------------- LOGIC --------------------

    public void addToZipFile(Path filePath) throws IOException {
        try(InputStream inputStream = Files.newInputStream(filePath)) {
            zipOutputStream.putNextEntry(new ZipEntry(filePath.getFileName().toString()));
            IOUtils.copy(inputStream, zipOutputStream);
            zipOutputStream.closeEntry();
        }
    }

    @Override
    public void close() throws IOException {
        zipOutputStream.close();
    }
}
