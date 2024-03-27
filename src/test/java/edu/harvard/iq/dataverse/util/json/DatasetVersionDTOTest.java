/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author ellenk
 */
public class DatasetVersionDTOTest {
    private final DateFormat dateFormat = Util.getDateTimeFormat();
    
    public DatasetVersionDTOTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
       
    }
    @AfterEach
    public void tearDown() {
    }
    
    

    @Test
    public void testReadDataSet() {
        try {
          
            File file = new File("src/test/java/edu/harvard/iq/dataverse/util/json/JsonDatasetVersion.txt");
            String text = new Scanner(file).useDelimiter("\\Z").next();
            Gson gson = new Gson();
            DatasetVersionDTO dto = gson.fromJson(text, DatasetVersionDTO.class);

            HashSet<FieldDTO> author1Fields = new HashSet<>();

            author1Fields.add(FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Top"));
            author1Fields.add(FieldDTO.createPrimitiveFieldDTO("authorIdentifier", "ellenid"));
            author1Fields.add(FieldDTO.createVocabFieldDTO("authorIdentifierScheme", "ORCID"));
            author1Fields.add(FieldDTO.createPrimitiveFieldDTO("authorName", "Privileged, Pete"));

            HashSet<FieldDTO> author2Fields = new HashSet<>();

            author2Fields.add(FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Bottom"));
            author2Fields.add(FieldDTO.createPrimitiveFieldDTO("authorIdentifier", "audreyId"));
            author2Fields.add(FieldDTO.createVocabFieldDTO("authorIdentifierScheme", "DAISY"));
            author2Fields.add(FieldDTO.createPrimitiveFieldDTO("authorName", "Awesome, Audrey"));

            List<HashSet<FieldDTO>> authorList = new ArrayList<>();
            authorList.add(author1Fields);
            authorList.add(author2Fields);
            FieldDTO expectedDTO = new FieldDTO();
            expectedDTO.setTypeName("author");
            expectedDTO.setMultipleCompound(authorList);

            FieldDTO authorDTO = dto.getMetadataBlocks().get("citation").getFields().get(1);
            
            // write both dto's to json to compare them with gson parser
            
            JsonElement expected = gson.toJsonTree(expectedDTO, FieldDTO.class);
            JsonElement result = gson.toJsonTree(authorDTO);
            
            assertEquals(expected, result);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

  
                  

}
