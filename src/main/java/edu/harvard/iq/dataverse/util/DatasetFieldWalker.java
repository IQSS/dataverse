package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * A means of iterating over {@link DatasetField}s, or a collection of them.
 * As these may have a complex structure (compound values, etc), this object 
 * allows processing them via an event stream, similar to SAX parsing of XML.
 * Visiting of the fields is done in display order.
 * 
 * @author michael
 */
public class DatasetFieldWalker {

    private static final Logger logger = Logger.getLogger(DatasetFieldWalker.class.getCanonicalName());

    public interface Listener {
        void startField( DatasetField f );
        void addExpandedValuesArray( DatasetField f );
        void endField( DatasetField f );
        void externalVocabularyValue( DatasetFieldValue dsfv, JsonObject cvocEntry );
        void primitiveValue( DatasetFieldValue dsfv );
        void controlledVocabularyValue( ControlledVocabularyValue cvv );
        void startCompoundValue( DatasetFieldCompoundValue dsfcv );
        void endCompoundValue( DatasetFieldCompoundValue dsfcv );
    }
    
    /**
     * Convenience method to walk over a field.
     * @param dsf the field to walk over.
     * @param l the listener to execute on {@code dsf}'s values and structure.
     */
    public static void walk( DatasetField dsf, Listener l, Map<Long, JsonObject> cvocMap ) {
        DatasetFieldWalker joe = new DatasetFieldWalker(l, cvocMap);
        SettingsServiceBean nullServiceBean = null;
        joe.walk(dsf, nullServiceBean);
    }

    /**
     * Convenience method to walk over a list of fields. Traversal
     * is done in display order.
     * @param fields the fields to go over. Does not have to be sorted.
     * @param exclude the fields to skip
     * @param l the listener to execute on each field values and structure.
     */
    public static void walk(List<DatasetField> fields, SettingsServiceBean settingsService, Map<Long, JsonObject> cvocMap, Listener l) {
        DatasetFieldWalker joe = new DatasetFieldWalker(l, cvocMap);
        for ( DatasetField dsf : sort( fields, DatasetField.DisplayOrder) ) {
            joe.walk(dsf, settingsService);
        }
    }
    
    private Listener l;
    private Map<Long, JsonObject> cvocMap;
    
    
    public DatasetFieldWalker(Listener l, Map<Long, JsonObject> cvocMap) {
        this.l = l;
        this.cvocMap = cvocMap;
    }
    
    public DatasetFieldWalker(){
        this( null, null);
    }
    
    public void walk(DatasetField fld, SettingsServiceBean settingsService) {
        l.startField(fld);
        DatasetFieldType datasetFieldType = fld.getDatasetFieldType();
        
        if ( datasetFieldType.isControlledVocabulary() ) {
            for ( ControlledVocabularyValue cvv 
                    : sort(fld.getControlledVocabularyValues(), ControlledVocabularyValue.DisplayOrder) ) {
                l.controlledVocabularyValue(cvv);
            }
            
        } else if ( datasetFieldType.isPrimitive() ) {
            for ( DatasetFieldValue pv : sort(fld.getDatasetFieldValues(), DatasetFieldValue.DisplayOrder) ) {
                if (settingsService != null && settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport, false) && DatasetFieldType.FieldType.EMAIL.equals(pv.getDatasetField().getDatasetFieldType().getFieldType())) {
                    continue;
                }
                l.primitiveValue(pv);
            }
            
        } else if ( datasetFieldType.isCompound() ) {
           for ( DatasetFieldCompoundValue dsfcv : sort( fld.getDatasetFieldCompoundValues(), DatasetFieldCompoundValue.DisplayOrder) ) {
               l.startCompoundValue(dsfcv);
               for ( DatasetField dsf : sort(dsfcv.getChildDatasetFields(), DatasetField.DisplayOrder ) ) {
                   walk(dsf, settingsService);
               }
               l.endCompoundValue(dsfcv);
           }
        }
        l.addExpandedValuesArray(fld); 
        if(datasetFieldType.isPrimitive() && cvocMap.containsKey(datasetFieldType.getId())) {
            for ( DatasetFieldValue evv : sort(fld.getDatasetFieldValues(), DatasetFieldValue.DisplayOrder) ) {
                if (settingsService != null && settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport, false) && DatasetFieldType.FieldType.EMAIL.equals(evv.getDatasetField().getDatasetFieldType().getFieldType())) {
                    continue;
                }
                l.externalVocabularyValue(evv, cvocMap.get(datasetFieldType.getId()));
            }
            
        }
        l.endField(fld);
    }
    
    
    public void setL(Listener l) {
        this.l = l;
    }
    
    static private <T> Iterable<T> sort( List<T> list, Comparator<T> cmp ) {
        ArrayList<T> tbs = new ArrayList<>(list);
        Collections.sort(tbs, cmp);
        return tbs;
    }
    
}
