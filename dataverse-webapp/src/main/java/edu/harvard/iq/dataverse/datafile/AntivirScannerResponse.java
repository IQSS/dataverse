package edu.harvard.iq.dataverse.datafile;

public class AntivirScannerResponse {

    private boolean fileInfected;
    private String message;
    
    public AntivirScannerResponse(boolean fileInfected, String message) {
        this.fileInfected = fileInfected;
        this.message = message;
    }
    public boolean isFileInfected() {
        return fileInfected;
    }
    public String getMessage() {
        return message;
    }
}
