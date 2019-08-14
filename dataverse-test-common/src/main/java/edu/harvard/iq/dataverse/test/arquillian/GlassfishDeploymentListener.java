package edu.harvard.iq.dataverse.test.arquillian;

import org.jboss.arquillian.container.spi.event.SetupContainer;
import org.jboss.arquillian.container.spi.event.StopContainer;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * Listener of arquillian events responsible for overriding glassfish configuration
 * when running tests.
 * 
 * @author madryk
 * @see ParametrizedGlassfishConfCreator
 */
public class GlassfishDeploymentListener {

    private ParametrizedGlassfishConfCreator glassfishConfCreator = new ParametrizedGlassfishConfCreator();
    
    // -------------------- LOGIC --------------------
    
    public void onSetupContainer(@Observes(precedence = 10000) SetupContainer event) {
        glassfishConfCreator.createTempGlassfishResources();
    }
    
    public void onStopContainer(@Observes StopContainer event) {
        glassfishConfCreator.cleanTempGlassfishResource();
    }

}
