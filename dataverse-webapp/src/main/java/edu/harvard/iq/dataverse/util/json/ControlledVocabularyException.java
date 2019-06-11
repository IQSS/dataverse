/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DatasetFieldType;

/**
 * Thrown when value is not a valid controlled vocabulary for datasetFeidType
 * @author ellenk
 */
public class ControlledVocabularyException extends JsonParseException {
    DatasetFieldType dsfType;
    String strValue;

    public DatasetFieldType getDsfType() {
        return dsfType;
    }

    public void setDsfType(DatasetFieldType dsfType) {
        this.dsfType = dsfType;
    }

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }
    
     public ControlledVocabularyException(String message, DatasetFieldType dsfType, String strValue) {         
        super(message);
        this.dsfType=dsfType;
        this.strValue=strValue;
    }

    public ControlledVocabularyException(String message, Throwable cause) {
        super(message, cause);
    }
}
