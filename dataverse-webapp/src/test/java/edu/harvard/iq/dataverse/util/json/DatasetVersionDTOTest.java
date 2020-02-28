/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.util.json;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;

/**
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
    public void testReadDataSet() throws IOException {
        String text = readFileToString("txt/util/JsonDatasetVersion.txt");
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
    }

    private String readFileToString(String resourcePath) throws IOException {
        return IOUtils.resourceToString(resourcePath, StandardCharsets.UTF_8, getClass().getClassLoader());
    }
}
