package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DatasetField;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Toolkit for datasetFields
 * @author alejandra.tenorio
 * @author qqmyers
 */
public class DatasetFieldUtil {

    private static final Logger logger = Logger.getLogger(DatasetFieldUtil.class.getCanonicalName());

    /**
     * Removed empty fields, sets field value display order.
     * @param datasetFields the datasetFields from templates or datasetVersions we want to tidy up.
     * @param removeBlanks if remove blank fields
     */
    public static void tidyUpFields(List<DatasetField> datasetFields, boolean removeBlanks) {
        if(removeBlanks){
            Iterator<DatasetField> dsfIt = datasetFields.iterator();
            while (dsfIt.hasNext()) {
                if (dsfIt.next().removeBlankDatasetFieldValues()) {
                    dsfIt.remove();
                }
            }
            Iterator<DatasetField> dsfItTrim = datasetFields.iterator();
            while (dsfItTrim.hasNext()) {
                dsfItTrim.next().trimTrailingSpaces();
            }
        }
        Iterator<DatasetField> dsfItSort = datasetFields.iterator();
        while (dsfItSort.hasNext()) {
            dsfItSort.next().setValueDisplayOrder();
        }

    }
}
