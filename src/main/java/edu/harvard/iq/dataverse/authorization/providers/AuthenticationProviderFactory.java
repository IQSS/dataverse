package edu.harvard.iq.dataverse.authorization.providers;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;

/**
 * This factory creates {@link AuthenticationProvider}s from {@link AuthenticationProviderRow}s.
 * Each factory has an alias. The alias also appears in the rows, and is used
 * to hand a row to a factory object that can understand it.
 * 
 * @author michael
 */
public interface AuthenticationProviderFactory {
    /**
     * The alias of a factory. Has to be unique in the system.
     * @return The alias of the factory.
     */
    String getAlias();
    
    /**
     * @return A human readable display string describing this factory.
     */
    String getInfo();
    
    /**
     * Instantiates an {@link AuthenticationProvider} based on the row passed.
     * @param aRow The row on which the created provider is based.
     * @return The provider
     * @throws AuthorizationSetupException If {@code aRow} contains malformed data.
     */
    AuthenticationProvider buildProvider( AuthenticationProviderRow aRow ) throws AuthorizationSetupException;
    
}
