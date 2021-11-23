package edu.harvard.iq.dataverse.api.helpers;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.api.UtilIT;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A JUnit5 extension to resolve a parameter of a test method and inject a {@link RandomUser} into it.
 * This user may be used to create/use other resources and subjects under test.
 */
public class RandomUserExtension implements ParameterResolver, AfterTestExecutionCallback {
    
    private static final Logger logger = Logger.getLogger(RandomUserExtension.class.getCanonicalName());
    
    private Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(getClass(), context.getRequiredTestClass(), context.getRequiredTestMethod()));
    }
    
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(RandomUser.class);
    }
    
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Store store = getStore(extensionContext);
        String parameterName = parameterContext.getParameter().getName();
        
        RandomUser user = getNewRandomUser();
        store.put(parameterName, user);
        return user;
    }
    
    /**
     * Create a new RandomUser via Dataverse API
     * Hint: package-private on purpose to allow reuse within other of our extensions
     *
     * @return a new random user (or nothing, when assumptions aren't met)
     */
    static RandomUser getNewRandomUser() {
        // request user via API
        Response createUser = UtilIT.createRandomUser();
    
        // assume created or abort
        assumeTrue(ContentType.fromContentType(createUser.contentType()) == ContentType.JSON,
            () -> "Return data for was no JSON: "+createUser.print());
        assumeTrue(createUser.getStatusCode() == HttpServletResponse.SC_OK,
            () -> "User creation failed with status "+createUser.getStatusCode()+": "+createUser.print());
    
        // store response as user
        RandomUser user = new RandomUser(createUser);
    
        // verify user
        assumeTrue(user.getUsername() != null, () -> "Could not find a username for successfully created user.");
        assumeFalse(user.getUsername().isEmpty(), () -> "Could not find a non-empty username for successfully created user.");
        assumeFalse(user.getUsername().isBlank(), () -> "Could not find a non-blank username for successfully created user.");
        assumeTrue(user.getApiToken() != null, () -> "Could not find an api token for successfully created user.");
        assumeFalse(user.getApiToken().isEmpty(), () -> "Could not find a non-empty api token for successfully created user.");
        assumeFalse(user.getApiToken().isBlank(), () -> "Could not find a non-blank api token for successfully created user.");
        
        return user;
    }
    
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        Store store = getStore(extensionContext);
        
        // iterate all parameters
        for (Parameter p : extensionContext.getRequiredTestMethod().getParameters()) {
            // if type matches ...
            if (p.getType() == RandomUser.class) {
                // ... retrieve user from store
                RandomUser user = store.get(p.getName(), RandomUser.class);
                // and delete if present
                if (user != null) {
                    deleteRandomUser(user);
                } else {
                    // if not present, log an info, as this seems not right (but might have happened when creation failed)
                    logger.info("Could not find a previously stored RandomUser for parameter "+p.getName());
                }
            }
        }
    }
    
    /**
     * Delete a RandomUser via Dataverse API
     * Hint: package-private on purpose to allow reuse within other of our extensions
     *
     * @param user A random user created via the Dataverse API before
     */
    static void deleteRandomUser(RandomUser user) {
        // delete via API
        logger.info("Deleting user "+user.getUsername());
        Response deleteUser = UtilIT.deleteUser(user.getUsername());
    
        // if this does not work, log a warning, but continue (try to delete the others)
        if (deleteUser.getStatusCode() != HttpServletResponse.SC_OK) {
            logger.warning("Could not delete user "+user.getUsername()+": "+deleteUser.print());
        }
    }
}
