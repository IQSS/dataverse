package edu.harvard.iq.dataverse.util.testing;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class SystemPropertyExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    
    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestClass(), context.getRequiredTestMethod()));
    }
    
    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        extensionContext.getTestMethod().ifPresent(method -> {
            SystemProperty[] settings = method.getAnnotationsByType(SystemProperty.class);
            for (SystemProperty setting : settings) {
                // get the property name
                String settingName = setting.key();
                
                // get the setting ...
                String oldSetting = System.getProperty(settingName);
                
                // if present - store in context to restore later
                if (oldSetting != null) {
                    getStore(extensionContext).put(settingName, oldSetting);
                }
                
                // set to new value
                System.setProperty(settingName, setting.value());
            }
        });
    }
    
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        extensionContext.getTestMethod().ifPresent(method -> {
            SystemProperty[] settings = method.getAnnotationsByType(SystemProperty.class);
            for (SystemProperty setting : settings) {
                /// get the property name
                String settingName = setting.key();
                
                // get a stored setting from context
                String oldSetting = getStore(extensionContext).remove(settingName, String.class);
                
                // if present before, restore
                if (oldSetting != null) {
                    System.setProperty(settingName, oldSetting);
                    // if NOT present before, delete
                } else {
                    System.clearProperty(settingName);
                }
            }
        });
    }
}
