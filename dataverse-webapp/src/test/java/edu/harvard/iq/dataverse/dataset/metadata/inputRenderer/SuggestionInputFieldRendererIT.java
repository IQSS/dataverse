package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.GrantSuggestionDao;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.GrantSuggestionHandler;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;


@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class SuggestionInputFieldRendererIT extends WebappArquillianDeployment {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private GrantSuggestionDao grantSuggestionDao;

    private SuggestionInputFieldRenderer suggestionInputFieldRenderer;

    // -------------------- TESTS --------------------

    @Test
    public void createSuggestions_WithoutFilters() {
        //given
        suggestionInputFieldRenderer = new SuggestionInputFieldRenderer(new GrantSuggestionHandler(grantSuggestionDao),
                                                                        new ArrayList<>(),
                                                                        "grantNumberAgency");

        //when
        List<String> turkishSuggestion = suggestionInputFieldRenderer.createSuggestions(new DatasetField(), "Turk");
        List<String> turkishSuggestionWithMiddleString = suggestionInputFieldRenderer.createSuggestions(new DatasetField(),
                                                                                                        "urk");
        List<String> allGrantAgencies = suggestionInputFieldRenderer.createSuggestions(new DatasetField(), "age");

        //then
        Assert.assertEquals("Turkish Agency", turkishSuggestion.get(0));
        Assert.assertEquals("Turkish Agency", turkishSuggestionWithMiddleString.get(0));
        Assert.assertEquals(4, allGrantAgencies.size());
    }

    @Test
    public void createSuggestions_WithFilters() {
        //given
        suggestionInputFieldRenderer = new SuggestionInputFieldRenderer(new GrantSuggestionHandler(grantSuggestionDao),
                                                                        Lists.newArrayList("grantNumberAgency"),
                                                                        "grantNumberProgram");

        //when
        List<String> testAgencySuggestion = suggestionInputFieldRenderer.createSuggestions(generateDsfFamily(
                "grantNumberAgency",
                "Test Agency"), "FR");

        List<String> turkishSuggestion = suggestionInputFieldRenderer.createSuggestions(
                generateDsfFamily(
                        "grantNumberAgency",
                        "Turkish Agency"),
                "eu");

        List<String> emptySuggestion = suggestionInputFieldRenderer.createSuggestions(
                generateDsfFamily(
                        "grantNumberAgency",
                        "EMPTY"),
                "eu");

        //then
        Assert.assertEquals("FREE", testAgencySuggestion.get(0));
        Assert.assertEquals("EURO", turkishSuggestion.get(0));
        Assert.assertEquals(0, emptySuggestion.size());
    }

    // -------------------- PRIVATE --------------------

    public DatasetField generateDsfFamily(String dsftName, String fieldValue) {
        DatasetField parentField = MocksFactory.makeEmptyDatasetField(MocksFactory.makeDatasetFieldType(), 0);
        DatasetField sourceField = MocksFactory.makeEmptyDatasetField(MocksFactory.makeDatasetFieldType(), 0);
        DatasetField valueField = MocksFactory.makeEmptyDatasetField(MocksFactory.makeDatasetFieldType(dsftName,
                                                                                                       FieldType.TEXT,
                                                                                                       false,
                                                                                                       new MetadataBlock()),
                                                                     0);
        valueField.setFieldValue(fieldValue);

        sourceField.setDatasetFieldParent(parentField);
        parentField.setDatasetFieldsChildren(Lists.newArrayList(valueField, sourceField));

        return sourceField;
    }
}