package edu.harvard.iq.dataverse.api.helpers;

import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.api.UtilIT;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

public class DBSettingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    
    private static final Logger logger = Logger.getLogger(DBSettingExtension.class.getCanonicalName());
    
    private Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(getClass(), context.getRequiredTestClass(), context.getRequiredTestMethod()));
    }
    
    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        extensionContext.getTestMethod().ifPresent(method -> {
            DBSetting[] settings = method.getAnnotationsByType(DBSetting.class);
            for (DBSetting setting : settings) {
                // get the setting ...
                Response getSetting = UtilIT.getSetting(setting.name());
                if (getSetting.getStatusCode() == HttpServletResponse.SC_OK) {
                    String oldSetting = getSetting.getBody().jsonPath().getString("data.message");
                    logger.fine("Found former value \""+oldSetting+"\" for "+setting.name()+".");
                    // ... and store in context to restore later
                    getStore(extensionContext).put(setting.name(), oldSetting);
                } else if (getSetting.getStatusCode() == HttpServletResponse.SC_NOT_FOUND) {
                    logger.fine("No former value for "+setting.name()+" present.");
                    // do nothing - will delete setting later
                } else {
                    Assumptions.assumeTrue(
                        getSetting.getStatusCode() == HttpServletResponse.SC_OK ||
                        getSetting.getStatusCode() == HttpServletResponse.SC_NOT_FOUND,
                        () -> "Aborting test: requesting setting \""+setting.name()+"\" failed with status "+getSetting.getStatusCode());
                }
                
                // set to new value
                Response setSetting = UtilIT.setSetting(setting.name(), setting.value());
                
                // assume set or abort
                Assumptions.assumeTrue(setSetting.getStatusCode() == HttpServletResponse.SC_OK,
                    () -> "Aborting test: setting failed for \""+setting.name()+"\" with status "+setSetting.getStatusCode());
    
                logger.fine("Set "+setting.name()+" to \""+setting.value()+"\".");
            }
        });
    }
    
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        extensionContext.getTestMethod().ifPresent(method -> {
            DBSetting[] settings = method.getAnnotationsByType(DBSetting.class);
            for (DBSetting setting : settings) {
                // get a stored setting from context
                String oldSetting = getStore(extensionContext).remove(setting.name(), String.class);
                // if present before, restore
                if (oldSetting != null) {
                    logger.fine("Restoring setting "+setting.name()+" during cleanup to value \""+oldSetting+"\".");
                    Response restoreSetting = UtilIT.setSetting(setting.name(), oldSetting);
                    restoreSetting.then().assertThat().statusCode(HttpServletResponse.SC_OK);
                // if NOT present before, delete
                } else {
                    logger.fine("Deleting setting "+setting.name()+" during cleanup.");
                    Response deleteSetting = UtilIT.deleteSetting(setting.name());
                    deleteSetting.then().assertThat().statusCode(HttpServletResponse.SC_OK);
                }
            }
        });
    }
}
