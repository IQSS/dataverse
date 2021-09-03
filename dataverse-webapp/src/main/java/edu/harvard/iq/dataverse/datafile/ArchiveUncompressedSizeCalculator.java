package edu.harvard.iq.dataverse.datafile;

import com.github.junrar.ContentDescription;
import com.github.junrar.Junrar;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ExternalRarDataUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;

@ApplicationScoped
public class ArchiveUncompressedSizeCalculator {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveUncompressedSizeCalculator.class);

    private SettingsServiceBean settingsService;


    // -------------------- CONSTRUCTORS --------------------

    ArchiveUncompressedSizeCalculator() { }
    
    @Inject
    public ArchiveUncompressedSizeCalculator(SettingsServiceBean settingsService) {
        this.settingsService = settingsService;
    }

    // -------------------- LOGIC --------------------

    /**
     * Returns summary size of files that are inside of an archive.
     * Returns 0 if file is not an archive or there was some problems in calculating. 
     */
    public Long calculateUncompressedSize(Path archivePath, String finalType, String fileName) {
        Long uncompressedSize = 0L;
        
        if ("application/zip".equals(finalType)) {
            uncompressedSize = extractZipContentsSize(archivePath);
        } else if ("application/vnd.rar".equals(finalType)) {
            try {
                List<ContentDescription> contentsDescription = Junrar.getContentsDescription(archivePath.toFile());
                uncompressedSize = contentsDescription.stream()
                        .mapToLong(d -> d.size)
                        .sum();
            } catch (UnsupportedRarV5Exception r5e) {
                uncompressedSize = new ExternalRarDataUtil(
                    settingsService.getValueForKey(SettingsServiceBean.Key.RarDataUtilCommand),
                    settingsService.getValueForKey(SettingsServiceBean.Key.RarDataUtilOpts),
                    settingsService.getValueForKey(SettingsServiceBean.Key.RarDataLineBeforeResultDelimiter))
                    .checkRarExternally(archivePath, fileName);
            } catch (RarException | IOException re) {
                logger.warn("Exception during rar file scan: " + fileName, re);
            }
        } else if ("application/x-7z-compressed".equals(finalType)) {
            long size = 0L;
            try (SevenZFile archive = new SevenZFile(archivePath.toFile())) {
                SevenZArchiveEntry entry;
                while ((entry = archive.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        size += entry.getSize();
                    }
                }
            } catch (IOException ioe) {
                logger.warn("Exception while checking contents of 7z file: " + fileName, ioe);
            }
            uncompressedSize = size;
        } else if ("application/gzip".equals(finalType) || "application/x-compressed-tar".equals(finalType)) {
            Long maxInputSize = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.GzipMaxInputFileSizeInBytes);
            Long maxOutputSize = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.GzipMaxOutputFileSizeInBytes);

            maxInputSize = maxInputSize != null ? maxInputSize : 0L;
            maxOutputSize = maxOutputSize != null ? maxOutputSize : 0L;

            long inputSize = archivePath.toFile().length();
            if (inputSize > 0L && inputSize <= maxInputSize) {
                Path outputFile = null;
                try (GZIPInputStream output = new GZIPInputStream(Files.newInputStream(archivePath))) {
                    outputFile = FileUtil.limitedInputStreamToTempFile(output, maxOutputSize);
                    uncompressedSize = Files.size(outputFile);
                } catch (FileExceedsMaxSizeException femse) {
                    logger.warn(
                        String.format("The contents of file [%s] exceed the max allowed size of uncompressed output", fileName),
                        femse);
                } catch (IOException ioe) {
                    logger.warn("Exception while trying to uncompress file: " + fileName, ioe);
                } finally {
                    if (outputFile != null) {
                        outputFile.toFile().delete();
                    }
                }
            }
        }
        
        return uncompressedSize;
    }

    // -------------------- PRIVATE --------------------

    private long extractZipContentsSize(Path tempFile) {
        long size = 0;
        try (ZipFile zipFile = new ZipFile(tempFile.toFile())) {
            Enumeration<ZipArchiveEntry> zipEntries = zipFile.getEntries();
            while (zipEntries.hasMoreElements()) {
                ZipArchiveEntry zipEntry = zipEntries.nextElement();
                if (!zipEntry.isDirectory()) {
                    size += zipEntry.getSize();
                }
            }
        } catch (IOException ioe) {
            logger.warn("Exception encountered: ", ioe);
        }
        return size;
    }
}
