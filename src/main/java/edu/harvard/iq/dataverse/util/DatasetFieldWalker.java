package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

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
        void endField( DatasetField f );
        void primitiveValue( DatasetFieldValue dsfv );
        void controledVocabularyValue( ControlledVocabularyValue cvv );
        void startCompoundValue( DatasetFieldCompoundValue dsfcv );
        void endCompoundValue( DatasetFieldCompoundValue dsfcv );
    }
    
    /**
     * Convenience method to walk over a field.
     * @param dsf the field to walk over.
     * @param l the listener to execute on {@code dsf}'s values and structure.
     */
    public static void walk( DatasetField dsf, Listener l ) {
        DatasetFieldWalker joe = new DatasetFieldWalker(l);
        joe.walk(dsf, Collections.emptyList());
    }

    /**
     * Convenience method to walk over a list of fields. Traversal
     * is done in display order.
     * @param fields the fields to go over. Does not have to be sorted.
     * @param exclude the fields to skip
     * @param l the listener to execute on each field values and structure.
     */
    public static void walk(List<DatasetField> fields, List<DatasetField> exclude, Listener l) {
        DatasetFieldWalker joe = new DatasetFieldWalker(l);
        for ( DatasetField dsf : sort( fields, DatasetField.DisplayOrder) ) {
            joe.walk(dsf, exclude);
        }
    }
    
    private Listener l;
    
    
    public DatasetFieldWalker(Listener l) {
        this.l = l;
    }
    
    public DatasetFieldWalker(){
        this( null );
    }
    
    public void walk(DatasetField fld, List<DatasetField> exclude) {
        l.startField(fld);
        DatasetFieldType datasetFieldType = fld.getDatasetFieldType();

        List<String> excludeStrings = new ArrayList<>();
        for (DatasetField datasetField : exclude) {
            excludeStrings.add(datasetField.getDatasetFieldType().getName());
        }

        if ( datasetFieldType.isControlledVocabulary() ) {
            for ( ControlledVocabularyValue cvv 
                    : sort(fld.getControlledVocabularyValues(), ControlledVocabularyValue.DisplayOrder) ) {
                l.controledVocabularyValue(cvv);
            }
            
        } else if ( datasetFieldType.isPrimitive() ) {
            for ( DatasetFieldValue pv : sort(fld.getDatasetFieldValues(), DatasetFieldValue.DisplayOrder) ) {
                // FIXME: Add exclusions elsewhere as needed.
                if (!excludeStrings.contains(datasetFieldType.getName())) {
                    l.primitiveValue(pv);
                }
            }
            
        } else if ( datasetFieldType.isCompound() ) {
           for ( DatasetFieldCompoundValue dsfcv : sort( fld.getDatasetFieldCompoundValues(), DatasetFieldCompoundValue.DisplayOrder) ) {
               l.startCompoundValue(dsfcv);
               for ( DatasetField dsf : sort(dsfcv.getChildDatasetFields(), DatasetField.DisplayOrder ) ) {
                   walk(dsf, exclude);
               }
               l.endCompoundValue(dsfcv);
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
