package edu.harvard.iq.dataverse.globus;

public class FileDetailsHolder {

    private String hash;
    private String mime;
    private String storageID;

    public FileDetailsHolder(String id, String hash, String mime) {

        this.storageID = id;
        this.hash = hash;
        this.mime = mime;

    }

    public String getStorageID() {
        return this.storageID;
    }

    public String getHash() {
        return hash;
    }

    public String getMime() {
        return mime;
    }

}