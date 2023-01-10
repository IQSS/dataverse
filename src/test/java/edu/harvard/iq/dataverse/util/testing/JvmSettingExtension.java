package edu.harvard.iq.dataverse.util.testing;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static edu.harvard.iq.dataverse.util.testing.JvmSetting.PLACEHOLDER;

public class JvmSettingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    
    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestClass(), context.getRequiredTestMethod()));
    }
    
    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        extensionContext.getTestMethod().ifPresent(method -> {
            JvmSetting[] settings = method.getAnnotationsByType(JvmSetting.class);
            for (JvmSetting setting : settings) {
                // get the setting name (might need var args substitution)
                String settingName = getSettingName(setting);
                
                // get the setting ...
                String oldSetting = System.getProperty(settingName);
    
                // if present - store in context to restore later
                if (oldSetting != null) {
                    getStore(extensionContext).put(settingName, oldSetting);
                }
                
                // set to new value
                if (setting.value().equals(PLACEHOLDER) && setting.method().equals(PLACEHOLDER)) {
                    throw new IllegalArgumentException("You must either provide a value or a method reference " +
                        "for key JvmSettings." + setting.key());
                }
                
                // retrieve value from static test class method if no setting given
                if (setting.value().equals(PLACEHOLDER)) {
                    extensionContext.getTestClass().ifPresent(klass -> {
                        try {
                            Method valueMethod = klass.getDeclaredMethod(setting.method());
                            valueMethod.setAccessible(true);
                            System.setProperty(settingName, (String)valueMethod.invoke(null));
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    System.setProperty(settingName, setting.value());
                }
            }
        });
    }
    
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        extensionContext.getTestMethod().ifPresent(method -> {
            JvmSetting[] settings = method.getAnnotationsByType(JvmSetting.class);
            for (JvmSetting setting : settings) {
                // get the setting name (might need var args substitution)
                String settingName = getSettingName(setting);
                
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
    
    private String getSettingName(JvmSetting setting) {
        JvmSettings target = setting.key();
        
        if (target.needsVarArgs()) {
            String[] variableArguments = setting.varArgs();
            
            if (variableArguments == null || variableArguments.length != target.numberOfVarArgs()) {
                throw new IllegalArgumentException("You must provide " + target.numberOfVarArgs() +
                    " variable arguments via varArgs = {...} for setting " + target +
                    " (\"" + target.getScopedKey() + "\")");
            }
            
            return target.insert(variableArguments);
        }
        
        return target.getScopedKey();
    }
}
