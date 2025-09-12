package edu.harvard.iq.dataverse.util.template;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A Test Data Builder for the Template entity.
 * <p>
 * Usage:
 * {@code Template template = aTemplate().withName("My Test").build();}
 * {@code Template defaultTemplate = aTemplate().isDefault(true).build();}
 */
public final class TemplateBuilder {

    private String name = "Test Template";
    private boolean isDefaultForDataverse = false;
    private final Long usageCount = 5L;
    private final Timestamp createTime = new Timestamp(new Date().getTime());
    private final Map<String, String> instructionsMap = new HashMap<>(Map.of(
            "author", "Enter the author's name here.",
            "title", "Provide a title for the dataset."
    ));

    private final String dataverseAlias = "test-dataverse";
    private final String termsOfUse = "Test Terms of Use";
    private final String termsOfAccess = "Test Terms of Access";

    /**
     * Entry point for creating a Template.
     */
    public static TemplateBuilder aTemplate() {
        return new TemplateBuilder();
    }

    /**
     * Builds the final Template object from the current builder state.
     */
    public Template build() {
        Dataverse dataverse = new Dataverse();
        dataverse.setAlias(dataverseAlias);

        TermsOfUseAndAccess terms = buildTermsOfUseAndAccess();

        Template template = new Template();
        template.setName(name);
        template.setIsDefaultForDataverse(isDefaultForDataverse);
        template.setUsageCount(usageCount);
        template.setCreateTime(createTime);
        template.setInstructionsMap(instructionsMap);
        template.setDatasetFields(new ArrayList<>());

        template.setDataverse(dataverse);
        template.setTermsOfUseAndAccess(terms);

        return template;
    }

    private TermsOfUseAndAccess buildTermsOfUseAndAccess() {
        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setId(1L);
        terms.setTermsOfUse(termsOfUse);
        terms.setTermsOfAccess(termsOfAccess);
        terms.setConfidentialityDeclaration("Test Confidentiality Declaration");
        terms.setSpecialPermissions("Test Special Permissions");
        terms.setRestrictions("Test Restrictions");
        terms.setCitationRequirements("Test Citation Requirements");
        terms.setDepositorRequirements("Test Depositor Requirements");
        terms.setConditions("Test Conditions");
        terms.setDisclaimer("Test Disclaimer");
        terms.setDataAccessPlace("Test Data Access Place");
        terms.setOriginalArchive("Test Original Archive");
        terms.setAvailabilityStatus("Test Availability Status");
        terms.setSizeOfCollection("Test Size of Collection");
        terms.setStudyCompletion("Test Study Completion");
        return terms;
    }

    public TemplateBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public TemplateBuilder isDefault(boolean isDefault) {
        this.isDefaultForDataverse = isDefault;
        return this;
    }
}
