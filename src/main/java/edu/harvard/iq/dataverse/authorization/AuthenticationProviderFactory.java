package edu.harvard.iq.dataverse.authorization;

/**
 * Creates authentication providers. Used by the {@link AuthenticationServiceBean} to
 * initialize the authentication providers upon startup.
 * 
 * @author michael
 */
public interface AuthenticationProviderFactory {
   public String getName(); 
   public AuthenticationProvider create( AuthenticationProviderRecord input );
}
