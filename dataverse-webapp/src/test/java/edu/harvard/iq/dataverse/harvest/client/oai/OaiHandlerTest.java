package edu.harvard.iq.dataverse.harvest.client.oai;

import com.google.common.collect.ImmutableList;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.dspace.xoai.serviceprovider.ServiceProvider;
import org.dspace.xoai.serviceprovider.exceptions.IdDoesNotExistException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OaiHandlerTest {

    @Mock
    private ServiceProvider serviceProvider;

    private final List<MetadataFormat> formats = ImmutableList.of(
            new MetadataFormat()
                    .withMetadataPrefix("oai_dc")
                    .withMetadataNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/")
                    .withSchema("http://www.openarchives.org/OAI/2.0/oai_dc.xsd"),
            new MetadataFormat()
                    .withMetadataPrefix("oai_datacite")
                    .withMetadataNamespace("http://datacite.org/schema/kernel-3")
                    .withSchema("http://schema.datacite.org/meta/kernel-3.1/metadata.xsd"));

    @Test
    public void withFetchedMetadataFormat() throws OaiHandlerException, IdDoesNotExistException {
        // given
        HarvestingClient harvestingClient = new HarvestingClient();
        harvestingClient.setMetadataPrefix("oai_dc");
        harvestingClient.setHarvestingSet("default");
        harvestingClient.setHarvestingUrl("http://oai-server.org/oai");
        harvestingClient.setId(1L);
        harvestingClient.setName("OAI DC client");
        OaiHandler oaiHandler = new OaiHandler(harvestingClient).withServiceProvider(serviceProvider);
        when(serviceProvider.listMetadataFormats()).thenReturn(formats.iterator());

        // when
        oaiHandler.withFetchedMetadataFormat();

        // then
        assertThat(oaiHandler.getMetadataFormat()).isEqualTo(formats.get(0));
    }

    @Test
    public void withFetchedMetadataFormat__no_matching_format_found() throws OaiHandlerException, IdDoesNotExistException {
        // given
        HarvestingClient harvestingClient = new HarvestingClient();
        harvestingClient.setMetadataPrefix("oai_ddi");
        harvestingClient.setHarvestingSet("default");
        harvestingClient.setHarvestingUrl("http://oai-server.org/oai");
        harvestingClient.setId(1L);
        harvestingClient.setName("OAI DDI client");
        OaiHandler oaiHandler = new OaiHandler(harvestingClient).withServiceProvider(serviceProvider);
        when(serviceProvider.listMetadataFormats()).thenReturn(formats.iterator());

        // when & then
        assertThatThrownBy(oaiHandler::withFetchedMetadataFormat).isInstanceOf(OaiHandlerException.class);
    }
}
