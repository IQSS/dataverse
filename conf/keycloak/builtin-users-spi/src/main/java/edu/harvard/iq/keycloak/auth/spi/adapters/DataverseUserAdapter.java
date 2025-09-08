package edu.harvard.iq.keycloak.auth.spi.adapters;

import edu.harvard.iq.keycloak.auth.spi.models.DataverseUser;
import edu.harvard.iq.keycloak.auth.spi.providers.DataverseUserStorageProviderFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.stream.Stream;

public class DataverseUserAdapter extends AbstractUserAdapterFederatedStorage {

    protected DataverseUser dataverseUser;
    protected String keycloakId;

    private static final String ATTRIBUTE_NAME_IDP = "idp";

    public DataverseUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, DataverseUser dataverseUser) {
        super(session, realm, model);
        this.dataverseUser = dataverseUser;
        keycloakId = StorageId.keycloakId(model, dataverseUser.getBuiltinUser().getId().toString());
        this.setSingleAttribute(ATTRIBUTE_NAME_IDP, DataverseUserStorageProviderFactory.PROVIDER_ID);
    }

    @Override
    public void setUsername(String s) {
    }

    @Override
    public String getUsername() {
        return dataverseUser.getBuiltinUser().getUsername();
    }

    @Override
    public String getEmail() {
        return dataverseUser.getAuthenticatedUser().getEmail();
    }

    @Override
    public String getFirstName() {
        return dataverseUser.getAuthenticatedUser().getFirstName();
    }

    @Override
    public String getLastName() {
        return dataverseUser.getAuthenticatedUser().getLastName();
    }

    @Override
    public Stream<GroupModel> getGroupsStream(String search, Integer first, Integer max) {
        return super.getGroupsStream(search, first, max);
    }

    @Override
    public long getGroupsCount() {
        return super.getGroupsCount();
    }

    @Override
    public long getGroupsCountByNameContaining(String search) {
        return super.getGroupsCountByNameContaining(search);
    }

    @Override
    public String getId() {
        return keycloakId;
    }
}
