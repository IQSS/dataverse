package edu.harvard.iq.dataverse.util.bagit;

import edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider.InputStreamProvider;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author adaybujeda
 */
public class ManifestReader {

    private static final Logger logger = Logger.getLogger(ManifestReader.class.getCanonicalName());

    public Optional<Path> getSupportedManifest(FileDataProvider fileDataProvider, Path bagRoot) {
        for(BagChecksumType type: BagChecksumType.values()) {
            Path manifestPath = bagRoot.resolve(type.getFileName());
            Optional<InputStreamProvider> manifestEntry = fileDataProvider.getInputStreamProvider(manifestPath);
            if (manifestEntry.isPresent()) {
                return Optional.of(manifestPath);
            }
        }

        return Optional.empty();
    }

    public Optional<ManifestChecksum> getManifestChecksums(FileDataProvider fileDataProvider, Path bagRoot) {
        for(BagChecksumType type: BagChecksumType.values()) {
            Path manifestPath = bagRoot.resolve(type.getFileName());
            try {
                Optional<InputStreamProvider> manifestEntry = fileDataProvider.getInputStreamProvider(manifestPath);
                if (manifestEntry.isPresent()) {
                    Map<Path, String> checksums = readManifestChecksums(bagRoot, manifestEntry.get().getInputStream());
                    ManifestChecksum manifestChecksum = new ManifestChecksum(manifestPath, type, checksums);
                    logger.log(Level.FINE, String.format("action=getManifestChecksums result=success fileDataProvider=%S bagRoot=%s manifestChecksum=%s", fileDataProvider.getName(), bagRoot, manifestChecksum));
                    return Optional.of(manifestChecksum);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, String.format("action=getManifestChecksums result=error fileDataProvider=%s bagRoot=%s manifestPath=%s", fileDataProvider.getName(), bagRoot, manifestPath), e);
                return Optional.empty();
            }
        }
        logger.log(Level.WARNING,String.format("action=getManifestChecksums result=no-supported-manifest-found fileDataProvider=%s bagRoot=%s supportedTypes=%s", fileDataProvider.getName(), bagRoot, BagChecksumType.asList()));
        return Optional.empty();
    }

    private Map<Path, String> readManifestChecksums(Path bagRoot, InputStream manifestEntry) throws Exception{
        final HashMap<Path, String> checksumsMap = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(manifestEntry));
            String line = br.readLine();
            while(line != null){
                final String[] parts = line.split("\\s+", 2);
                final Path file = bagRoot.resolve(Path.of(parts[1]));
                final String hash = parts[0];
                checksumsMap.put(file, hash);
                line = br.readLine();
            }
        } finally {
            IOUtils.closeQuietly(manifestEntry);
        }

        return checksumsMap;
    }

    public static class ManifestChecksum {
        private final Path manifestFile;
        private final BagChecksumType type;
        private final Map<Path, String> fileChecksums;

        public ManifestChecksum(Path manifestFile, BagChecksumType type, Map<Path, String> fileChecksums) {
            this.manifestFile = manifestFile;
            this.type = type;
            this.fileChecksums = fileChecksums != null ? fileChecksums : Collections.emptyMap();
        }

        public Path getManifestFile() {
            return manifestFile;
        }

        public BagChecksumType getType() {
            return type;
        }

        public Map<Path, String> getFileChecksums() {
            return fileChecksums;
        }

        @Override
        public String toString() {
            return String.format("ManifestChecksum{manifestFile=%s, type=%s, fileChecksumItems=%s}", manifestFile, type, fileChecksums.size());
        }
    }
}
