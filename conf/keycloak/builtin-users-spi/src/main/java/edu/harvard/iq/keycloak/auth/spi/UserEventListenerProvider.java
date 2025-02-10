package edu.harvard.iq.keycloak.auth.spi;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.events.EventType;

import org.jboss.logging.Logger;

public class UserEventListenerProvider implements EventListenerProvider {
    private static final Logger logger = Logger.getLogger(UserEventListenerProvider.class);
    private final KeycloakSession session;

    public UserEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == EventType.LOGIN || event.getType() == EventType.REGISTER) {
            logger.infof("Event captured: %s for user: %s", event.getType(), event.getUserId());
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean b) {
    }

    @Override
    public void close() {}
}
