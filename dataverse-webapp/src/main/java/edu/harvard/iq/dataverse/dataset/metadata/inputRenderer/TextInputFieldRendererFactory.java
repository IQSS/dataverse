package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.buttonaction.FieldButtonActionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Try;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class TextInputFieldRendererFactory implements InputFieldRendererFactory<TextInputFieldRenderer> {
    
    private Instance<FieldButtonActionHandler> fieldButtonActionHandlersInstance;
    
    private Map<String, FieldButtonActionHandler> fieldButtonActionHandlers = new HashMap<>();
    
    // -------------------- CONSTRUCTORS --------------------
    
    @Inject
    public TextInputFieldRendererFactory(Instance<FieldButtonActionHandler> fieldButtonActionHandlersInstance) {
        this.fieldButtonActionHandlersInstance = fieldButtonActionHandlersInstance;
    }
    
    @PostConstruct
    public void postConstruct() {
        IteratorUtils.toList(fieldButtonActionHandlersInstance.iterator())
            .forEach(factory -> fieldButtonActionHandlers.put(factory.getName(), factory));
    }
    
    // -------------------- LOGIC --------------------
    
    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.TEXT;
    }

    @Override
    public TextInputFieldRenderer createRenderer(JsonObject jsonOptions) {
        
        TextInputRendererOptions rendererOptions = Try.of(() -> new Gson().fromJson(jsonOptions, TextInputRendererOptions.class))
                .getOrElseThrow((e) -> new InputRendererInvalidConfigException("Invalid syntax of input renderer options " + jsonOptions + ")", e));
        
        if (StringUtils.isNotBlank(rendererOptions.getButtonActionHandler())) {
            return createRendererWithActionHandler(rendererOptions);
        }
        return new TextInputFieldRenderer();
    }

    // -------------------- PRIVATE --------------------
    
    private TextInputFieldRenderer createRendererWithActionHandler(TextInputRendererOptions options) {
        FieldButtonActionHandler actionHandler = fieldButtonActionHandlers.get(options.getButtonActionHandler());
        
        if (actionHandler == null) {
            throw new InputRendererInvalidConfigException("Action handler with name: " + options.getButtonActionHandler() + " doesn't exist.");
        }
        
        return new TextInputFieldRenderer(actionHandler, options.getButtonActionTextKey(), options.getActionForOperations());
    }
    
    // -------------------- INNER CLASSES --------------------
    
    /**
     * Class representing allowed options for {@link TextInputFieldRenderer}
     */
    public static class TextInputRendererOptions {
        private String buttonActionHandler;
        private String buttonActionTextKey;
        private List<MetadataOperationSource> actionForOperations;
        
        // -------------------- GETTERS --------------------
        
        public String getButtonActionHandler() {
            return buttonActionHandler;
        }
        public String getButtonActionTextKey() {
            return buttonActionTextKey;
        }
        public List<MetadataOperationSource> getActionForOperations() {
            return actionForOperations;
        }
        
        // -------------------- SETTERS --------------------
        
        public void setButtonActionHandler(String buttonActionHandlerName) {
            this.buttonActionHandler = buttonActionHandlerName;
        }
        public void setButtonActionTextKey(String buttonActionTextKey) {
            this.buttonActionTextKey = buttonActionTextKey;
        }
        public void setActionForOperations(List<MetadataOperationSource> actionForOperations) {
            this.actionForOperations = actionForOperations;
        }
        
    }
}
