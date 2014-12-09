/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.dto;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ellenk
 */
public class FieldDTOTest {
    FieldDTO author;
    public FieldDTOTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
       
        Set<FieldDTO> authorFields = new HashSet<>();
        
      
        author = FieldDTO.createCompoundFieldDTO("author",
                FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Top"),
                FieldDTO.createPrimitiveFieldDTO("authorIdentifier", "ellenId"),
                FieldDTO.createVocabFieldDTO("authorIdentifierScheme", "ORCID"));
        
        
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of setSinglePrimitive method, of class FieldDTO.
     */
    @Test
    public void testSinglePrimitive() {
        FieldDTO affil = FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Top");
        System.out.println(affil.getSinglePrimitive());
        Assert.assertEquals("Top", affil.getSinglePrimitive());
        
    }

    /**
     * Test of getMultiplePrimitive method, of class FieldDTO.
     */
    @Test
    public void testMultipleVocab() {
        Gson gson = new Gson();
        FieldDTO astroType = new FieldDTO();
        astroType.setTypeName("astroType");
        ArrayList<String> value = new ArrayList<>();
        value.add("Image");
        value.add("Mosaic");
        value.add("EventList");
        astroType.setMultipleVocab(value);
        
        Assert.assertEquals(value, astroType.getMultipleVocab());
        String jsonStr = gson.toJson(astroType);
        FieldDTO astroType2 = gson.fromJson(jsonStr, FieldDTO.class);
        Assert.assertEquals(astroType, astroType2);
        
    }

    /**
     * Test of setMultiplePrimitive method, of class FieldDTO.
     */
    @Test
    public void testSetMultipleValue() {
    }

    /**
     * Test of getSingleCompound method, of class FieldDTO.
     */
    @Test
    public void testSetMultipleCompound() {
         HashSet<FieldDTO> author1Fields = new HashSet<>();
        
        author1Fields.add(FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Top"));
        author1Fields.add(FieldDTO.createPrimitiveFieldDTO("authorIdentifier", "ellenId"));
        author1Fields.add(FieldDTO.createVocabFieldDTO("authorIdentifierScheme", "ORCID"));
          
        HashSet<FieldDTO> author2Fields = new HashSet<>();
        
        author2Fields.add(FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Bottom"));
        author2Fields.add(FieldDTO.createPrimitiveFieldDTO("authorIdentifier", "ernieId"));
        author2Fields.add(FieldDTO.createVocabFieldDTO("authorIdentifierScheme", "DAISY"));
       
        List<HashSet<FieldDTO>> authorList = new ArrayList<>();
        authorList.add(author1Fields);
        authorList.add(author2Fields);
        FieldDTO compoundField = new FieldDTO();
        compoundField.setTypeName("author");
        compoundField.setMultipleCompound(authorList);
        
        Assert.assertEquals(compoundField.getMultipleCompound(), authorList);
    }

    /**
     * Test of setSingleCompound method, of class FieldDTO.
     */
    @Test
    public void testSetSingleCompound() {
        Set<FieldDTO> authorFields = new HashSet<>();
        
        authorFields.add(FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Top"));
        authorFields.add(FieldDTO.createPrimitiveFieldDTO("authorIdentifier", "ellenId"));
        authorFields.add(FieldDTO.createVocabFieldDTO("authorIdentifierScheme", "ORCID"));
        
        FieldDTO compoundField = new FieldDTO();
        compoundField.setSingleCompound(authorFields.toArray(new FieldDTO[]{}));
        Set<FieldDTO>  returned = compoundField.getSingleCompound();   
        Assert.assertTrue(returned.equals(authorFields));
       
    }

    /**
     * Test of setMultipleCompound method, of class FieldDTO.
     */
    @Test
    public void testJsonTree() {
       
         Gson gson = new Gson();
        FieldDTO test1 = new FieldDTO();
       
        test1.value = gson.toJsonTree("ellen", String.class);
        JsonElement elem =  gson.toJsonTree(test1, FieldDTO.class);
        
        FieldDTO field1 = gson.fromJson(elem.getAsJsonObject(), FieldDTO.class);
       
    }
    

    /**
     * Test of getMultipleCompound method, of class FieldDTO.
     */
    @Test
    public void testGetMultipleCompound() {
       
    }

    /**
     * Test of getConvertedValue method, of class FieldDTO.
     */
    @Test
    public void testGetConvertedValue() {
    }

    /**
     * Test of toString method, of class FieldDTO.
     */
    @Test
    public void testToString() {
    }
    
}
