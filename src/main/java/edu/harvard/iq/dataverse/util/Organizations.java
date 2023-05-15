package edu.harvard.iq.dataverse.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

/**
 * Used by PersonOrOrgUtil
 * @author francesco.cadili@4science.it
 */
class Organizations {

    private static Organizations instance = null;

    private boolean[] loadOrganizationModels = {
        true,
        false // es-ner-organization.bin seams not work...
    };
    private boolean[] loadTokenizerModels = {
        true
    };

    private static final Logger logger = Logger.getLogger(FirstNames.class.getCanonicalName());
    private static List<NameFinderME> organizationNameFinders = new ArrayList<NameFinderME>();
    private static List<TokenizerME> tokenizers = new ArrayList<TokenizerME>();

    public static final String[] TAG_ORGANIZATIONS = {"organization"};
    public static String[] ORGANIZATION_MODELS = {
        "edu/harvard/iq/dataverse/firstNames/en-ner-organization.bin",
        "edu/harvard/iq/dataverse/firstNames/es-ner-organization.bin"
    };
    public static String[] TOCKENIZER_MODELS = {
        "edu/harvard/iq/dataverse/firstNames/en-token.bin"
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
        for (boolean loadTokenizerModel : loadTokenizerModels) {

            //Loading the Tokenizer model 
            if (loadTokenizerModel) {
                try {
                    loadTokenizerModel(TOCKENIZER_MODELS[pos]);
                } catch (IOException ioe) {
                    logger.log(Level.WARNING,
                            String.format("I cannot read model {0}, with name {1}",
                                    pos, TOCKENIZER_MODELS[pos]));
                }
            }
            pos++;
        }

        pos = 0;
        for (boolean loadOrganizationModel : loadOrganizationModels) {

            //Loading the NER-organization model 
            if (loadOrganizationModel) {
                try {
                    loadOrganizationModel(ORGANIZATION_MODELS[pos]);
                } catch (IOException ioe) {
                    logger.log(Level.WARNING,
                            String.format("I cannot read model {0}, with name {1}",
                                    pos, ORGANIZATION_MODELS[pos]));
                }
            }
            pos++;
        }
    }

    /**
     * Get ISO 639 alpha-2 language code.
     *
     * @param model Model name
     * @return The ISO code or null
     */
    public String getLanguage(String model) {
        if (StringUtil.nonEmpty(model)) {
            if (model.contains("/") && model.lastIndexOf("/") < model.length() - 1) {
                model = model.substring(model.lastIndexOf("/") + 1);
            }
            String[] tokens = model.split("-");

            Locale l = new Locale(tokens[0]);
            if (l != null) {
                return tokens[0];
            }
        }

        return null;
    }

    /**
     * Check if organization is recognized by models.
     *
     * @param organization
     * @return
     */
    public boolean isOrganization(String organization) {
        int pos = 0;
        for (TokenizerME tokenizer : tokenizers) {
            String language = getLanguage(TOCKENIZER_MODELS[pos]);
            
            if (isOrganization(organization, tokenizer, language)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if organization is recognized by models.
     *
     * @param organization Organization description
     * @param tokenizer Tokenizer
     * @param tokenizerLanguage tokenizer language or null
     * @return
     */
    public boolean isOrganization(String organization, TokenizerME tokenizer, String tokenizerLanguage) {
        String sentence[] = tokenizer.tokenize(organization);

        int pos = 0;    // organization Name position
        for (NameFinderME organizationNameFinder : organizationNameFinders) {
            if (StringUtil.isEmpty(tokenizerLanguage)
                    || tokenizerLanguage.equalsIgnoreCase(getLanguage(ORGANIZATION_MODELS[pos]))) {
                Span spans[] = organizationNameFinder.find(sentence);
                for (Span span : spans) {
                    for (String tagOrganization : TAG_ORGANIZATIONS) {
                        if (tagOrganization.equalsIgnoreCase(span.getType())) {
                            return true;
                        }
                    }
                }
            }
            pos++;
        }

        return false;
    }

    /**
     * Load Tokenizer Model
     *
     * @see <a href="http://opennlp.apache.org/models.html">models</a>
     *
     * @throws IOException
     */
    private void loadTokenizerModel(String modelFileName) throws IOException {
        InputStream fis = this.getClass().getClassLoader().getResourceAsStream(modelFileName);
        TokenizerModel tokenModel = new TokenizerModel(fis);

        tokenizers.add(new TokenizerME(tokenModel));
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
