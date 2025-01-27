package edu.harvard.iq.keycloak.auth.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.stream.Stream;

public class DataverseUserAdapter extends AbstractUserAdapterFederatedStorage {

    protected DataverseUser user;
    protected String keycloakId;

    public DataverseUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, DataverseUser user) {
        super(session, realm, model);
        this.user = user;
        keycloakId = StorageId.keycloakId(model, user.getId());
    }

    public String getEncryptedPassword() {
        return user.getEncryptedPassword();
    }

    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public void setUsername(String s) {

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
