package edu.harvard.iq.dataverse.util.bagit.data;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 *
 * @author adaybujeda
 */
class ZipFileDataProvider implements FileDataProvider {
    private static final Logger logger = Logger.getLogger(ZipFileDataProvider.class.getCanonicalName());
    private final String name;
    private final ZipFile zipFile;

    public ZipFileDataProvider(String name, ZipFile zipFile) {
        this.name = name;
        this.zipFile = zipFile;
    }

    public ZipFileDataProvider(String name, File file) throws IOException {
        this(name,  new ZipFile(file));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Path> getFilePaths() {
        Enumeration<ZipArchiveEntry> zipEntries = zipFile.getEntries();
        List<Path> files = new ArrayList<>();
        while (zipEntries.hasMoreElements()) {
            ZipArchiveEntry zipEntry = zipEntries.nextElement();
            if (zipEntry.isDirectory()) {
                continue;
            }
            files.add(Path.of(zipEntry.getName()));
        }
        return files;
    }

    @Override
    public Optional<InputStreamProvider> getInputStreamProvider(Path filePath) {
        ZipArchiveEntry fileEntry = zipFile.getEntry(filePath.toString());
        if (fileEntry != null) {
            return Optional.of(() -> zipFile.getInputStream(fileEntry));
        }

        logger.fine(String.format("action=getFileInputStream result=file-not-found filePath=%s", filePath));
        return Optional.empty();
    }

    @Override
    public void close() throws IOException {
        ZipFile.closeQuietly(zipFile);
    }
}
