package edu.harvard.iq.keycloak.auth.spi;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class UserEventListenerProviderFactory implements EventListenerProviderFactory {

    @Override
    public String getId() {
        return "user-event-listener";
    }

    @Override
    public int order() {
        return EventListenerProviderFactory.super.order();
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return EventListenerProviderFactory.super.getConfigMetadata();
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new UserEventListenerProvider(session);
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Post-init if needed
    }

    @Override
    public void close() {
        // Close if needed
    }
}
