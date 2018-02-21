/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author madunlap
 */
public class ProvUploadFragmentTest {
    
    private ProvenanceUploadFragmentBean provBean;
    private static final Logger logger = Logger.getLogger(ProvenanceUploadFragmentBean.class.getCanonicalName());
    
    @Before
    public void setUp() {
        provBean = new ProvenanceUploadFragmentBean();
    }
    
    @Test
    public void testProvNameJsonParser() throws IOException {
        String jsonString = "{\"name\":\"testzame\",\"name2\":\"ohnobutt\"}";
        
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(jsonString).getAsJsonObject();
        
        provBean.recurseNames(jsonObject);
        ArrayList<String> theNames = provBean.getProvJsonParsedNames();
        logger.warning("Names found: " + theNames);
        assertTrue(theNames.size() > 0);
    }
    
    @Test
    public void testProvNameJsonParser2() throws IOException {
        String jsonString = "{\n" +
            "\n" +
            "\"prefix\" : {\n" +
            "\"prov\" : \"http://www.w3.org/ns/prov#\",\n" +
            "\"rdt\" : \"http://rdatatracker.org/\"\n" +
            "},\n" +
            "\"activity\":{\n" +
            "\n" +
            "\"p1\" : {\n" +
            "\"rdt:name\" : \"test.R\",\n" +
            "\"rdt:type\" : \"Start\",\n" +
            "\"rdt:elapsedTime\" : \"8.87\",\n" +
            "\"rdt:scriptNum\" : \"NA\",\n" +
            "\"rdt:startLine\" : \"NA\",\n" +
            "\"rdt:startCol\" : \"NA\",\n" +
            "\"rdt:endLine\" : \"NA\",\n" +
            "\"rdt:endCol\" : \"NA\"\n" +
            "} ,\n" +
            "\n" +
            "\"p2\" : {\n" +
            "\"rdt:name\" : \"library(CamFlow)\",\n" +
            "\"rdt:type\" : \"Operation\",\n" +
            "\"rdt:elapsedTime\" : \"8.87400000000001\",\n" +
            "\"rdt:scriptNum\" : \"0\",\n" +
            "\"rdt:startLine\" : \"2\",\n" +
            "\"rdt:startCol\" : \"1\",\n" +
            "\"rdt:endLine\" : \"2\",\n" +
            "\"rdt:endCol\" : \"16\"\n" +
            "} ,\n" +
            "\n" +
            "\"p3\" : {\n" +
            "\"rdt:name\" : \"CamFlowVisualiser(\\\"~/Projects/HF/projects/Dprov/projects/dev\",\n" +
            "\"rdt:type\" : \"Operation\",\n" +
            "\"rdt:elapsedTime\" : \"8.88300000000001\",\n" +
            "\"rdt:scriptNum\" : \"0\",\n" +
            "\"rdt:startLine\" : \"4\",\n" +
            "\"rdt:startCol\" : \"1\",\n" +
            "\"rdt:endLine\" : \"4\",\n" +
            "\"rdt:endCol\" : \"97\"\n" +
            "} ,\n" +
            "\n" +
            "\"p4\" : {\n" +
            "\"rdt:name\" : \"test.R\",\n" +
            "\"rdt:type\" : \"Finish\",\n" +
            "\"rdt:elapsedTime\" : \"8.88800000000001\",\n" +
            "\"rdt:scriptNum\" : \"NA\",\n" +
            "\"rdt:startLine\" : \"NA\",\n" +
            "\"rdt:startCol\" : \"NA\",\n" +
            "\"rdt:endLine\" : \"NA\",\n" +
            "\"rdt:endCol\" : \"NA\"\n" +
            "} ,\n" +
            "\n" +
            "\"environment\" : {\n" +
            "\"rdt:name\" : \"environment\",\n" +
            "\"rdt:architecture\" : \"x86_64\",\n" +
            "\"rdt:operatingSystem\" : \"unix\",\n" +
            "\"rdt:language\" : \"R\",\n" +
            "\"rdt:rVersion\" : \"R version 3.3.1 (2016-06-21)\",\n" +
            "\"rdt:script\" : \"/Users/hermes/Desktop/test.R\",\n" +
            "\"rdt:sourcedScripts\" : \"\"\n" +
            ",\n" +
            "\"rdt:scriptTimeStamp\" : \"2016-10-07T17.19.21EDT\",\n" +
            "\"rdt:workingDirectory\" : \"/Users/hermes/Desktop\",\n" +
            "\"rdt:ddgDirectory\" : \"./test_ddg\",\n" +
            "\"rdt:ddgTimeStamp\" : \"2016-10-07T17.19.50EDT\",\n" +
            "\"rdt:rdatatrackerVersion\" : \"2.24.1\",\n" +
            "\"rdt:InstalledPackages\": [\n" +
            "    {\"package\":\"akima\", \"version\":\"0.5-12\"},\n" +
            "    {\"package\":\"CamFlow\", \"version\":\"0.0.0.9000\"},\n" +
            "    {\"package\":\"cowplot\", \"version\":\"0.6.2\"},\n" +
            "    {\"package\":\"ggplot2\", \"version\":\"2.1.0\"},\n" +
            "    {\"package\":\"lattice\", \"version\":\"0.20-33\"},\n" +
            "    {\"package\":\"MASS\", \"version\":\"7.3-45\"},\n" +
            "    {\"package\":\"permute\", \"version\":\"0.9-0\"},\n" +
            "    {\"package\":\"plyr\", \"version\":\"1.8.4\"},\n" +
            "    {\"package\":\"png\", \"version\":\"0.1-7\"},\n" +
            "    {\"package\":\"RColorBrewer\", \"version\":\"1.1-2\"},\n" +
            "    {\"package\":\"RDataTracker\", \"version\":\"2.24.1\"},\n" +
            "    {\"package\":\"reshape\", \"version\":\"0.8.5\"},\n" +
            "    {\"package\":\"scatterplot3d\", \"version\":\"0.3-37\"},\n" +
            "    {\"package\":\"vegan\", \"version\":\"2.4-0\"}]\n" +
            "}},\n" +
            "\"entity\":{\n" +
            "},\n" +
            "\"wasInformedBy\":{\n" +
            "\n" +
            "\"e1\" : {\n" +
            "\"prov:informant\" : \"p1\",\n" +
            "\"prov:informed\" : \"p2\"\n" +
            "} ,\n" +
            "\n" +
            "\"e2\" : {\n" +
            "\"prov:informant\" : \"p2\",\n" +
            "\"prov:informed\" : \"p3\"\n" +
            "} ,\n" +
            "\n" +
            "\"e3\" : {\n" +
            "\"prov:informant\" : \"p3\",\n" +
            "\"prov:informed\" : \"p4\"\n" +
            "}},\n" +
            "\"wasGeneratedBy\":{\n" +
            "},\n" +
            "\"used\":{\n" +
            "}\n" +
            "}";
        
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(jsonString).getAsJsonObject();
        
        provBean.recurseNames(jsonObject);
        ArrayList<String> theNames = provBean.getProvJsonParsedNames();
        logger.warning("Names found: " + theNames);
        assertTrue(theNames.size() > 0);
    }
    
}
