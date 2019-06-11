package edu.harvard.iq.dataverse.arquillian;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

/**
 * Class responsible for running integration tests in dataverse.
 */
public class DataverseArquillian extends Arquillian {

    public DataverseArquillian(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    /**
     * Does exactly the same thing as Arquillian with addition of creating temporary resource file for glassfish.
     *
     * @param notifier
     */
    @Override
    public void run(RunNotifier notifier) {
        ParametrizedGlassfishConfCreator arquillianGlassfishConfigurator =
                new ParametrizedGlassfishConfCreator();

        arquillianGlassfishConfigurator.createTempGlassfishResources();
        super.run(notifier);
    }
}
