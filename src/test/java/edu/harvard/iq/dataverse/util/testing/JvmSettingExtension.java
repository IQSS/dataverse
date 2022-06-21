package edu.harvard.iq.dataverse.util.testing;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class JvmSettingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    
    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestClass(), context.getRequiredTestMethod()));
    }
    
    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        extensionContext.getTestMethod().ifPresent(method -> {
            JvmSetting[] settings = method.getAnnotationsByType(JvmSetting.class);
            for (JvmSetting setting : settings) {
                // get the setting ...
                String oldSetting = System.getProperty(setting.key().getScopedKey());
    
                // if present - store in context to restore later
                if (oldSetting != null) {
                    getStore(extensionContext).put(setting.key().getScopedKey(), oldSetting);
                }
                
                // set to new value
                System.setProperty(setting.key().getScopedKey(), setting.value());
            }
        });
    }
    
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        extensionContext.getTestMethod().ifPresent(method -> {
            JvmSetting[] settings = method.getAnnotationsByType(JvmSetting.class);
            for (JvmSetting setting : settings) {
                // get a stored setting from context
                String oldSetting = getStore(extensionContext).remove(setting.key().getScopedKey(), String.class);
                // if present before, restore
                if (oldSetting != null) {
                    System.setProperty(setting.key().getScopedKey(), oldSetting);
                // if NOT present before, delete
                } else {
                    System.clearProperty(setting.key().getScopedKey());
                }
            }
        });
    }
}
