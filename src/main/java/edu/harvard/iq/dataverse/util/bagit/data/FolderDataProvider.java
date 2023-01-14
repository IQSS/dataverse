package edu.harvard.iq.dataverse.util.bagit.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author adaybujeda
 */
class FolderDataProvider implements FileDataProvider {
    private static final Logger logger = Logger.getLogger(FolderDataProvider.class.getCanonicalName());

    private final FileUtilWrapper fileUtilWrapper;
    private final Path folderLocation;

    public FolderDataProvider(FileUtilWrapper fileUtilWrapper, Path folderLocation) {
        this.fileUtilWrapper = fileUtilWrapper;
        this.folderLocation = folderLocation;
    }

    public FolderDataProvider(Path folderLocation) {
        this(new FileUtilWrapper(), folderLocation);
    }

    @Override
    public String getName() {
        return folderLocation.toString();
    }

    @Override
    public List<Path> getFilePaths() {
        if(!folderLocation.toFile().exists()) {
            logger.warning(String.format("action=getFilePaths result=folder-not-found folderLocation=%s", folderLocation));
            return Collections.emptyList();
        }

        try {
            return fileUtilWrapper.list(folderLocation).map(path -> path.getFileName()).collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(Level.WARNING, String.format("action=getFilePaths error folderLocation=%s", folderLocation), e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<InputStreamProvider> getInputStreamProvider(Path filePath) {
        Path actualFileLocation = folderLocation.resolve(filePath);
        if (actualFileLocation.toFile().exists()) {
            return Optional.of(() ->  fileUtilWrapper.newInputStream(actualFileLocation));
        }

        logger.fine(String.format("action=getFileInputStream result=file-not-found filePath=%s", actualFileLocation));
        return Optional.empty();
    }

    @Override
    public void close() throws IOException {
        // Intentionally left blank
        // Nothing to do in this implementation.
    }
}
