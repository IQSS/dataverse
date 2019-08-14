package edu.harvard.iq.dataverse.test.arquillian;

import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * Arquillian extension that allows to configure
 * glassfish resources using external properties
 * file.
 * 
 * @author madryk
 * @see ParametrizedGlassfishConfCreator
 */
public class GlassfishDeploymentExtension implements LoadableExtension {

    // -------------------- LOGIC --------------------
    
    @Override
    public void register(final ExtensionBuilder builder) {
        builder.observer(GlassfishDeploymentListener.class);
    }
}