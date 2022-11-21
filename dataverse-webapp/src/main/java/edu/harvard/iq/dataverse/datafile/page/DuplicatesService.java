package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@Stateless
public class DuplicatesService {

    // -------------------- LOGIC --------------------

    public List<DuplicateGroup> listDuplicates(List<DataFile> existingFiles, List<DataFile> newFiles) {
        if (existingFiles == null || newFiles == null || newFiles.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, List<String>> existingFilesIndex = existingFiles.stream()
                .filter(this::hasChecksum)
                .filter(f -> f.getId() != null)
                .collect(Collectors.groupingBy(DataFile::getChecksumValue,
                        Collectors.mapping(f -> f.getFileMetadata().getLabel(), Collectors.toList())));
        Map<String, List<DataFile>> newFilesIndex = newFiles.stream()
                .filter(this::hasChecksum)
                .collect(Collectors.groupingBy(DataFile::getChecksumValue));
        return newFilesIndex.entrySet().stream()
                .map(e -> DuplicateGroup.of(e.getKey(),
                        existingFilesIndex.getOrDefault(e.getKey(), Collections.emptyList()),
                        e.getValue())) // Some of these are not duplicates, but they are filtered out in the next step
                .filter(g -> g.getExistingDuplicatesLabels().size() + g.getDuplicates().size() > 1) // Here real duplicates are found
                .sorted()
                .collect(Collectors.toList());
    }

    public boolean hasDuplicatesInUploadedFiles(List<DataFile> existingFiles, List<DataFile> newFiles) {
        if (existingFiles == null || newFiles == null || newFiles.isEmpty()) {
            return false;
        }
        Set<String> existingFilesChecksums = existingFiles.stream()
                .filter(this::hasChecksum)
                .filter(f -> f.getId() != null)
                .map(DataFile::getChecksumValue)
                .collect(toSet());
        Set<String> newFilesChecksums = new HashSet<>();
        for (DataFile dataFile : newFiles) {
            String checksum = dataFile.getChecksumValue();
            if (StringUtils.isBlank(checksum)) {
                continue;
            }
            if (existingFilesChecksums.contains(checksum) || newFilesChecksums.contains(checksum)) {
                return true;
            }
            newFilesChecksums.add(checksum);
        }
        return false;
    }

    // -------------------- PRIVATE --------------------

    private boolean hasChecksum(DataFile dataFile) {
        return dataFile != null && StringUtils.isNotBlank(dataFile.getChecksumValue());
    }

    // -------------------- INNER CLASSES --------------------

    public static class DuplicateGroup implements Comparable<DuplicateGroup> {
        private String checksum;
        private List<String> existingDuplicatesLabels = new ArrayList<>();
        private List<DuplicateItem> duplicates = new ArrayList<>();

        // -------------------- CONSTRUCTORS --------------------

        private DuplicateGroup(String checksum, List<String> existingDuplicatesLabels, List<DuplicateItem> duplicates) {
            this.checksum = checksum;
            this.existingDuplicatesLabels.addAll(existingDuplicatesLabels);
            this.duplicates.addAll(duplicates);
        }

        // -------------------- GETTERS --------------------

        public String getChecksum() {
            return checksum;
        }

        public List<String> getExistingDuplicatesLabels() {
            return existingDuplicatesLabels;
        }

        public List<DuplicateItem> getDuplicates() {
            return duplicates;
        }

        // -------------------- LOGIC --------------------

        public static DuplicateGroup of(String checksum, List<String> existing, List<DataFile> newDatafiles) {
            return new DuplicateGroup(checksum, existing,
                    newDatafiles.stream()
                    .map(DuplicateItem::of)
                    .collect(Collectors.toList()));
        }

        @Override
        public int compareTo(DuplicateGroup other) {
            return checksum.compareTo(other.getChecksum());
        }
    }

    public static class DuplicateItem {
        private String label;
        private DataFile dataFile;
        private boolean selected = false;

        // -------------------- CONSTRUCTORS --------------------

        private DuplicateItem(String label, DataFile dataFile) {
            this.label = label;
            this.dataFile = dataFile;
        }

        // -------------------- GETTERS --------------------

        public String getLabel() {
            return label;
        }

        public DataFile getDataFile() {
            return dataFile;
        }

        public boolean isSelected() {
            return selected;
        }

        // -------------------- LOGIC --------------------

        public static DuplicateItem of(DataFile dataFile) {
            return new DuplicateItem(dataFile.getFileMetadata().getLabel(), dataFile);
        }

        // -------------------- SETTERS --------------------

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
}
