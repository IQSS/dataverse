package edu.harvard.iq.dataverse.mocks;

import java.util.HashMap;
import java.util.Map;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;

public class MockDatasetFieldSvc extends DatasetFieldServiceBean {

    Map<String, DatasetFieldType> fieldTypes = new HashMap<>();
    long nextId = 1;

    public DatasetFieldType add(DatasetFieldType t) {
        if (t.getId() == null) {
            t.setId(nextId++);
        }
        fieldTypes.put(t.getName(), t);
        return t;
    }

    @Override
    public DatasetFieldType findByName(String name) {
        return fieldTypes.get(name);
    }

    @Override
    public DatasetFieldType findByNameOpt(String name) {
        return findByName(name);
    }

    @Override
    public ControlledVocabularyValue findControlledVocabularyValueByDatasetFieldTypeAndStrValue(DatasetFieldType dsft,
            String strValue, boolean lenient) {
        ControlledVocabularyValue cvv = new ControlledVocabularyValue();
        cvv.setDatasetFieldType(dsft);
        cvv.setStrValue(strValue);
        return cvv;
    }

}