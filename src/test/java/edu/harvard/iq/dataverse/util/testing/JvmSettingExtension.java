package edu.harvard.iq.dataverse.util.testing;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;

import static edu.harvard.iq.dataverse.util.testing.JvmSetting.PLACEHOLDER;

public class JvmSettingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, BeforeAllCallback, AfterAllCallback {
    
    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        List<JvmSetting> settings = AnnotationSupport.findRepeatableAnnotations(extensionContext.getTestClass(), JvmSetting.class);
        ExtensionContext.Store store = extensionContext.getStore(
            ExtensionContext.Namespace.create(getClass(), extensionContext.getRequiredTestClass()));
        
        setSetting(extensionContext.getRequiredTestClass(), settings, getBroker(extensionContext), store);
    }
    
    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        List<JvmSetting> settings = AnnotationSupport.findRepeatableAnnotations(extensionContext.getTestClass(), JvmSetting.class);
        ExtensionContext.Store store = extensionContext.getStore(
            ExtensionContext.Namespace.create(getClass(), extensionContext.getRequiredTestClass()));
        
        resetSetting(settings, getBroker(extensionContext), store);
    }
    
    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        List<JvmSetting> settings = AnnotationSupport.findRepeatableAnnotations(extensionContext.getTestMethod(), JvmSetting.class);
        ExtensionContext.Store store = extensionContext.getStore(
            ExtensionContext.Namespace.create(
                getClass(),
                extensionContext.getRequiredTestClass(),
                extensionContext.getRequiredTestMethod()
            ));
        
        setSetting(extensionContext.getRequiredTestClass(), settings, getBroker(extensionContext), store);
    }
    
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        List<JvmSetting> settings = AnnotationSupport.findRepeatableAnnotations(extensionContext.getTestMethod(), JvmSetting.class);
        ExtensionContext.Store store = extensionContext.getStore(
            ExtensionContext.Namespace.create(
                getClass(),
                extensionContext.getRequiredTestClass(),
                extensionContext.getRequiredTestMethod()
            ));
        
        resetSetting(settings, getBroker(extensionContext), store);
    }
    
    private void setSetting(Class<?> testClass, List<JvmSetting> settings, JvmSettingBroker broker, ExtensionContext.Store store) throws Exception {
        for (JvmSetting setting : settings) {
            // get the setting name (might need var args substitution)
            String settingName = getSettingName(setting);
            
            // get the setting value ...
            String oldSetting = broker.getJvmSetting(settingName);
            
            // if present - store in context to restore later
            if (oldSetting != null) {
                store.put(settingName, oldSetting);
            }
            
            // set to new value
            if (setting.value().equals(PLACEHOLDER) && setting.method().equals(PLACEHOLDER)) {
                throw new IllegalArgumentException("You must either provide a value or a method reference " +
                    "for key JvmSettings" + setting.key());
            }
            
            String value;
            // Retrieve value from static (!) test class method if no direct setting given
            if (setting.value().equals(PLACEHOLDER)) {
                Optional<Method> valueMethod = ReflectionSupport.findMethod(testClass, setting.method());
                if (valueMethod.isEmpty() || ! Modifier.isStatic(valueMethod.get().getModifiers())) {
                    throw new IllegalStateException("Could not find a static method '" + setting.method() + "' in test class");
                }
                value = (String) ReflectionSupport.invokeMethod(valueMethod.get(), null);
            // Set to new value by using the directly given value
            } else {
                value = setting.value();
            }
            
            // If the retrieved value is null, delete the setting (will be reset after the test), otherwise set.
            if (value != null) {
                broker.setJvmSetting(settingName, value);
            } else if (oldSetting != null) {
                broker.deleteJvmSetting(settingName);
            }
        }
    }
    
    private void resetSetting(List<JvmSetting> settings, JvmSettingBroker broker, ExtensionContext.Store store) throws Exception {
        for (JvmSetting setting : settings) {
            // get the setting name (might need var args substitution)
            String settingName = getSettingName(setting);
            
            // get a stored setting from context
            String oldSetting = store.remove(settingName, String.class);
            
            // if present before, restore
            if (oldSetting != null) {
                broker.setJvmSetting(settingName, oldSetting);
                // if NOT present before, delete
            } else {
                broker.deleteJvmSetting(settingName);
            }
        }
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
    
    private JvmSettingBroker getBroker(ExtensionContext extensionContext) throws Exception {
        // Is this test class using local system properties, then get a broker for these
        if (AnnotationSupport.isAnnotated(extensionContext.getTestClass(), LocalJvmSettings.class)) {
            return LocalJvmSettings.localBroker;
        // NOTE: this might be extended later with other annotations to support other means of handling the settings
        } else {
            throw new IllegalStateException("You must provide the @LocalJvmSettings annotation to the test class");
        }
    }
    
}
