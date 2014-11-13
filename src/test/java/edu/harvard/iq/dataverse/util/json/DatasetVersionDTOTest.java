/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import com.google.gson.Gson;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO.FieldDTO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ellenk
 */
public class DatasetVersionDTOTest {
    
    public DatasetVersionDTOTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testReadDataSet() {
        try {
            File pwd = new File(".");
            File file = new File("src/test/java/edu/harvard/iq/dataverse/util/json/JsonDatasetVersion.txt");
            String text = new Scanner(file).useDelimiter("\\Z").next();
            Gson gson = new Gson();
            DatasetVersionDTO dto = gson.fromJson(text,DatasetVersionDTO.class);
            String jsontext = gson.toJson(dto);
            
            ArrayList<ArrayList<FieldDTO>> authors = dto.getMetadataBlocks().get("citation").getFields().get(1).getMultipleCompound();
           
            dto.metadataBlocks.get("geospatial").fields.get(1);
            System.out.println("jontext is " + jsontext);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    
  
                  

}
