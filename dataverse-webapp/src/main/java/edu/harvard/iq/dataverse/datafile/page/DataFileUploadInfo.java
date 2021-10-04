package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The class is used to store the data about files being uploaded, namely
 * the size of originally uploaded file and datafiles produced from that file
 * that are going to be saved.
 * <br><br>
 * When datafile is removed from upload (by the user action or automatically)
 * it should be also removed from the instance of the class using
 * {@link DataFileUploadInfo#removeFromDataFilesToSave(DataFile)} method.
 * That allows the {@link DataFileUploadInfo#canSubtractSize(DataFile)} method
 * to return reliable information whether the size of originally uploaded file
 * can be subtracted from already used upload batch size limit – it should be
 * possible only when all the datafiles created from source file (ie. original
 * file added to upload) are not going to be saved.
 * <br><br>
 * Internally the storageId of {@link DataFile} is used instead of hashCode as
 * the latter is unusable before the datafile is persisted.
 */
public class DataFileUploadInfo {
    private Map<String, Integer> index = new HashMap<>();
    private Map<Integer, Long> sizes = new HashMap<>();
    private Map<Integer, Set<String>> dataFilesToSave = new HashMap<>();

    private AtomicInteger generator = new AtomicInteger(0);

    // -------------------- LOGIC --------------------

    /**
     * Adds info about size of source file and all datafiles produced from it
     * to {@link DataFileUploadInfo} instance.
     */
    public void addSizeAndDataFiles(long sourceFileSize, List<DataFile> dataFiles) {
        if (dataFiles == null || dataFiles.isEmpty()) {
            return;
        }
        int linkingValue = nextValue();
        dataFiles.stream()
                .map(DvObject::getStorageIdentifier)
                .forEach(id -> index.put(id, linkingValue));
        sizes.put(linkingValue, sourceFileSize);
        dataFilesToSave.put(linkingValue, dataFiles.stream()
                .map(DvObject::getStorageIdentifier)
                .collect(Collectors.toSet()));
    }

    /**
     * Returns the size of source file for the given datafile. If the datafile is
     * unknown for the instance of {@link DataFileUploadInfo} 0 will be returned.
     */
    public long getSourceFileSize(DataFile dataFile) {
        return Optional.ofNullable(index.get(dataFile.getStorageIdentifier()))
                .map(sizes::get)
                .orElse(0L);
    }

    /**
     * Removes the given datafile from the set of files produced from source file
     * that are going to be saved.
     */
    public void removeFromDataFilesToSave(DataFile dataFile) {
        String storageId = dataFile.getStorageIdentifier();
        Optional.ofNullable(index.get(storageId))
                .map(dataFilesToSave::get)
                .ifPresent(u -> u.remove(storageId));
    }

    /**
     * Returns true only when the given datafile and all the other datafiles that
     * were created from single source file are not going to be saved.
     */
    public boolean canSubtractSize(DataFile dataFile) {
        return Optional.ofNullable(index.get(dataFile.getStorageIdentifier()))
                .map(dataFilesToSave::get)
                .map(Set::isEmpty)
                .orElse(false);
    }

    /**
     * Clears all the internal data.
     */
    public void clear() {
        index.clear();
        sizes.clear();
        dataFilesToSave.clear();
        generator.set(0);
    }

    // -------------------- PRIVATE --------------------

    private int nextValue() {
        return generator.incrementAndGet();
    }
}
