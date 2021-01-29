package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

import javax.ejb.Stateless;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class DatasetSummaryService {

    public List<DatasetFieldsByType> getDatasetSummaryFields(DatasetVersion datasetVersion, List<String> customFieldList) {

        Map<String, DatasetFieldsByType> allFieldsByType = DatasetFieldUtil.groupByType(datasetVersion.getFlatDatasetFields())
                .stream()
                .collect(HashMap::new,
                        (map, fieldsByType) -> map.put(fieldsByType.getDatasetFieldType().getName(), fieldsByType),
                        (map1, map2) -> map1.putAll(map2));

        List<DatasetFieldsByType> datasetFieldsByTypes = new ArrayList<>();
        
        for (String summaryField: customFieldList) {
            if (allFieldsByType.containsKey(summaryField)) {
                datasetFieldsByTypes.add(allFieldsByType.get(summaryField));
            }
        }

        return datasetFieldsByTypes;
    }

}
