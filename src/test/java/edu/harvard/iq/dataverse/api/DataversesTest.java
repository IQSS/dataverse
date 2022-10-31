package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseMetadataBlockFacet;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.GuestbookServiceBean;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.datadeposit.SwordServiceBean;
import edu.harvard.iq.dataverse.api.dto.DataverseMetadataBlockFacetDTO;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.engine.command.impl.ListMetadataBlockFacetsCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateMetadataBlockFacetRootCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateMetadataBlockFacetsCommand;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author adaybujeda
 */
@RunWith(MockitoJUnitRunner.class)
public class DataversesTest {
    // From AbstractApiBean class
    @Mock
    private EjbDataverseEngine engineSvc;
    @Mock
    private MetadataBlockServiceBean metadataBlockSvc;
    @Mock
    private PrivateUrlServiceBean privateUrlSvc;
    @Mock
    private HttpServletRequest httpRequest;

    // From Dataverses class
    @Mock
    private ExplicitGroupServiceBean explicitGroupSvc;
    @Mock
    private ImportServiceBean importService;
    @Mock
    private SettingsServiceBean settingsService;
    @Mock
    private GuestbookResponseServiceBean guestbookResponseService;
    @Mock
    private GuestbookServiceBean guestbookService;
    @Mock
    private DataverseServiceBean dataverseService;
    @Mock
    private SwordServiceBean swordService;

    @InjectMocks
    private Dataverses target;

    private Dataverse VALID_DATAVERSE;

    @Before
    public void beforeEachTest() {
        VALID_DATAVERSE = new Dataverse();
        VALID_DATAVERSE.setId(MocksFactory.nextId());
        VALID_DATAVERSE.setAlias(UUID.randomUUID().toString());
        VALID_DATAVERSE.setMetadataBlockFacetRoot(true);

        Mockito.lenient().when(dataverseService.findByAlias(VALID_DATAVERSE.getAlias())).thenReturn(VALID_DATAVERSE);
        Mockito.lenient().when(httpRequest.getHeader("X-Dataverse-key")).thenReturn(UUID.randomUUID().toString());
        Mockito.lenient().when(privateUrlSvc.getPrivateUrlUserFromToken(Mockito.anyString())).thenReturn(new PrivateUrlUser(0));
    }

    @Test
    public void listMetadataBlockFacets_should_return_404_when_dataverse_is_not_found() {
        String dataverseAlias = UUID.randomUUID().toString();
        Mockito.when(dataverseService.findByAlias(dataverseAlias)).thenReturn(null);
        Response result = target.listMetadataBlockFacets(dataverseAlias);

        MatcherAssert.assertThat(result.getStatus(), Matchers.is(404));
        Mockito.verifyNoMoreInteractions(engineSvc);
    }

    @Test
    public void listMetadataBlockFacets_should_return_the_list_of_metadataBlockFacetDTOs() throws Exception{
        MetadataBlock metadataBlock = Mockito.mock(MetadataBlock.class);
        Mockito.when(metadataBlock.getName()).thenReturn("test_metadata_block_name");
        Mockito.when(metadataBlock.getLocaleDisplayFacet()).thenReturn("test_metadata_facet_name");
        DataverseMetadataBlockFacet dataverseMetadataBlockFacet = new DataverseMetadataBlockFacet();
        dataverseMetadataBlockFacet.setDataverse(VALID_DATAVERSE);
        dataverseMetadataBlockFacet.setMetadataBlock(metadataBlock);
        Mockito.when(engineSvc.submit(Mockito.any(ListMetadataBlockFacetsCommand.class))).thenReturn(Arrays.asList(dataverseMetadataBlockFacet));

        Response response = target.listMetadataBlockFacets(VALID_DATAVERSE.getAlias());

        MatcherAssert.assertThat(response.getStatus(), Matchers.is(200));
        MatcherAssert.assertThat(response.getEntity(), Matchers.notNullValue());
        DataverseMetadataBlockFacetDTO result = (DataverseMetadataBlockFacetDTO)response.getEntity();
        MatcherAssert.assertThat(result.getDataverseId(), Matchers.is(VALID_DATAVERSE.getId()));
        MatcherAssert.assertThat(result.getDataverseAlias(), Matchers.is(VALID_DATAVERSE.getAlias()));
        MatcherAssert.assertThat(result.isMetadataBlockFacetRoot(), Matchers.is(VALID_DATAVERSE.isMetadataBlockFacetRoot()));
        MatcherAssert.assertThat(result.getMetadataBlocks().size(), Matchers.is(1));
        MatcherAssert.assertThat(result.getMetadataBlocks().get(0).getMetadataBlockName(), Matchers.is("test_metadata_block_name"));
        MatcherAssert.assertThat(result.getMetadataBlocks().get(0).getMetadataBlockFacet(), Matchers.is("test_metadata_facet_name"));

        Mockito.verify(engineSvc).submit(Mockito.any(ListMetadataBlockFacetsCommand.class));
    }

    @Test
    public void listMetadataBlockFacets_should_return_empty_list_when_metadata_block_facet_is_null() throws Exception{
        Mockito.when(engineSvc.submit(Mockito.any(ListMetadataBlockFacetsCommand.class))).thenReturn(null);

        Response response = target.listMetadataBlockFacets(VALID_DATAVERSE.getAlias());

        MatcherAssert.assertThat(response.getStatus(), Matchers.is(200));
        DataverseMetadataBlockFacetDTO result = (DataverseMetadataBlockFacetDTO)response.getEntity();
        MatcherAssert.assertThat(result.getDataverseId(), Matchers.is(VALID_DATAVERSE.getId()));
        MatcherAssert.assertThat(result.getDataverseAlias(), Matchers.is(VALID_DATAVERSE.getAlias()));
        MatcherAssert.assertThat(result.isMetadataBlockFacetRoot(), Matchers.is(VALID_DATAVERSE.isMetadataBlockFacetRoot()));
        MatcherAssert.assertThat(result.getMetadataBlocks(), Matchers.is(Collections.emptyList()));

        Mockito.verify(engineSvc).submit(Mockito.any(ListMetadataBlockFacetsCommand.class));
    }

    @Test
    public void setMetadataBlockFacets_should_return_404_when_dataverse_is_not_found() {
        String dataverseAlias = UUID.randomUUID().toString();
        Mockito.when(dataverseService.findByAlias(dataverseAlias)).thenReturn(null);
        Response result = target.setMetadataBlockFacets(dataverseAlias, Collections.emptyList());

        MatcherAssert.assertThat(result.getStatus(), Matchers.is(404));
        Mockito.verifyNoMoreInteractions(engineSvc);
    }

    @Test
    public void setMetadataBlockFacets_should_return_400_when_dataverse_has_metadata_facet_root_set_to_false() {
        String dataverseAlias = UUID.randomUUID().toString();
        Dataverse dataverse = Mockito.mock(Dataverse.class);
        Mockito.when(dataverse.isMetadataBlockFacetRoot()).thenReturn(false);
        Mockito.when(dataverseService.findByAlias(dataverseAlias)).thenReturn(dataverse);

        Response result = target.setMetadataBlockFacets(dataverseAlias, Collections.emptyList());

        MatcherAssert.assertThat(result.getStatus(), Matchers.is(400));
        Mockito.verifyNoMoreInteractions(engineSvc);
    }

    @Test
    public void setMetadataBlockFacets_should_return_400_when_invalid_metadata_block() {
        Mockito.when(metadataBlockSvc.findByName("valid_block")).thenReturn(new MetadataBlock());
        Mockito.when(metadataBlockSvc.findByName("invalid_block")).thenReturn(null);
        List<String> metadataBlocks = Arrays.asList("valid_block", "invalid_block");
        Response result = target.setMetadataBlockFacets(VALID_DATAVERSE.getAlias(), metadataBlocks);

        MatcherAssert.assertThat(result.getStatus(), Matchers.is(400));
        Mockito.verifyNoMoreInteractions(engineSvc);
    }

    @Test
    public void setMetadataBlockFacets_should_return_200_when_update_is_successful() throws Exception {
        MetadataBlock validBlock = new MetadataBlock();
        Mockito.when(metadataBlockSvc.findByName("valid_block")).thenReturn(validBlock);
        List<String> metadataBlocks = Arrays.asList("valid_block");
        Response result = target.setMetadataBlockFacets(VALID_DATAVERSE.getAlias(), metadataBlocks);

        MatcherAssert.assertThat(result.getStatus(), Matchers.is(200));
        ArgumentCaptor<UpdateMetadataBlockFacetsCommand> updateCommand = ArgumentCaptor.forClass(UpdateMetadataBlockFacetsCommand.class);
        Mockito.verify(engineSvc).submit(updateCommand.capture());

        MatcherAssert.assertThat(updateCommand.getValue().getEditedDataverse(), Matchers.is(VALID_DATAVERSE));
        MatcherAssert.assertThat(updateCommand.getValue().getMetadataBlockFacets().size(), Matchers.is(1));
        MatcherAssert.assertThat(updateCommand.getValue().getMetadataBlockFacets().get(0).getDataverse(), Matchers.is(VALID_DATAVERSE));
        MatcherAssert.assertThat(updateCommand.getValue().getMetadataBlockFacets().get(0).getMetadataBlock(), Matchers.is(validBlock));
    }

    @Test
    public void setMetadataBlockFacets_should_support_empty_metadatablock_list() throws Exception{
        Response result = target.setMetadataBlockFacets(VALID_DATAVERSE.getAlias(), Collections.emptyList());

        MatcherAssert.assertThat(result.getStatus(), Matchers.is(200));
        Mockito.verify(engineSvc).submit(Mockito.any(UpdateMetadataBlockFacetsCommand.class));
    }

    @Test
    public void updateMetadataBlockFacetsRoot_should_return_404_when_dataverse_is_not_found() {
        String dataverseAlias = UUID.randomUUID().toString();
        Mockito.when(dataverseService.findByAlias(dataverseAlias)).thenReturn(null);
        Response result = target.updateMetadataBlockFacetsRoot(dataverseAlias, "true");

        MatcherAssert.assertThat(result.getStatus(), Matchers.is(404));
        Mockito.verifyNoMoreInteractions(engineSvc);
    }

    @Test
    public void updateMetadataBlockFacetsRoot_should_return_400_when_invalid_boolean() throws Exception{
        Response result = target.updateMetadataBlockFacetsRoot(VALID_DATAVERSE.getAlias(), "invalid");

        MatcherAssert.assertThat(result.getStatus(), Matchers.is(400));
        Mockito.verifyNoMoreInteractions(engineSvc);
    }

    @Test
    public void updateMetadataBlockFacetsRoot_should_return_200_and_make_no_update_when_dataverse_is_found_and_facet_root_has_not_changed() {
        // VALID_DATAVERSE.metadataBlockFacetRoot is true
        Response result = target.updateMetadataBlockFacetsRoot(VALID_DATAVERSE.getAlias(), "true");

        MatcherAssert.assertThat(result.getStatus(), Matchers.is(200));
        Mockito.verifyZeroInteractions(engineSvc);
    }

    @Test
    public void updateMetadataBlockFacetsRoot_should_return_200_and_execute_command_when_dataverse_is_found_and_facet_root_has_changed() throws Exception {
        // VALID_DATAVERSE.metadataBlockFacetRoot is true
        Response result = target.updateMetadataBlockFacetsRoot(VALID_DATAVERSE.getAlias(), "false");

        MatcherAssert.assertThat(result.getStatus(), Matchers.is(200));
        ArgumentCaptor<UpdateMetadataBlockFacetRootCommand> updateRootCommand = ArgumentCaptor.forClass(UpdateMetadataBlockFacetRootCommand.class);
        Mockito.verify(engineSvc).submit(updateRootCommand.capture());

        MatcherAssert.assertThat(updateRootCommand.getValue().getEditedDataverse(), Matchers.is(VALID_DATAVERSE));
    }
}