package edu.harvard.iq.dataverse.datavariable;

import org.junit.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import static org.junit.Assert.*;

public class VariableMetadataDDIParserTest {

    @Test
    /**
     * Test XML ddi parser
     */
    public void testDDIReader()  {

        String fileName = "src/test/resources/xml/DataCuration.xml";
        XMLStreamReader xmlr = null;

        XMLInputFactory factory=XMLInputFactory.newInstance();
        try {
            xmlr = factory.createXMLStreamReader(new FileInputStream(fileName));
        } catch (Exception e) {
            xmlr = null;
            assertNotNull(xmlr);
        }
        VariableMetadataDDIParser dti = new VariableMetadataDDIParser();

        Map<Long,VariableMetadata> mapVarToVarMet = new HashMap<Long, VariableMetadata>();
        Map<Long,VarGroup> varGroupMap = new HashMap<Long, VarGroup>();

        try {

            dti.processDataDscr(xmlr,  mapVarToVarMet, varGroupMap);

        } catch ( XMLStreamException e) {
            assertNotNull(null);
        }

        assertEquals(mapVarToVarMet.size(),3);
        variableTest(mapVarToVarMet);

        assertEquals(varGroupMap.size(),2);
        groupTest(varGroupMap);


        return;

    }

    void variableTest(Map<Long,VariableMetadata> vmMap) {

        VariableMetadata vm =  vmMap.get(619L);
        assertNotNull(vm);

        assertEquals(vm.getLabel(),"gender");
        assertEquals(vm.getInterviewinstruction(),"int");
        assertEquals(vm.getLiteralquestion(), "lit q");
        assertEquals(vm.getNotes(), "note");
        assertEquals(vm.getUniverse(),"univrse");
        assertEquals(false, vm.isIsweightvar());
        assertEquals(false, vm.isWeighted());

        vm =  vmMap.get(618L);
        assertNotNull(vm);
        assertEquals(false, vm.isIsweightvar());
        assertEquals(false, vm.isWeighted());
        assertEquals(vm.getLabel(), "age_rollup"  );

        assertEquals(vm.getInterviewinstruction(), null);
        assertEquals(vm.getLiteralquestion(), null);
        assertEquals(vm.getNotes(), null);
        assertEquals(vm.getUniverse(), null);

        vm =  vmMap.get(620L);
        assertNotNull(vm);


    }

    void groupTest(Map<Long,VarGroup> varGroupMap) {

        VarGroup vg1 =  varGroupMap.get(1L);
        assertNotNull(vg1);

        //first group
        Set<DataVariable> dvSet1 = new HashSet<DataVariable>();
        DataVariable dv = new DataVariable();
        dv.setId(619L);
        dvSet1.add(dv);
        dv.setId(620L);
        dvSet1.add(dv);
        eachGroupTest(vg1,"New Group 1",dvSet1);

        //second group
        VarGroup vg2 =  varGroupMap.get(2L);
        assertNotNull(vg2);
        Set<DataVariable> dvSet2 = new HashSet<DataVariable>();
        dv.setId(618L);
        dvSet2.add(dv);
        eachGroupTest(vg2,"New Group 2",dvSet2);

    }

    void eachGroupTest( VarGroup vg, String label, Set<DataVariable> dvSet) {
        assertEquals(vg.getLabel(), label);
        Set<DataVariable> varsInGroups = vg.getVarsInGroup();
        assertNotNull(varsInGroups);
        assertEquals(varsInGroups.size(),dvSet.size());
        assertEquals(varsInGroups, dvSet);
    }
}
