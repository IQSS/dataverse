package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseMetadataBlockFacet;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author adaybujeda
 */
public class UpdateMetadataBlockFacetsCommandTest {

    private DataverseRequest dataverseRequest;
    private Dataverse dataverse;

    @Before
    public void beforeEachTest() {
        dataverseRequest = Mockito.mock(DataverseRequest.class);
        dataverse = Mockito.mock(Dataverse.class);
    }

    @Test
    public void should_set_metadataBlockFacetRoot_to_true_and_update_facets() throws CommandException {
        DataverseMetadataBlockFacet metadataBlockFacet = new DataverseMetadataBlockFacet();
        metadataBlockFacet.setId(MocksFactory.nextId());
        List<DataverseMetadataBlockFacet> metadataBlockFacets = Arrays.asList(metadataBlockFacet);

        UpdateMetadataBlockFacetsCommand target = new UpdateMetadataBlockFacetsCommand(dataverseRequest, dataverse, metadataBlockFacets);

        CommandContext context = Mockito.mock(CommandContext.class, Mockito.RETURNS_DEEP_STUBS);
        target.execute(context);

        Mockito.verify(dataverse).setMetadataBlockFacetRoot(true);
        Mockito.verify(dataverse).setMetadataBlockFacets(metadataBlockFacets);
        Mockito.verify(context.dataverses()).save(dataverse);
    }

    @Test
    public void getEditedDataverse_should_return_set_dataverse() {
        UpdateMetadataBlockFacetsCommand target = new UpdateMetadataBlockFacetsCommand(dataverseRequest, dataverse, Collections.emptyList());

        MatcherAssert.assertThat(target.getEditedDataverse(), Matchers.is(dataverse));
    }

    @Test
    public void getMetadataBlockFacets_should_return_set_metadata_block_facets() {
        DataverseMetadataBlockFacet metadataBlockFacet = new DataverseMetadataBlockFacet();
        metadataBlockFacet.setId(MocksFactory.nextId());
        List<DataverseMetadataBlockFacet> metadataBlockFacets = Arrays.asList(metadataBlockFacet);
        UpdateMetadataBlockFacetsCommand target = new UpdateMetadataBlockFacetsCommand(dataverseRequest, dataverse, metadataBlockFacets);

        MatcherAssert.assertThat(target.getMetadataBlockFacets(), Matchers.is(metadataBlockFacets));
    }

}