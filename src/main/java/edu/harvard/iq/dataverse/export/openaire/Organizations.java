package edu.harvard.iq.dataverse.export.openaire;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

/**
 *
 * @author francesco.cadili@4science.it
 */
public class Organizations {

    private static Organizations instance = null;

    private boolean[] loadOrganizationModels = {
        true,
        false // es-ner-organization.bin seams not work...
    };

    private static final Logger logger = Logger.getLogger(FirstNames.class.getCanonicalName());
    private static List<NameFinderME> organizationNameFinders = new ArrayList<NameFinderME>();

    public static final String[] TAG_ORGANIZATIONS = {"organization"};
    public static String[] ORGNANIZATION_MODELS = {
        "edu/harvard/iq/dataverse/firstNames/en-ner-organization.bin",
        "edu/harvard/iq/dataverse/firstNames/es-ner-organization.bin"
    };

    /**
     * Singleton method
     *
     * @return The organizations object
     */
    public static synchronized Organizations getInstance() {
        if (instance == null) {
            instance = new Organizations();
        }

        return instance;
    }

    /**
     * Initialize organization model.
     *
     */
    private Organizations() {
        int pos = 0;
        for (boolean loadEnOrganizationModel : loadOrganizationModels) {

            //Loading the NER-organization model 
            if (loadEnOrganizationModel) {
                try {
                    loadOrganizationModel(ORGNANIZATION_MODELS[pos]);
                } catch (IOException ioe) {
                    logger.log(Level.WARNING,
                            String.format("I cannot read model {0}, with name {1}",
                                    pos, ORGNANIZATION_MODELS[pos]));
                }
            }
            pos++;
        }
    }

    /**
     * Check if organization is recognized by models.
     *
     * @param organization
     * @return
     */
    public boolean isOrganization(String organization) {
        organization = Cleanup.normalize(organization);

        String[] sentence = organization.split(" ");

        for (NameFinderME organizationNameFinder : organizationNameFinders) {
            Span spans[] = organizationNameFinder.find(sentence);
            for (Span span : spans) {
                for (String tagOrganization : TAG_ORGANIZATIONS) {
                    if (tagOrganization.equalsIgnoreCase(span.getType())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Load Organization Model
     *
     * @see <a href="http://opennlp.apache.org/models.html">models</a>
     *
     * @throws IOException
     */
    private void loadOrganizationModel(String modelFileName) throws IOException {
        InputStream fis = this.getClass().getClassLoader().getResourceAsStream(modelFileName);
        TokenNameFinderModel organizationModel = new TokenNameFinderModel(fis);

        organizationNameFinders.add(new NameFinderME(organizationModel));
    }
}
