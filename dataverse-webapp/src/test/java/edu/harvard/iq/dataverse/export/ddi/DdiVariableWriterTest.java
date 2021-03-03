package edu.harvard.iq.dataverse.export.ddi;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable.VariableInterval;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable.VariableType;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.SummaryStatistic;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.SummaryStatistic.SummaryStatisticType;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.VariableRange.VariableRangeType;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.VariableRange;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

public class DdiVariableWriterTest {

    private DdiVariableWriter ddiDataVariableWriter = new DdiVariableWriter();
    
    private StringWriter writer;
    private XMLStreamWriter xmlw;


    @BeforeEach
    public void before() throws XMLStreamException {
        writer = new StringWriter();
        xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
    }

    @AfterEach
    public void after() throws IOException, XMLStreamException {
        xmlw.close();
        writer.close();
    }

    // -------------------- TESTS --------------------

    @Test
    void createVarDDI_continuous_numeric() throws IOException, URISyntaxException, XMLStreamException {

        // given
        DataVariable dataVariable = createContinuousNumericVariable();
        
        DataTable dataTable = new DataTable();
        dataVariable.setDataTable(dataTable);
        
        DataFile dataFile = new DataFile();
        dataFile.setId(11L);
        dataTable.setDataFile(dataFile);
        

        // when
        ddiDataVariableWriter.createVarDDI(xmlw, dataVariable, new FileMetadata());
        xmlw.flush();

        // then
        assertThat(XmlPrinter.prettyPrintXml(writer.toString()))
            .isEqualTo(XmlPrinter.prettyPrintXml(UnitTestUtils.readFileToString("xml/export/ddi/variable-continuous-numeric.xml")));
    }

    @Test
    void createVarDDI_category_group() throws IOException, URISyntaxException, XMLStreamException {

        // given
        DataVariable dataVariable = createCategoryGroupVariable();
        
        DataTable dataTable = new DataTable();
        dataVariable.setDataTable(dataTable);
        
        DataFile dataFile = new DataFile();
        dataFile.setId(12L);
        dataTable.setDataFile(dataFile);
        

        // when
        ddiDataVariableWriter.createVarDDI(xmlw, dataVariable, new FileMetadata());
        xmlw.flush();

        // then
        assertThat(XmlPrinter.prettyPrintXml(writer.toString()))
            .isEqualTo(XmlPrinter.prettyPrintXml(UnitTestUtils.readFileToString("xml/export/ddi/variable-category-group.xml")));
    }
    
    @Test
    void createVarDDI_with_invalid_range() throws XMLStreamException, IOException {

        // given
        DataVariable dataVariable = createVariableWithInvalidRange();
        
        DataTable dataTable = new DataTable();
        dataVariable.setDataTable(dataTable);
        
        DataFile dataFile = new DataFile();
        dataFile.setId(13L);
        dataTable.setDataFile(dataFile);
        

        // when
        ddiDataVariableWriter.createVarDDI(xmlw, dataVariable, new FileMetadata());
        xmlw.flush();

        // then
        assertThat(XmlPrinter.prettyPrintXml(writer.toString()))
            .isEqualTo(XmlPrinter.prettyPrintXml(UnitTestUtils.readFileToString("xml/export/ddi/variable-with-invalid-range.xml")));
    }
    
    
    // -------------------- PRIVATE --------------------

    private DataVariable createContinuousNumericVariable() {
        DataVariable dataVariable = new DataVariable();
        dataVariable.setFileOrder(0);
        dataVariable.setId(12L);
        dataVariable.setName("varName");
        dataVariable.setLabel("varLabel");
        dataVariable.setType(VariableType.NUMERIC);
        dataVariable.setInterval(VariableInterval.CONTINUOUS);
        dataVariable.setInvalidRanges(Lists.newArrayList());
        dataVariable.setCategories(Lists.newArrayList());
        SummaryStatistic variableMinStatistic = new SummaryStatistic();
        variableMinStatistic.setType(SummaryStatisticType.MIN);
        variableMinStatistic.setValue("1");
        SummaryStatistic variableMaxStatistic = new SummaryStatistic();
        variableMaxStatistic.setType(SummaryStatisticType.MAX);
        variableMaxStatistic.setValue("10");
        dataVariable.setUnf("UNF:number:000");
        dataVariable.setSummaryStatistics(Lists.newArrayList(variableMinStatistic, variableMaxStatistic));
        
        return dataVariable;
    }
    
    private DataVariable createCategoryGroupVariable() {
        DataVariable dataVariable = new DataVariable();
        dataVariable.setFileOrder(0);
        dataVariable.setId(13L);
        dataVariable.setName("yes_no");
        dataVariable.setLabel("Is yes or no?");
        dataVariable.setType(VariableType.NUMERIC);
        dataVariable.setInterval(VariableInterval.DISCRETE);
        dataVariable.setInvalidRanges(Lists.newArrayList());
        SummaryStatistic variableMinStatistic = new SummaryStatistic();
        variableMinStatistic.setType(SummaryStatisticType.MIN);
        variableMinStatistic.setValue("1");
        SummaryStatistic variableMaxStatistic = new SummaryStatistic();
        variableMaxStatistic.setType(SummaryStatisticType.MAX);
        variableMaxStatistic.setValue("2");
        dataVariable.setSummaryStatistics(Lists.newArrayList(variableMinStatistic, variableMaxStatistic));
        
        VariableCategory varCategory1 = new VariableCategory();
        varCategory1.setDataVariable(dataVariable);
        varCategory1.setId(101L);
        varCategory1.setLabel("No");
        varCategory1.setFrequency(39.99);
        varCategory1.setValue("1");
        
        VariableCategory varCategory2 = new VariableCategory();
        varCategory2.setDataVariable(dataVariable);
        varCategory2.setId(102L);
        varCategory2.setLabel("Yes");
        varCategory2.setFrequency(68.0);
        varCategory2.setValue("2");
        dataVariable.setCategories(Lists.newArrayList(varCategory1, varCategory2));
        
        return dataVariable;
    }

    private DataVariable createVariableWithInvalidRange() {
        DataVariable dataVariable = new DataVariable();
        dataVariable.setFileOrder(0);
        dataVariable.setId(13L);
        dataVariable.setName("invalid_range_var");
        dataVariable.setLabel("Invalid range variable");
        dataVariable.setType(VariableType.NUMERIC);
        dataVariable.setInterval(VariableInterval.CONTINUOUS);
        dataVariable.setSummaryStatistics(Lists.newArrayList());
        dataVariable.setCategories(Lists.newArrayList());
        VariableRange variableRange = new VariableRange();
        variableRange.setBeginValueType(VariableRangeType.MIN_EXCLUSIVE);
        variableRange.setBeginValue("2");
        variableRange.setEndValueType(VariableRangeType.MAX);
        variableRange.setEndValue("5");
        
        dataVariable.setInvalidRanges(Lists.newArrayList(variableRange));
        return dataVariable;
    }
}
