package edu.harvard.iq.keycloak.auth.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.stream.Stream;

public class DataverseUserAdapter extends AbstractUserAdapterFederatedStorage {

    protected DataverseBuiltinUser builtinUser;
    protected DataverseAuthenticatedUser authenticatedUser;
    protected String keycloakId;

    public DataverseUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, DataverseBuiltinUser builtinUser, DataverseAuthenticatedUser authenticatedUser) {
        super(session, realm, model);
        this.builtinUser = builtinUser;
        this.authenticatedUser = authenticatedUser;
        keycloakId = StorageId.keycloakId(model, builtinUser.getId().toString());
    }

    @Override
    public void setUsername(String s) {
    }

    @Override
    public String getUsername() {
        return builtinUser.getUsername();
    }

    @Override
    public String getEmail() {
        return authenticatedUser.getEmail();
    }

    @Override
    public String getFirstName() {
        return authenticatedUser.getFirstName();
    }

    @Override
    public String getLastName() {
        return authenticatedUser.getLastName();
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
