package edu.harvard.iq.dataverse.dataset.file;

import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class takes care of editing tags(FileMetadata and Datafile) for single or multiple files.
 */
@ViewScoped
@Named("FileTagModal")
public class FileTagModal implements Serializable {

    private Set<String> fileMetadataTags = new HashSet<>();
    private Set<String> dataFileTags = new HashSet<>();
    private TreeSet<String> selectedFileMetadataTags = new TreeSet<>();
    private TreeSet<String> selectedDataFileTags = new TreeSet<>();

    private boolean removeUnusedTags;
    private String newCategoryName;
    private FileMetadata selectedFile;
    private Collection<FileMetadata> selectedFiles = new HashSet<>();


    // -------------------- GETTERS --------------------

    public Set<String> getFileMetadataTags() {
        return fileMetadataTags;
    }

    public Set<String> getDataFileTags() {
        return dataFileTags;
    }

    public TreeSet<String> getSelectedFileMetadataTags() {
        return selectedFileMetadataTags;
    }

    public TreeSet<String> getSelectedDataFileTags() {
        return selectedDataFileTags;
    }

    public String getNewCategoryName() {
        return newCategoryName;
    }

    public boolean isRemoveUnusedTags() {
        return removeUnusedTags;
    }

    public FileMetadata getSelectedFile() {
        return selectedFile;
    }

    public Collection<FileMetadata> getSelectedFiles() {
        return selectedFiles;
    }

    // -------------------- LOGIC --------------------

    public void initForSingleFile(FileMetadata fileMetadata, Dataset dataset) {
        cleanupModalState();
        this.selectedFile = fileMetadata;

        prepareTags(fileMetadata, dataset);
    }

    public void initForMultipleFiles(Collection<FileMetadata> fileMetadatas, Dataset dataset) {
        cleanupModalState();
        this.selectedFiles = fileMetadatas;

        prepareTags(fileMetadatas, dataset);
    }

    /**
     * Saves custom category typed by user.
     */
    public String saveNewCategory() {

        if (!newCategoryName.isEmpty()) {

            fileMetadataTags.add(newCategoryName);
            selectedFileMetadataTags.add(newCategoryName);
            newCategoryName = "";
        }

        return "";
    }

    public boolean isTabularFile() {
        if (!selectedFiles.isEmpty()) {
            return selectedFiles.stream()
                    .anyMatch(fm -> fm.getDataFile().isTabularData());
        } else if (selectedFile != null) {
            return selectedFile.getDataFile().isTabularData();
        }

        return false;
    }

    // -------------------- PRIVATE --------------------

    /**
     * Cleans up the model since it is ViewScoped, it will not clean itself when the modal window will popup again.
     * <p></p>
     * It had to be used in 'constructor' and not when closing window,
     * otherwise jsf will raise an error https://stackoverflow.com/questions/9069379/validation-error-value-is-not-valid
     */
    public void cleanupModalState() {
        selectedFileMetadataTags.clear();
        selectedDataFileTags.clear();
        selectedFiles.clear();
        fileMetadataTags.clear();
        dataFileTags.clear();

        removeUnusedTags = false;
    }

    private void prepareTags(Collection<FileMetadata> fileMetadata, Dataset dataset) {
        fileMetadata.forEach(fileMD -> prepareFileMetadataTags(fileMD, dataset));
        fileMetadata.forEach(this::prepareDataFileTags);
    }

    private void prepareTags(FileMetadata fileMetadata, Dataset dataset) {
        prepareFileMetadataTags(fileMetadata, dataset);
        prepareDataFileTags(fileMetadata);
    }

    private void prepareFileMetadataTags(FileMetadata fileMetadata, Dataset dataset) {
        fileMetadataTags.addAll(dataset.getCategoriesByName());
        fileMetadataTags.addAll(fileMetadata.getCategoriesByName());

        selectedFileMetadataTags.addAll(fileMetadata.getCategoriesByName());
    }

    /**
     * DataFile tags are used exclusively by TAB separated values (ingested files).
     */
    private void prepareDataFileTags(FileMetadata fileMetadata) {
        dataFileTags.addAll(DataFileTag.listTags());
        selectedDataFileTags.addAll(fileMetadata.getDataFile().getTagLabels());
    }

    // -------------------- SETTERS --------------------

    public void setFileMetadataTags(Set<String> fileMetadataTags) {
        this.fileMetadataTags = fileMetadataTags;
    }

    public void setDataFileTags(Set<String> dataFileTags) {
        this.dataFileTags = dataFileTags;
    }

    public void setSelectedFileMetadataTags(TreeSet<String> selectedFileMetadataTags) {
        this.selectedFileMetadataTags = selectedFileMetadataTags;
    }

    public void setSelectedDataFileTags(TreeSet<String> selectedDataFileTags) {
        this.selectedDataFileTags = selectedDataFileTags;
    }

    public void setRemoveUnusedTags(boolean removeUnusedTags) {
        this.removeUnusedTags = removeUnusedTags;
    }

    public void setNewCategoryName(String newCategoryName) {
        this.newCategoryName = newCategoryName;
    }

    public void setSelectedFile(FileMetadata selectedFile) {
        this.selectedFile = selectedFile;
    }
}
