package edu.harvard.iq.dataverse.search;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Lists;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.search.advanced.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.DateSearchField;
import edu.harvard.iq.dataverse.search.advanced.NumberSearchField;
import edu.harvard.iq.dataverse.search.advanced.SearchBlock;
import edu.harvard.iq.dataverse.search.advanced.SearchField;
import edu.harvard.iq.dataverse.search.advanced.SelectOneSearchField;
import edu.harvard.iq.dataverse.search.advanced.SolrQueryCreator;
import edu.harvard.iq.dataverse.search.advanced.TextSearchField;

@ExtendWith(MockitoExtension.class)
class SolrQueryCreatorTest {

    @Mock
    LicenseRepository licenseRepository = mock(LicenseRepository.class);
    
    @InjectMocks
    SolrQueryCreator solrQueryCreator = new SolrQueryCreator();

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
    public void constructQuery_BothDatesQuery() {
        //given
        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", createBothDatesSearchFields());
        //when
        String result = solrQueryCreator.constructQuery(Lists.newArrayList((searchBlock)));
        //then
        Assert.assertEquals("date1:[dateValue1 TO dateValue2]", result);
    }

    @Test
    public void constructQuery_OneDateQuery() {
        //given
        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", createOneDateSearchFields());
        //when
        String result = solrQueryCreator.constructQuery(Lists.newArrayList((searchBlock)));
        //then
        Assert.assertEquals("date1:[dateValue1 TO *] AND date2:[* TO dateValue2]", result);
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
    public void constructQuery_SelectOneQuery() {
        //given
        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", createSelectOneSearchFields());
        //when
        String result = solrQueryCreator.constructQuery(Lists.newArrayList((searchBlock)));
        //then
        Assert.assertEquals("selectOneValue1:\"checkedFieldValue1\" AND selectOneValue2:\"checkedFieldValue2\"", result);
    }

    @Test
    public void constructQuery_CombinedQuery() {
        //given
        List<SearchField> searchFields = new ArrayList<>();
        searchFields.addAll(createBothNumbersSearchFields());
        searchFields.addAll(createOneNumberSearchFields());
        searchFields.addAll(createCheckboxSearchFields());
        searchFields.addAll(createTextSearchFields());
        searchFields.addAll(createDateSearchFields());
        searchFields.addAll(createSelectOneSearchFields());

        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", searchFields);
        //when
        String result = solrQueryCreator.constructQuery(Lists.newArrayList((searchBlock)));
        //then
        Assert.assertEquals("number1:[1 TO 2] AND number2:[3.1 TO 4.1] AND number1:[1 TO *] AND" +
                                    " number2:[* TO 4.1] AND checkboxValues:\"checkboxValue1\" AND checkboxValues:\"checkboxValue2\" AND" +
                                    " text1:testValue1 AND text2:testValue2 AND date1:[testDate1 TO *] AND date2:[* TO testDate2] AND " +
                                    "date3:[testDate31 TO testDate32] AND selectOneValue1:\"checkedFieldValue1\" AND selectOneValue2:\"checkedFieldValue2\"", result);
    }

    @Test
    public void constructQuery_LicenseQuery() {
        
        //given
        License license1 = new License();
        license1.setName("License 1");
        License license2 = new License();
        license2.setName("License 2");
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                if (invocation.getArgument(0).equals(1L)) {
                    return license1;
                } else if (invocation.getArgument(0).equals(2L)) {
                    return license2;
                } else {
                    return null;
                }
            }
        }).when(licenseRepository).getById(anyLong());        
        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", createCheckboxLicenseSearchFields());
        //when
        String result = solrQueryCreator.constructQuery(Lists.newArrayList((searchBlock)));
        //then
        Assert.assertEquals("(license:\"License 1\" OR license:\"License 2\") AND dvObjectType:\"files\"", result);
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
    
    private List<SearchField> createBothDatesSearchFields() {
        DateSearchField testValue1 = new DateSearchField("date1", "date1", "desc");
        testValue1.setLowerLimit("dateValue1");
        testValue1.setUpperLimit("dateValue2");

        return Lists.newArrayList(testValue1);
    }

    private List<SearchField> createOneDateSearchFields() {
        DateSearchField testValue1 = new DateSearchField("date1", "date1", "desc");
        testValue1.setLowerLimit("dateValue1");

        DateSearchField testValue2 = new DateSearchField("date2", "date2", "desc");
        testValue2.setUpperLimit("dateValue2");

        return Lists.newArrayList(testValue1, testValue2);
    }

    private List<SearchField> createCheckboxSearchFields() {
        CheckboxSearchField testValue1 = new CheckboxSearchField("checkboxValues", "checkboxValues", "desc");
        testValue1.getCheckedFieldValues().addAll(Lists.newArrayList("checkboxValue1", "checkboxValue2"));

        return Lists.newArrayList(testValue1);
    }

    private List<SearchField> createCheckboxLicenseSearchFields() {
        CheckboxSearchField testValue1 = new CheckboxSearchField("license", "checkboxLicenseValues", "desc");
        testValue1.getCheckedFieldValues().addAll(Lists.newArrayList("license:1", "license:2", "license:3"));

        return Lists.newArrayList(testValue1);
    }

    private List<SearchField> createSelectOneSearchFields() {
        SelectOneSearchField testValue1 = new SelectOneSearchField("selectOneValue1", "selectOneDisplayValue1", "desc1");
        testValue1.setCheckedFieldValue("checkedFieldValue1");

        SelectOneSearchField testValue2 = new SelectOneSearchField("selectOneValue2", "selectOneDisplayValue2", "desc2");
        testValue2.setCheckedFieldValue("checkedFieldValue2");

        return Lists.newArrayList(testValue1, testValue2);
    }

    private List<SearchField> createTextSearchFields() {
        TextSearchField testValue1 = new TextSearchField("text1", "text1", "desc");
        testValue1.setFieldValue("testValue1");

        TextSearchField testValue2 = new TextSearchField("text2", "text2", "desc");
        testValue2.setFieldValue("testValue2");

        return Lists.newArrayList(testValue1, testValue2);
    }

    private List<SearchField> createDateSearchFields() {
        DateSearchField testValue1 = new DateSearchField("date1", "date1", "desc");
        testValue1.setLowerLimit("testDate1");

        DateSearchField testValue2 = new DateSearchField("date2", "date2", "desc");
        testValue2.setUpperLimit("testDate2");

        DateSearchField testValue3 = new DateSearchField("date3", "date3", "desc");
        testValue3.setLowerLimit("testDate31");
        testValue3.setUpperLimit("testDate32");

        return Lists.newArrayList(testValue1, testValue2, testValue3);
    }

    private List<SearchField> createLongTextSearchFields() {
        TextSearchField testValue1 = new TextSearchField("text1", "text1", "desc");
        testValue1.setFieldValue("very long text one");

        TextSearchField testValue2 = new TextSearchField("text2", "text2", "desc");
        testValue2.setFieldValue("very long text two");

        return Lists.newArrayList(testValue1, testValue2);
    }
}