package edu.harvard.iq.keycloak.auth.spi.providers;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class DataverseUserStorageProviderFactory implements UserStorageProviderFactory<DataverseUserStorageProvider> {

    public static final String PROVIDER_ID = "dv-builtin-users-authenticator";

    private static final Logger logger = Logger.getLogger(DataverseUserStorageProviderFactory.class);

    @Override
    public DataverseUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new DataverseUserStorageProvider(session, model);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "A Keycloak Storage Provider to authenticate Dataverse Builtin Users";
    }

    @Override
    public void close() {
        logger.debug("<<<<<< Closing factory");
    }
}
