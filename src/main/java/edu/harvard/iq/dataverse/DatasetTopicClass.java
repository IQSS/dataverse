/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import javax.persistence.Column;
import javax.persistence.Version;

/**
 *
 * @author skraffmiller
 */
public class DatasetTopicClass  {

    private DatasetVersion datasetVersion;
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }
    public void setDatasetVersion(DatasetVersion metadata) {
        this.datasetVersion = metadata;
    }
    
    private int displayOrder;
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private DatasetFieldValue value;
    public DatasetFieldValue getValue() {
        return this.value;
    }
    public void setValue(DatasetFieldValue value) {
        this.value = value;
    }

    private Long version;
    public Long getVersion() {
        return this.version;
    }
    public void setVersion(Long version) {
        this.version = version;
    }    

    private DatasetFieldValue vocab;
    public DatasetFieldValue getVocab() {
        return this.vocab;
    }
    public void setVocab(DatasetFieldValue vocab) {
        this.vocab = vocab;
    }

    private DatasetFieldValue vocabURI;
    public DatasetFieldValue getVocabURI() {
        return this.vocabURI;
    }
    public void setVocabURI(DatasetFieldValue vocabURI) {
        this.vocabURI = vocabURI;
    }

    
    
    public boolean isEmpty() {/*
        return ((value==null || value.getValue().trim().equals(""))
            && (vocab==null || vocab.getValue().trim().equals(""))
            && (vocabURI==null || vocabURI.getValue().trim().equals("")));*/
        return false;
    }

  
    
}
