package edu.harvard.iq.dataverse.persistence.dataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class with common operations on dataset fields
 * 
 * @author madryk
 */
public class DatasetFieldUtil {

    private DatasetFieldUtil() {
        throw new IllegalArgumentException("Could not be instantiated");
    }
    
    // -------------------- LOGIC --------------------
    
    public static List<DatasetField> getFlatDatasetFields(List<DatasetField> datasetFields) {
        List<DatasetField> retList = new LinkedList<>();
        for (DatasetField dsf : datasetFields) {
            retList.add(dsf);
            if (dsf.getDatasetFieldType().isCompound()) {
                for (DatasetFieldCompoundValue compoundValue : dsf.getDatasetFieldCompoundValues()) {
                    retList.addAll(getFlatDatasetFields(compoundValue.getChildDatasetFields()));
                }

            }
        }
        return retList;
    }
    
    public static List<DatasetField> copyDatasetFields(List<DatasetField> copyFromList) {
        List<DatasetField> retList = new ArrayList<>();

        for (DatasetField sourceDsf : copyFromList) {
            retList.add(sourceDsf.copy());
        }

        return retList;
    }
    
    public static Map<MetadataBlock, List<DatasetField>> groupByBlock(List<DatasetField> datasetFields) {
        Map<MetadataBlock, List<DatasetField>> metadataBlocks = new HashMap<>();
        
        for (DatasetField dsf: datasetFields) {
            MetadataBlock metadataBlockOfField = dsf.getDatasetFieldType().getMetadataBlock();
            metadataBlocks.putIfAbsent(metadataBlockOfField, new ArrayList<>());
            metadataBlocks.get(metadataBlockOfField).add(dsf);
        }
        
        return metadataBlocks;
    }
    
    /**
     * Returns merged dataset fields from two given dataset field lists.
     * Merging is based on equality of {@link DatasetField#getDatasetFieldType()}.
     * <p>
     * If both source lists contains dataset field with the same {@link DatasetField#getDatasetFieldType()}
     * then resulting list will contain only dataset field from the second
     * source list.
     */
    public static List<DatasetField> mergeDatasetFields(List<DatasetField> fields1, List<DatasetField> fields2) {
        Map<DatasetFieldType, DatasetField> datasetFieldsMap = new LinkedHashMap<>();

        for (DatasetField datasetField : fields1) {
            datasetFieldsMap.put(datasetField.getDatasetFieldType(), datasetField);
        }
        for (DatasetField datasetField : fields2) {
            datasetFieldsMap.put(datasetField.getDatasetFieldType(), datasetField);
        }
        
        return new ArrayList<>(datasetFieldsMap.values());
    }
}
