package edu.harvard.iq.dataverse.search;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.search.advanced.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.NumberSearchField;
import edu.harvard.iq.dataverse.search.advanced.SearchBlock;
import edu.harvard.iq.dataverse.search.advanced.SearchField;
import edu.harvard.iq.dataverse.search.advanced.SolrQueryCreator;
import edu.harvard.iq.dataverse.search.advanced.TextSearchField;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

class SolrQueryCreatorTest {

    private SolrQueryCreator solrQueryCreator = new SolrQueryCreator();

    @Test
    public void constructQuery_TextQuery() {
        //given
        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", createTextSearchFields());
        //when
        String result = solrQueryCreator.constructQuery(Lists.newArrayList((searchBlock)));
        //then
        Assert.assertEquals("text1:testValue1 AND text2:testValue2", result);
    }

    @Test
    public void constructQuery_TextQueryWithLongText() {
        //given
        List<SearchField> textSearchFields = createLongTextSearchFields();

        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", textSearchFields);
        //when
        String result = solrQueryCreator.constructQuery(Lists.newArrayList((searchBlock)));
        //then
        Assert.assertEquals("text1:very AND text1:long AND text1:text AND text1:one AND " +
                                    "text2:very AND text2:long AND text2:text AND text2:two", result);
    }

    @Test
    public void constructQuery_BothNumbersQuery() {
        //given
        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", createBothNumbersSearchFields());
        //when
        String result = solrQueryCreator.constructQuery(Lists.newArrayList((searchBlock)));
        //then
        Assert.assertEquals("number1:[1 TO 2] AND number2:[3.1 TO 4.1]", result);
    }

    @Test
    public void constructQuery_OneNumberQuery() {
        //given
        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", createOneNumberSearchFields());
        //when
        String result = solrQueryCreator.constructQuery(Lists.newArrayList((searchBlock)));
        //then
        Assert.assertEquals("number1:[1 TO *] AND number2:[* TO 4.1]", result);
    }

    @Test
    public void constructQuery_CheckboxQuery() {
        //given
        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", createCheckboxSearchFields());
        //when
        String result = solrQueryCreator.constructQuery(Lists.newArrayList((searchBlock)));
        //then
        Assert.assertEquals("checkboxValues:\"checkboxValue1\" AND checkboxValues:\"checkboxValue2\"", result);
    }

    @Test
    public void constructQuery_CombinedQuery() {
        //given
        List<SearchField> searchFields = new ArrayList<>();
        searchFields.addAll(createBothNumbersSearchFields());
        searchFields.addAll(createOneNumberSearchFields());
        searchFields.addAll(createCheckboxSearchFields());
        searchFields.addAll(createTextSearchFields());

        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", searchFields);
        //when
        String result = solrQueryCreator.constructQuery(Lists.newArrayList((searchBlock)));
        //then
        Assert.assertEquals("number1:[1 TO 2] AND number2:[3.1 TO 4.1] AND number1:[1 TO *] AND" +
                                    " number2:[* TO 4.1] AND checkboxValues:\"checkboxValue1\" AND checkboxValues:\"checkboxValue2\" AND" +
                                    " text1:testValue1 AND text2:testValue2", result);
    }

    private List<SearchField> createBothNumbersSearchFields() {
        NumberSearchField testValue1 = new NumberSearchField("number1", "number1", "desc");
        testValue1.setMinimum(new BigDecimal(1));
        testValue1.setMaximum(new BigDecimal(2));

        NumberSearchField testValue2 = new NumberSearchField("number2", "number2", "desc");
        testValue2.setMinimum(new BigDecimal("3.1"));
        testValue2.setMaximum(new BigDecimal("4.1"));

        return Lists.newArrayList(testValue1, testValue2);
    }

    private List<SearchField> createOneNumberSearchFields() {
        NumberSearchField testValue1 = new NumberSearchField("number1", "number1", "desc");
        testValue1.setMinimum(new BigDecimal(1));

        NumberSearchField testValue2 = new NumberSearchField("number2", "number2", "desc");
        testValue2.setMaximum(new BigDecimal("4.1"));

        return Lists.newArrayList(testValue1, testValue2);
    }

    private List<SearchField> createCheckboxSearchFields() {
        CheckboxSearchField testValue1 = new CheckboxSearchField("checkboxValues", "checkboxValues", "desc");
        testValue1.getCheckedFieldValues().addAll(Lists.newArrayList("checkboxValue1", "checkboxValue2"));

        return Lists.newArrayList(testValue1);
    }

    private List<SearchField> createTextSearchFields() {
        TextSearchField testValue1 = new TextSearchField("text1", "text1", "desc");
        testValue1.setFieldValue("testValue1");

        TextSearchField testValue2 = new TextSearchField("text2", "text2", "desc");
        testValue2.setFieldValue("testValue2");

        return Lists.newArrayList(testValue1, testValue2);
    }

    private List<SearchField> createLongTextSearchFields() {
        TextSearchField testValue1 = new TextSearchField("text1", "text1", "desc");
        testValue1.setFieldValue("very long text one");

        TextSearchField testValue2 = new TextSearchField("text2", "text2", "desc");
        testValue2.setFieldValue("very long text two");

        return Lists.newArrayList(testValue1, testValue2);
    }
}