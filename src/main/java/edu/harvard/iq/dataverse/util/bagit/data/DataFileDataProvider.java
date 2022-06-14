package edu.harvard.iq.dataverse.util.bagit.data;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 *
 * @author adaybujeda
 */
class DataFileDataProvider implements FileDataProvider {
    private static final Logger logger = Logger.getLogger(DataFileDataProvider.class.getCanonicalName());

    private final FileUtilWrapper fileUtilWrapper;
    private final String name;
    private final Map<Path, DataFile> dataFilesIndex;

    public DataFileDataProvider(FileUtilWrapper fileUtilWrapper, String name, List<DataFile> dataFiles) {
        this.fileUtilWrapper = fileUtilWrapper;
        this.name = name;
        this.dataFilesIndex = new LinkedHashMap<>();
        dataFiles.stream().forEach(dataFile -> {
            String directory = Optional.ofNullable(dataFile.getDirectoryLabel()).orElse("");
            String fileName = dataFile.getCurrentName();
            dataFilesIndex.put(Path.of(directory, fileName), dataFile);
        });
    }

    public DataFileDataProvider(String name, List<DataFile> dataFiles) {
        this(new FileUtilWrapper(), name, dataFiles);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Path> getFilePaths() {
        return List.copyOf(dataFilesIndex.keySet());
    }

    @Override
    public Optional<InputStreamProvider> getInputStreamProvider(Path filePath) {
        Optional<DataFile> dataFileInfo = Optional.ofNullable(dataFilesIndex.get(filePath));

        if (dataFileInfo.isEmpty()) {
            logger.fine(String.format("action=getFileInputStream result=file-not-found filePath=%s", filePath));
            return Optional.empty();
        }

        String[] storageInfo = DataAccess.getDriverIdAndStorageLocation(dataFileInfo.get().getStorageIdentifier());
        String driverType = DataAccess.getDriverType(storageInfo[0]);
        String storageLocation = storageInfo[1];
        if (!driverType.equals("tmp")) {
            logger.warning(String.format("action=getFileInputStream result=driver-not-supported driverType=%s filePath=%s", driverType, filePath));
            return Optional.empty();
        }

        Path actualFileLocation = Path.of(fileUtilWrapper.getFilesTempDirectory(), storageLocation);
        if (actualFileLocation.toFile().exists()) {
            return Optional.of(() ->  fileUtilWrapper.newInputStream(actualFileLocation));
        }

        logger.fine(String.format("action=getFileInputStream result=file-not-found filePath=%s actualFileLocation=%s", filePath, actualFileLocation));
        return Optional.empty();
    }

    @Override
    public void close() throws IOException {
        // Intentionally left blank
        // Nothing to do in this implementation.
    }
}
