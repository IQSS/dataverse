package edu.harvard.iq.dataverse.api.helpers;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.api.UtilIT;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.api.helpers.ExtensionStoreHelper.addToStoredList;
import static edu.harvard.iq.dataverse.api.helpers.ExtensionStoreHelper.getAfterAllScope;
import static edu.harvard.iq.dataverse.api.helpers.ExtensionStoreHelper.getAfterEachScope;
import static edu.harvard.iq.dataverse.api.helpers.ExtensionStoreHelper.getStore;
import static edu.harvard.iq.dataverse.api.helpers.ExtensionStoreHelper.getStoredList;
import static edu.harvard.iq.dataverse.api.helpers.ExtensionStoreHelper.isAfterMethod;
import static edu.harvard.iq.dataverse.api.helpers.ExtensionStoreHelper.isBeforeMethod;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A JUnit5 extension to resolve a parameter of a test method and inject a {@link RandomUser} into it.
 * This user may be used to create/use other resources and subjects under test.
 */
public class RandomUserExtension implements ParameterResolver, AfterTestExecutionCallback, AfterEachCallback, AfterAllCallback {
    
    private static final Logger logger = Logger.getLogger(RandomUserExtension.class.getCanonicalName());
    private static final String listKey = "generated";
    
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(RandomUser.class);
    }
    
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Store store = getStore(getClass(), extensionContext, parameterContext);
        String parameterName = parameterContext.getParameter().getName();
        RandomUser user;
        
        // if this a request for a parameter from an "@AfterEach" or "@AfterAll" method, send back the stored data
        // from a @BeforeEach/@BeforeAll method (or fail if not found, as this is a user error)
        if (isAfterMethod(parameterContext)) {
            user = store.get(parameterName, RandomUser.class);
            if (user == null) {
                throw new ParameterResolutionException("Cannot resolve random user in @AfterEach/All not stored in @BeforeEach/All");
            }
        } else {
            user = getNewRandomUser();
            store.put(parameterName, user);
            
            // in case of a before method, we need to store the users in a list _AS WELL_ - we cannot retrieve parameter
            // names from afterEachCallback and afterAllCallback as there are no methods involved! still the users
            // need deletion from the Dataverse instance...
            // Note: the store is already scoped with the annotation - no need to differ between BeforeEach/All here
            if (isBeforeMethod(parameterContext)) {
                addToStoredList(store, listKey, user);
            }
        }
        
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
    
    /**
     * Delete a RandomUser via Dataverse API
     * Hint: package-private on purpose to allow reuse within other of our extensions
     *
     * @param user The RandomUser to delete
     */
    static void deleteRandomUser(RandomUser user) {
        if (user != null) {
            // delete via API
            logger.info("Deleting user "+user.getUsername());
            Response deleteUser = UtilIT.deleteUser(user.getUsername());
    
            // if this does not work, log a warning, but continue (try to delete the others)
            if (deleteUser.getStatusCode() != HttpServletResponse.SC_OK) {
                logger.warning("Could not delete user "+user.getUsername()+": "+deleteUser.print());
            }
        } else {
            logger.warning("Cannot delete a null RandomUser object");
        }
    }
    
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        Store store = getStore(getClass(), extensionContext, extensionContext.getRequiredTestMethod());
        
        // iterate all parameters
        for (Parameter p : extensionContext.getRequiredTestMethod().getParameters()) {
            // if type matches ...
            if (p.getType() == RandomUser.class) {
                // retrieve user from store
                RandomUser user = store.get(p.getName(), RandomUser.class);
                if (user != null) {
                    // delete the user
                    deleteRandomUser(user);
                } else {
                    // if not present, log an info, as this seems not right (but might happen when creation failed)
                    logger.info("Could not find a previously stored RandomUser for parameter "+p.getName());
                }
            }
        }
    }
    
    static void deleteRandomUserList(Store store) {
        List<RandomUser> users = getStoredList(store, listKey);
        for (RandomUser user : users) {
            deleteRandomUser(user);
        }
    }
    
    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        Store store = getStore(getClass(), extensionContext, getAfterEachScope());
        deleteRandomUserList(store);
    }
    
    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        Store store = getStore(getClass(), extensionContext, getAfterAllScope());
        deleteRandomUserList(store);
    }
}
