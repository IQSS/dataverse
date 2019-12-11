package edu.harvard.iq.dataverse.datafile.page;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseDialog;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * View scoped class that keeps information on what
 * file/files are currently requested for download.
 * <p>
 * Information about requested download is kept in this class
 * for easier communication between guestboook dialog
 * and download handling
 * 
 * @author madryk
 * 
 * @see GuestbookResponseDialog
 * @see FileDownloadHelper
 */
@ViewScoped
@Named("RequestedDownloadType")
public class RequestedDownloadType implements Serializable {

    private DownloadType fileFormat;
    private ExternalTool tool;
    private List<FileMetadata> fileMetadatas = new ArrayList<>();
    
    

    // -------------------- GETTERS --------------------
    
    public DownloadType getFileFormat() {
        return fileFormat;
    }
    
    public ExternalTool getTool() {
        return tool;
    }
    
    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }
    
    // -------------------- LOGIC --------------------
    
    public void initDownloadType(FileMetadata fileMetadata) {
        this.fileMetadatas = Lists.newArrayList(fileMetadata);
        this.fileFormat = DownloadType.DOWNLOAD;
    }
    
    public void initDownloadTypeForOriginal(FileMetadata fileMetadata) {
        this.fileMetadatas = Lists.newArrayList(fileMetadata);
        this.fileFormat = DownloadType.ORIGINAL;
    }
    
    public void initDownloadTypeForTab(FileMetadata fileMetadata) {
        this.fileMetadatas = Lists.newArrayList(fileMetadata);
        this.fileFormat = DownloadType.TAB;
    }
    
    public void initDownloadTypeForRdata(FileMetadata fileMetadata) {
        this.fileMetadatas = Lists.newArrayList(fileMetadata);
        this.fileFormat = DownloadType.RDATA;
    }
    
    public void initDownloadTypeForVar(FileMetadata fileMetadata) {
        this.fileMetadatas = Lists.newArrayList(fileMetadata);
        this.fileFormat = DownloadType.VAR;
    }
    public void initDownloadTypeForSubset(FileMetadata fileMetadata) {
        this.fileMetadatas = Lists.newArrayList(fileMetadata);
        this.fileFormat = DownloadType.SUBSET;
    }
    
    public void initDownloadTypeForWorldMap(FileMetadata fileMetadata) {
        this.fileMetadatas = Lists.newArrayList(fileMetadata);
        this.fileFormat = DownloadType.WORLDMAP;
    }
    
    public void initDownloadTypeForPackage(FileMetadata fileMetadata) {
        this.fileMetadatas = Lists.newArrayList(fileMetadata);
        this.fileFormat = DownloadType.PACKAGE;
    }
    
    public void initDownloadTypeForTool(FileMetadata fileMetadata, ExternalTool tool) {
        this.fileMetadatas = Lists.newArrayList(fileMetadata);
        this.fileFormat = DownloadType.EXTERNALTOOL;
        this.tool = tool;
    }
    
    
    public void initMultiDownloadType(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
        this.fileFormat = DownloadType.DOWNLOAD;
    }
    public void initMultiOriginalDownloadType(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
        this.fileFormat = DownloadType.ORIGINAL;
    }
    
    
    public boolean isSingleFileRequested() {
        return fileMetadatas.size() == 1;
    }
    
    public FileMetadata getSingleFileMetadata() {
        return fileMetadatas.get(0);
    }
    
}
