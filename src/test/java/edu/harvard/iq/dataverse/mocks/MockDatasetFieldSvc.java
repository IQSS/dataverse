package edu.harvard.iq.dataverse.mocks;

import java.util.HashMap;
import java.util.Map;

import jakarta.json.JsonObject;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.MetadataBlock;

public class MockDatasetFieldSvc extends DatasetFieldServiceBean {

    Map<String, DatasetFieldType> fieldTypes = new HashMap<>();
    long nextId = 1;
    
    static MetadataBlock test = null; 

    public void setMetadataBlock(String name) {
        test = new MetadataBlock();
        test.setName(name);
    }
    
    public DatasetFieldType add(DatasetFieldType t) {
        if (t.getId() == null) {
            t.setId(nextId++);
        }
        //Assure it has a metadatablock (used in ControlledVocabularValue for i18n)
        t.setMetadataBlock(test);
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
    
    public Map<Long, JsonObject> getCVocConf(boolean byTermUriField){
        return new HashMap<Long, JsonObject>();
    }

}