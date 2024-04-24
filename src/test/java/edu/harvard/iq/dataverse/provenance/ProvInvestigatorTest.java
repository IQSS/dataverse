/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.provenance;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

import edu.harvard.iq.dataverse.util.testing.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 *
 * @author madunlap
 */

//Fix this ignore when we classify our tests, these tests are slow and unimportant
public class ProvInvestigatorTest {
    
    private ProvInvestigator provUtilBean;
    JsonParser jsonParser;
    private static final Logger logger = Logger.getLogger(ProvInvestigatorTest.class.getCanonicalName());
    
    @BeforeEach
    public void setUp() {
        provUtilBean = ProvInvestigator.getInstance();
        jsonParser = new JsonParser();
    }
    
    @Tag(Tags.NOT_ESSENTIAL_UNITTESTS)
    @Test
    public void testProvValidator() {   
        String validJsonString = "{\n" +
            "    \"entity\": {\n" +
            "        \"ex:report2\": {\n" +
            "            \"prov:type\": \"report\",\n" +
            "            \"ex:version\": 2\n" +
            "        },\n" +
            "        \"ex:report1\": {\n" +
            "            \"prov:type\": \"report\",\n" +
            "            \"ex:version\": 1\n" +
            "        },\n" +
            "        \"bob:bundle1\": {\n" +
            "            \"prov:type\": {\n" +
            "                \"$\": \"prov:Bundle\",\n" +
            "                \"type\": \"xsd:QName\"\n" +
            "            }\n" +
            "        }\n" +
            "    },\n" +
            "    \"bundle\": {\n" +
            "        \"alice:bundle2\": {\n" +
            "            \"entity\": {\n" +
            "                \"ex:report2\": {\n" +
            "                    \"prov:type\": \"not report\",\n" +
            "                    \"ex:version\": 2\n" +
            "                },\n" +
            "                \"ex:report1\": {\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";
        
        assertTrue(provUtilBean.isProvValid(validJsonString)); 
        
        //This string has the "entity" section misnamed to "entitddd"
        String invalidJsonString = "{\n" +
            "    \"entitddd\": {\n" +
            "        \"ex:report2\": {\n" +
            "            \"prov:type\": \"report\",\n" +
            "            \"ex:version\": 2\n" +
            "        },\n" +
            "        \"ex:report1\": {\n" +
            "            \"prov:type\": \"report\",\n" +
            "            \"ex:version\": 1\n" +
            "        },\n" +
            "        \"bob:bundle1\": {\n" +
            "            \"prov:type\": {\n" +
            "                \"$\": \"prov:Bundle\",\n" +
            "                \"type\": \"xsd:QName\"\n" +
            "            }\n" +
            "        }\n" +
            "    },\n" +
            "    \"bundle\": {\n" +
            "        \"alice:bundle2\": {\n" +
            "            \"entity\": {\n" +
            "                \"ex:report2\": {\n" +
            "                    \"prov:type\": \"not report\",\n" +
            "                    \"ex:version\": 2\n" +
            "                },\n" +
            "                \"ex:report1\": {\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";
        
        assertFalse(provUtilBean.isProvValid(invalidJsonString)); 

    }
    
    @Tag(Tags.NOT_ESSENTIAL_UNITTESTS)
    @Test
    public void testProvNamesNotInsideEntity() throws IOException {
        //name and type on their own
        String jsonString = "{\"name\":\"testzame\",\"type\":\"ohno\"}";
        HashMap<String,ProvEntityFileData> entities = provUtilBean.startRecurseNames(jsonString);
        assertFalse(entities.size() > 0); 
        
        //name and type in an individual entity but not inside the "entity" grouping
        jsonString = "{\"p1\":{\"name\":\"testzame\",\"name2\":\"ohno\"}}";

        entities = provUtilBean.startRecurseNames(jsonString);
        assertFalse(entities.size() > 0); 
    }
    
    @Tag(Tags.NOT_ESSENTIAL_UNITTESTS)
    @Test
    public void testProvNameJsonParserEmptyEntities() throws IOException {
        String jsonString = "{\n" +
            "\n" +
            "\"prefix\" : {\n" +
            "\"prov\" : \"http://www.w3.org/ns/prov#\",\n" +
            "\"rdt\" : \"http://rdatatracker.org/\"\n" +
            "},\n" +
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
        
        HashMap<String,ProvEntityFileData> entities = provUtilBean.startRecurseNames(jsonString);
        assertTrue(entities.size() == 0);
    }
    
    //Note: this test has entity tags in multiple places, all with unique names
    //Only one entity is added to our list per unique name.
    
    @Tag(Tags.NOT_ESSENTIAL_UNITTESTS)
    @Test
    public void testProvJsonWithEntitiesInMultiplePlaces() throws IOException {
        String jsonString = "{\n" +
"    \"entity\": {\n" +
"        \"ex:report2\": {\n" +
"            \"prov:type\": \"report\",\n" +
"            \"ex:name\": \"best\"\n" +
"        },\n" +
"        \"ex:report1\": {\n" +
"            \"prov:type\": \"report\",\n" +
"            \"ex:version\": 1\n" +
"        },\n" +
"        \"alice:bundle2\": {\n" +
"            \"prov:type\": {\n" +
"                \"$\": \"prov:Bundle\",\n" +
"                \"type\": \"xsd:QName\"\n" +
"            }\n" +
"        },\n" +
"        \"bob:bundle1\": {\n" +
"            \"prov:type\": {\n" +
"                \"$\": \"prov:Bundle\",\n" +
"                \"type\": \"xsd:QName\"\n" +
"            }\n" +
"        }\n" +
"    },\n" +
"    \"bundle\": {\n" +
"        \"alice:bundle2\": {\n" +
"            \"wasGeneratedBy\": {\n" +
"                \"_:wGB29\": {\n" +
"                    \"prov:time\": \"2012-05-25T11:00:01\",\n" +
"                    \"prov:entity\": \"ex:report2\"\n" +
"                }\n" +
"            },\n" +
"            \"entity\": {\n" +
"                \"ex:report3\": {\n" +
"                    \"prov:type\": \"report\",\n" +
"                    \"ex:version\": 2\n" +
"                },\n" +
"                \"ex:report4\": {\n" +
"                }\n" +
"            },\n" +
"            \"wasDerivedFrom\": {\n" +
"                \"_:wDF25\": {\n" +
"                    \"prov:generatedEntity\": \"ex:report2\",\n" +
"                    \"prov:usedEntity\": \"ex:report1\"\n" +
"                }\n" +
"            }\n" +
"        },\n" +
"        \"bob:bundle1\": {\n" +
"            \"wasGeneratedBy\": {\n" +
"                \"_:wGB28\": {\n" +
"                    \"prov:time\": \"2012-05-24T10:00:01\",\n" +
"                    \"prov:entity\": \"ex:report1\"\n" +
"                }\n" +
"            },\n" +
"            \"entity\": {\n" +
"                \"ex:report5\": {\n" +
"                    \"prov:type\": \"report\",\n" +
"                    \"ex:version\": 1\n" +
"                }\n" +
"            }\n" +
"        }\n" +
"    }\n" +
"}";
        
        
        HashMap<String,ProvEntityFileData> entities = provUtilBean.startRecurseNames(jsonString);
        assertTrue(entities.get("ex:report5").fileType.equals("report"));
        assertTrue(entities.get("ex:report2").fileName.equals("best"));
        assertTrue(entities.size() == 7);
    }
    
    @Tag(Tags.NOT_ESSENTIAL_UNITTESTS)
    @Test
    public void testProvJsonWithEntitiesInMultiplePlacesWithSameNames() throws IOException {
        String jsonString = "{\n" +
"    \"entity\": {\n" +
"        \"ex:report2\": {\n" +
"            \"prov:type\": \"report\",\n" +
"            \"ex:version\": 2\n" +
"        },\n" +
"        \"ex:report1\": {\n" +
"            \"prov:type\": \"report\",\n" +
"            \"ex:version\": 1\n" +
"        },\n" +
"        \"bob:bundle1\": {\n" +
"            \"prov:type\": {\n" +
"                \"$\": \"prov:Bundle\",\n" +
"                \"type\": \"xsd:QName\"\n" +
"            }\n" +
"        }\n" +
"    },\n" +
"    \"bundle\": {\n" +
"        \"alice:bundle2\": {\n" +
"            \"entity\": {\n" +
"                \"ex:report2\": {\n" +
"                    \"prov:type\": \"not report\",\n" +
"                    \"ex:version\": 2\n" +
"                },\n" +
"                \"ex:report1\": {\n" +
"                }\n" +
"            }\n" +
"        }\n" +
"    }\n" +
"}";
        
        HashMap<String,ProvEntityFileData> entities = provUtilBean.startRecurseNames(jsonString);   
        assertTrue(entities.get("ex:report2").fileType.equals("not report"));
        assertTrue(entities.size() == 3); //ex:report2 & ex:report1 are repeated
    }
    
    @Tag(Tags.NOT_ESSENTIAL_UNITTESTS)
    @Test
    public void testProvLongJsonWithEntities() throws IOException {
        String jsonString = "{\n" +
            "   \"prefix\":{\n" +
            "      \"prov\":\"http://www.w3.org/ns/prov#\",\n" +
            "      \"rdt\":\"http://rdatatracker.org/\"\n" +
            "   },\n" +
            "   \"activity\":{\n" +
            "      \"p1\":{\n" +
            "         \"rdt:name\":\"Test.R\",\n" +
            "         \"rdt:type\":\"Start\",\n" +
            "         \"rdt:elapsedTime\":\"1.21\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"NA\"\n" +
            "      },\n" +
            "      \"p2\":{\n" +
            "         \"rdt:name\":\"fn1 <- function() {    fn <- function() {        a <- 1    }\",\n" +
            "         \"rdt:type\":\"Operation\",\n" +
            "         \"rdt:elapsedTime\":\"1.22\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"1\"\n" +
            "      },\n" +
            "      \"p3\":{\n" +
            "         \"rdt:name\":\"fn2 <- function() {    fn <- function() {        b <- 2    }\",\n" +
            "         \"rdt:type\":\"Operation\",\n" +
            "         \"rdt:elapsedTime\":\"1.22\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"9\"\n" +
            "      },\n" +
            "      \"p4\":{\n" +
            "         \"rdt:name\":\"fn1()\",\n" +
            "         \"rdt:type\":\"Start\",\n" +
            "         \"rdt:elapsedTime\":\"1.22\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"17\"\n" +
            "      },\n" +
            "      \"p5\":{\n" +
            "         \"rdt:name\":\"fn1\",\n" +
            "         \"rdt:type\":\"Operation\",\n" +
            "         \"rdt:elapsedTime\":\"1.22\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"17\"\n" +
            "      },\n" +
            "      \"p6\":{\n" +
            "         \"rdt:name\":\"fn <- function() {    a <- 1}\",\n" +
            "         \"rdt:type\":\"Operation\",\n" +
            "         \"rdt:elapsedTime\":\"1.24\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"17\"\n" +
            "      },\n" +
            "      \"p7\":{\n" +
            "         \"rdt:name\":\"return (fn <- function() {    a <- 1})\",\n" +
            "         \"rdt:type\":\"Operation\",\n" +
            "         \"rdt:elapsedTime\":\"1.24\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"17\"\n" +
            "      },\n" +
            "      \"p8\":{\n" +
            "         \"rdt:name\":\"fn1()\",\n" +
            "         \"rdt:type\":\"Finish\",\n" +
            "         \"rdt:elapsedTime\":\"1.25\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"17\"\n" +
            "      },\n" +
            "      \"p9\":{\n" +
            "         \"rdt:name\":\"fn2()\",\n" +
            "         \"rdt:type\":\"Start\",\n" +
            "         \"rdt:elapsedTime\":\"1.25\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"18\"\n" +
            "      },\n" +
            "      \"p10\":{\n" +
            "         \"rdt:name\":\"fn2\",\n" +
            "         \"rdt:type\":\"Operation\",\n" +
            "         \"rdt:elapsedTime\":\"1.25\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"18\"\n" +
            "      },\n" +
            "      \"p11\":{\n" +
            "         \"rdt:name\":\"fn <- function() {    b <- 2}\",\n" +
            "         \"rdt:type\":\"Operation\",\n" +
            "         \"rdt:elapsedTime\":\"1.25\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"18\"\n" +
            "      },\n" +
            "      \"p12\":{\n" +
            "         \"rdt:name\":\"return (fn <- function() {    b <- 2})\",\n" +
            "         \"rdt:type\":\"Operation\",\n" +
            "         \"rdt:elapsedTime\":\"1.25\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"18\"\n" +
            "      },\n" +
            "      \"p13\":{\n" +
            "         \"rdt:name\":\"fn2()\",\n" +
            "         \"rdt:type\":\"Finish\",\n" +
            "         \"rdt:elapsedTime\":\"1.27\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"18\"\n" +
            "      },\n" +
            "      \"p14\":{\n" +
            "         \"rdt:name\":\"Test.R\",\n" +
            "         \"rdt:type\":\"Finish\",\n" +
            "         \"rdt:elapsedTime\":\"1.27\",\n" +
            "         \"rdt:scriptNum\":\"0\",\n" +
            "         \"rdt:scriptLine\":\"NA\"\n" +
            "      },\n" +
            "      \"environment\":{\n" +
            "         \"rdt:name\":\"environment\",\n" +
            "         \"rdt:architecture\":\"x86_64\",\n" +
            "         \"rdt:operatingSystem\":\"windows\",\n" +
            "         \"rdt:language\":\"R\",\n" +
            "         \"rdt:rVersion\":\"R version 3.3.1 (2016-06-21)\",\n" +
            "         \"rdt:script\":\"C:/Users/fong22e/Documents/HarvardForest/Test/Test.R\",\n" +
            "         \"rdt:sourcedScripts\":\"\",\n" +
            "         \"rdt:scriptTimeStamp\":\"2016-09-28T04.31.54EDT\",\n" +
            "         \"rdt:workingDirectory\":\"C:/Users/fong22e/Documents/HarvardForest/Test\",\n" +
            "         \"rdt:ddgDirectory\":\"./Test_ddg\",\n" +
            "         \"rdt:ddgTimeStamp\":\"2016-09-28T04.31.58EDT\",\n" +
            "         \"rdt:rdatatrackerVersion\":\"2.24.1\",\n" +
            "         \"rdt:InstalledPackages\":[\n" +
            "            {\n" +
            "               \"package\":\"RDataTracker\",\n" +
            "               \"version\":\"2.24.1\"\n" +
            "            }\n" +
            "         ]\n" +
            "      }\n" +
            "   },\n" +
            "   \"entity\":{\n" +
            "      \"d1\":{\n" +
            "         \"rdt:name\":\"fn1\",\n" +
            "         \"rdt:value\":\"#ddg.function\",\n" +
            "         \"rdt:type\":\"Data\",\n" +
            "         \"rdt:scope\":\"R_GlobalEnv\",\n" +
            "         \"rdt:fromEnv\":\"FALSE\",\n" +
            "         \"rdt:timestamp\":\"\",\n" +
            "         \"rdt:location\":\"\"\n" +
            "      },\n" +
            "      \"d2\":{\n" +
            "         \"rdt:name\":\"fn2\",\n" +
            "         \"rdt:value\":\"#ddg.function\",\n" +
            "         \"rdt:type\":\"Data\",\n" +
            "         \"rdt:scope\":\"R_GlobalEnv\",\n" +
            "         \"rdt:fromEnv\":\"FALSE\",\n" +
            "         \"rdt:timestamp\":\"\",\n" +
            "         \"rdt:location\":\"\"\n" +
            "      },\n" +
            "      \"d3\":{\n" +
            "         \"rdt:name\":\"fn\",\n" +
            "         \"rdt:value\":\"#ddg.function\",\n" +
            "         \"rdt:type\":\"Data\",\n" +
            "         \"rdt:scope\":\"0x00000000055e57d0\",\n" +
            "         \"rdt:fromEnv\":\"FALSE\",\n" +
            "         \"rdt:timestamp\":\"\",\n" +
            "         \"rdt:location\":\"\"\n" +
            "      },\n" +
            "      \"d4\":{\n" +
            "         \"rdt:name\":\"fn1() return\",\n" +
            "         \"rdt:value\":\"#ddg.function\",\n" +
            "         \"rdt:type\":\"Data\",\n" +
            "         \"rdt:scope\":\"R_GlobalEnv\",\n" +
            "         \"rdt:fromEnv\":\"FALSE\",\n" +
            "         \"rdt:timestamp\":\"\",\n" +
            "         \"rdt:location\":\"\"\n" +
            "      },\n" +
            "      \"d5\":{\n" +
            "         \"rdt:name\":\"fn\",\n" +
            "         \"rdt:value\":\"#ddg.function\",\n" +
            "         \"rdt:type\":\"Data\",\n" +
            "         \"rdt:scope\":\"0x00000000054e1eb0\",\n" +
            "         \"rdt:fromEnv\":\"FALSE\",\n" +
            "         \"rdt:timestamp\":\"\",\n" +
            "         \"rdt:location\":\"\"\n" +
            "      },\n" +
            "      \"d6\":{\n" +
            "         \"rdt:name\":\"fn2() return\",\n" +
            "         \"rdt:value\":\"#ddg.function\",\n" +
            "         \"rdt:type\":\"Data\",\n" +
            "         \"rdt:scope\":\"R_GlobalEnv\",\n" +
            "         \"rdt:fromEnv\":\"FALSE\",\n" +
            "         \"rdt:timestamp\":\"\",\n" +
            "         \"rdt:location\":\"\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"wasInformedBy\":{\n" +
            "      \"e1\":{\n" +
            "         \"prov:informant\":\"p1\",\n" +
            "         \"prov:informed\":\"p2\"\n" +
            "      },\n" +
            "      \"e3\":{\n" +
            "         \"prov:informant\":\"p2\",\n" +
            "         \"prov:informed\":\"p3\"\n" +
            "      },\n" +
            "      \"e5\":{\n" +
            "         \"prov:informant\":\"p3\",\n" +
            "         \"prov:informed\":\"p4\"\n" +
            "      },\n" +
            "      \"e7\":{\n" +
            "         \"prov:informant\":\"p4\",\n" +
            "         \"prov:informed\":\"p5\"\n" +
            "      },\n" +
            "      \"e8\":{\n" +
            "         \"prov:informant\":\"p5\",\n" +
            "         \"prov:informed\":\"p6\"\n" +
            "      },\n" +
            "      \"e10\":{\n" +
            "         \"prov:informant\":\"p6\",\n" +
            "         \"prov:informed\":\"p7\"\n" +
            "      },\n" +
            "      \"e12\":{\n" +
            "         \"prov:informant\":\"p7\",\n" +
            "         \"prov:informed\":\"p8\"\n" +
            "      },\n" +
            "      \"e13\":{\n" +
            "         \"prov:informant\":\"p8\",\n" +
            "         \"prov:informed\":\"p9\"\n" +
            "      },\n" +
            "      \"e15\":{\n" +
            "         \"prov:informant\":\"p9\",\n" +
            "         \"prov:informed\":\"p10\"\n" +
            "      },\n" +
            "      \"e16\":{\n" +
            "         \"prov:informant\":\"p10\",\n" +
            "         \"prov:informed\":\"p11\"\n" +
            "      },\n" +
            "      \"e18\":{\n" +
            "         \"prov:informant\":\"p11\",\n" +
            "         \"prov:informed\":\"p12\"\n" +
            "      },\n" +
            "      \"e20\":{\n" +
            "         \"prov:informant\":\"p12\",\n" +
            "         \"prov:informed\":\"p13\"\n" +
            "      },\n" +
            "      \"e21\":{\n" +
            "         \"prov:informant\":\"p13\",\n" +
            "         \"prov:informed\":\"p14\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"wasGeneratedBy\":{\n" +
            "      \"e2\":{\n" +
            "         \"prov:entity\":\"d1\",\n" +
            "         \"prov:activity\":\"p2\"\n" +
            "      },\n" +
            "      \"e4\":{\n" +
            "         \"prov:entity\":\"d2\",\n" +
            "         \"prov:activity\":\"p3\"\n" +
            "      },\n" +
            "      \"e9\":{\n" +
            "         \"prov:entity\":\"d3\",\n" +
            "         \"prov:activity\":\"p6\"\n" +
            "      },\n" +
            "      \"e11\":{\n" +
            "         \"prov:entity\":\"d4\",\n" +
            "         \"prov:activity\":\"p7\"\n" +
            "      },\n" +
            "      \"e17\":{\n" +
            "         \"prov:entity\":\"d5\",\n" +
            "         \"prov:activity\":\"p11\"\n" +
            "      },\n" +
            "      \"e19\":{\n" +
            "         \"prov:entity\":\"d6\",\n" +
            "         \"prov:activity\":\"p12\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"used\":{\n" +
            "      \"e6\":{\n" +
            "         \"prov:activity\":\"p5\",\n" +
            "         \"prov:entity\":\"d1\"\n" +
            "      },\n" +
            "      \"e14\":{\n" +
            "         \"prov:activity\":\"p10\",\n" +
            "         \"prov:entity\":\"d2\"\n" +
            "      }\n" +
            "   }\n" +
            "}";
        
        
        HashMap<String,ProvEntityFileData> entities = provUtilBean.startRecurseNames(jsonString);
        assertTrue(entities.size() == 6);
    }
}
