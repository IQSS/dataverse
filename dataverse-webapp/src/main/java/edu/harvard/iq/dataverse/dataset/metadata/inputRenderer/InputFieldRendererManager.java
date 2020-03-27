package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Try;
import org.apache.commons.collections4.IteratorUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateless
public class InputFieldRendererManager {
    
    private Instance<InputFieldRendererFactory<?>> inputRendererFactoriesInstance;
    
    private Map<InputRendererType, InputFieldRendererFactory<?>> inputRendererFactories = new HashMap<>();
    
    // -------------------- CONSTRUCTORS --------------------
    
    @Inject
    public InputFieldRendererManager(Instance<InputFieldRendererFactory<?>> inputRendererFactoriesInstance) {
        this.inputRendererFactoriesInstance = inputRendererFactoriesInstance;
    }
    
    @PostConstruct
    public void postConstruct() {
        IteratorUtils.toList(inputRendererFactoriesInstance.iterator())
            .forEach(factory -> inputRendererFactories.put(factory.isFactoryForType(), factory));
    }
    
    // -------------------- LOGIC --------------------
    
    /**
     * Returns {@link InputFieldRenderer}s grouped by
     * {@link DatasetFieldType}.
     * 
     * @see #obtainRenderer(DatasetFieldType)
     */
    public Map<DatasetFieldType, InputFieldRenderer> obtainRenderersByType(List<DatasetField> datasetFields) {
        Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType = new HashMap<>();
        Set<DatasetFieldType> fieldTypes = new HashSet<>();
        
        for (DatasetField field: datasetFields) {
            fieldTypes.add(field.getDatasetFieldType());
            for (DatasetField child: field.getDatasetFieldsChildren()) {
                fieldTypes.add(child.getDatasetFieldType());
            }
        }
        for (DatasetFieldType fieldType: fieldTypes) {
            inputRenderersByFieldType.put(fieldType, obtainRenderer(fieldType));
        }
        
        return inputRenderersByFieldType;
    }
    
    /**
     * Returns {@link InputFieldRenderer} associated with
     * the given {@link DatasetFieldType}
     */
    public InputFieldRenderer obtainRenderer(DatasetFieldType fieldType) {
        InputRendererType rendererType = fieldType.getInputRendererType();
        String rendererOptions = fieldType.getInputRendererOptions();
        
        JsonParser jsonParser = new JsonParser();
        
        JsonObject jsonOptions = Try.of(() -> jsonParser.parse(rendererOptions))
                 .map(json -> json.getAsJsonObject())
                 .getOrElseThrow((e) -> new InputRendererInvalidConfigException(
                         "Unable to parse input renderer options for field " + fieldType + " - check your field type configuration", e));
        
        InputFieldRenderer renderer = inputRendererFactories.get(rendererType)
            .createRenderer(jsonOptions);
        
        
        return renderer;
    }
}
