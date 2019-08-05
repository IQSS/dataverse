package edu.harvard.iq.dataverse.dataset.difference;

/**
 * Listing of differences in two files metadata
 * 
 * @author skraffmiller
 * @author madryk
 */
public class FileMetadataDifferenceItem {
    
    private String fileName1;
    private String fileType1;
    private String fileSize1;
    private String fileCat1;
    private String fileDesc1;
    private String fileProvFree1;

    private String fileName2;
    private String fileType2;
    private String fileSize2;
    private String fileCat2;
    private String fileDesc2;
    private String fileProvFree2;

    private boolean file1Empty = false;
    private boolean file2Empty = false;
    
    // -------------------- GETTERS --------------------
    
    public String getFileName1() {
        return fileName1;
    }
    public String getFileType1() {
        return fileType1;
    }
    public String getFileSize1() {
        return fileSize1;
    }
    public String getFileCat1() {
        return fileCat1;
    }
    public String getFileDesc1() {
        return fileDesc1;
    }
    public String getFileProvFree1() {
        return fileProvFree1;
    }
    
    public String getFileName2() {
        return fileName2;
    }
    public String getFileType2() {
        return fileType2;
    }
    public String getFileSize2() {
        return fileSize2;
    }
    public String getFileCat2() {
        return fileCat2;
    }
    public String getFileDesc2() {
        return fileDesc2;
    }
    public String getFileProvFree2() {
        return fileProvFree2;
    }
    
    public boolean isFile1Empty() {
        return file1Empty;
    }
    public boolean isFile2Empty() {
        return file2Empty;
    }
    
    // -------------------- SETTERS --------------------
    
    public void setFileName1(String fileName1) {
        this.fileName1 = fileName1;
    }
    public void setFileType1(String fileType1) {
        this.fileType1 = fileType1;
    }
    public void setFileSize1(String fileSize1) {
        this.fileSize1 = fileSize1;
    }
    public void setFileCat1(String fileCat1) {
        this.fileCat1 = fileCat1;
    }
    public void setFileDesc1(String fileDesc1) {
        this.fileDesc1 = fileDesc1;
    }
    public void setFileProvFree1(String fileProvFree1) {
        this.fileProvFree1 = fileProvFree1;
    }
    
    public void setFileName2(String fileName2) {
        this.fileName2 = fileName2;
    }
    public void setFileType2(String fileType2) {
        this.fileType2 = fileType2;
    }
    public void setFileSize2(String fileSize2) {
        this.fileSize2 = fileSize2;
    }
    public void setFileCat2(String fileCat2) {
        this.fileCat2 = fileCat2;
    }
    public void setFileDesc2(String fileDesc2) {
        this.fileDesc2 = fileDesc2;
    }
    public void setFileProvFree2(String fileProvFree2) {
        this.fileProvFree2 = fileProvFree2;
    }
    
    public void setFile1Empty(boolean file1Empty) {
        this.file1Empty = file1Empty;
    }
    public void setFile2Empty(boolean file2Empty) {
        this.file2Empty = file2Empty;
    }

}