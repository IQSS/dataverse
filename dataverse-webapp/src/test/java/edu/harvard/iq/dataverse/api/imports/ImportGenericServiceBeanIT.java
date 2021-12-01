package edu.harvard.iq.dataverse.api.imports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.MetadataBlockDao;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class ImportGenericServiceBeanIT extends WebappArquillianDeployment {

    @Inject
    private ImportGenericServiceBean importGenericServiceBean;

    @Inject
    private DatasetFieldServiceBean datasetFieldService;

    @Inject
    private MetadataBlockDao metadataBlockDao;

    @Inject
    private SettingsServiceBean settingsService;

    @Test
    public void doImport() throws IOException, XMLStreamException, JsonParseException {

        //given
        final String xml = IOUtils.resourceToString("xml/imports/oaidc.xml", StandardCharsets.UTF_8, ImportGenericServiceBeanIT.class
                .getClassLoader());

        //when
        final DatasetDTO datasetDTO = importGenericServiceBean.processOAIDCxml(xml);

        //then
        Assertions.assertThat(parseDTOtoEntity(datasetDTO)).isNotNull();
    }

    private Dataset parseDTOtoEntity(DatasetDTO datasetDTO) throws JsonParseException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(datasetDTO);

        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject obj = jsonReader.readObject();

        JsonParser parser = new JsonParser(datasetFieldService, metadataBlockDao, settingsService);
        return parser.parseDataset(obj);
    }
}
