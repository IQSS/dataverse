package edu.harvard.iq.dataverse.util.testing;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.List;

public class FeatureFlagExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, BeforeAllCallback, AfterAllCallback {
    
    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        List<FeatureFlag> flags = AnnotationSupport.findRepeatableAnnotations(extensionContext.getTestClass(), FeatureFlag.class);
        ExtensionContext.Store store = extensionContext.getStore(
            ExtensionContext.Namespace.create(getClass(), extensionContext.getRequiredTestClass()));
        
        setFlag(extensionContext.getRequiredTestClass(), flags, getBroker(extensionContext), store);
    }
    
    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        List<FeatureFlag> flags = AnnotationSupport.findRepeatableAnnotations(extensionContext.getTestClass(), FeatureFlag.class);
        ExtensionContext.Store store = extensionContext.getStore(
            ExtensionContext.Namespace.create(getClass(), extensionContext.getRequiredTestClass()));
        
        resetFlag(flags, getBroker(extensionContext), store);
    }
    
    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        List<FeatureFlag> flags = AnnotationSupport.findRepeatableAnnotations(extensionContext.getTestMethod(), FeatureFlag.class);
        ExtensionContext.Store store = extensionContext.getStore(
            ExtensionContext.Namespace.create(
                getClass(),
                extensionContext.getRequiredTestClass(),
                extensionContext.getRequiredTestMethod()
            ));
        
        setFlag(extensionContext.getRequiredTestClass(), flags, getBroker(extensionContext), store);
    }
    
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        List<FeatureFlag> flags = AnnotationSupport.findRepeatableAnnotations(extensionContext.getTestMethod(), FeatureFlag.class);
        ExtensionContext.Store store = extensionContext.getStore(
            ExtensionContext.Namespace.create(
                getClass(),
                extensionContext.getRequiredTestClass(),
                extensionContext.getRequiredTestMethod()
            ));
        
        resetFlag(flags, getBroker(extensionContext), store);
    }
    
    private void setFlag(Class<?> testClass, List<FeatureFlag> flags, FeatureFlagBroker broker, ExtensionContext.Store store) throws Exception {
        for (FeatureFlag flag : flags) {
            // get the current state
            Boolean oldState = broker.get(flag.flag());
            
            // if present - store in context to restore later
            if (oldState != null) {
                store.put(flag, oldState);
            }
            
            broker.set(flag.flag(), flag.value());
        }
    }
    
    private void resetFlag(List<FeatureFlag> flags, FeatureFlagBroker broker, ExtensionContext.Store store) throws Exception {
        for (FeatureFlag flag : flags) {
            // get a stored setting from context
            Boolean oldState = store.remove(flag, Boolean.class);
            
            // if present before, restore
            if (oldState != null) {
                broker.set(flag.flag(), oldState);
                // if NOT present before, delete
            } else {
                broker.delete(flag.flag());
            }
        }
    }
    
    private FeatureFlagBroker getBroker(ExtensionContext extensionContext) throws Exception {
        // Is this test class using local system properties, then get a broker for these
        if (AnnotationSupport.isAnnotated(extensionContext.getTestClass(), LocalFeatureFlags.class)) {
            return LocalFeatureFlags.localBroker;
            // NOTE: this might be extended later with other annotations to support other means of handling the settings
        } else {
            throw new IllegalStateException("You must provide the @LocalFeatureFlags annotation to the test class");
        }
    }
    
}
