/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when a compound field contains invalid controlled vocabulary values
 * @author ellenk
 */
public class CompoundVocabularyException extends JsonParseException {
    private String message;
    private List<ControlledVocabularyException> exList = new ArrayList<>(); 
    private List<DatasetFieldCompoundValue> validValues;

    
     public CompoundVocabularyException(String message) {
        super(message);
    }

    public CompoundVocabularyException(String message, List<ControlledVocabularyException>  cause, List<DatasetFieldCompoundValue> vals) {
        super(message);
        exList = cause;
        this.message=message;
        this.validValues=vals;
        for (ControlledVocabularyException ex : exList) {
            this.message+= ex.getMessage() +";";
        }
    }

    public List<ControlledVocabularyException> getExList() {
        return exList;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    public List<DatasetFieldCompoundValue> getValidValues() {
        return validValues;
    }
}
