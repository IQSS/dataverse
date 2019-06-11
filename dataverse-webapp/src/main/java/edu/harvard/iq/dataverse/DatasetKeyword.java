package edu.harvard.iq.dataverse;

/**
 *
 * @author skraffmiller
 */

public class DatasetKeyword  {

    private int displayOrder;
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private DatasetField value;
    public DatasetField getValue() {
        return this.value;
    }
    public void setValue(DatasetField value) {
        this.value = value;
    }
    
    private DatasetVersion datasetVersion;
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }
    public void setDatasetVersion(DatasetVersion metadata) {
        this.datasetVersion = metadata;
    }
    /*
    @Version
    private Long version;
    public Long getVersion() {
        return this.version;
    }
    public void setVersion(Long version) {
        this.version = version;
    }  */  

    private DatasetField vocab;
    public DatasetField getVocab() {
        return this.vocab;
    }
    public void setVocab(DatasetField vocab) {
        this.vocab = vocab;
    }

    private DatasetField vocabURI;
    public DatasetField getVocabURI() {
        return this.vocabURI;
    }
    public void setVocabURI(DatasetField vocabURI) {
        this.vocabURI = vocabURI;
    }


     public boolean isEmpty() {
        /*return ((value==null || value.getValue().trim().equals(""))
            && (vocab==null || vocab.getValue().trim().equals(""))
            && (vocabURI==null || vocabURI.getValue().trim().equals("")));*/
         return false;
    }
     

}
