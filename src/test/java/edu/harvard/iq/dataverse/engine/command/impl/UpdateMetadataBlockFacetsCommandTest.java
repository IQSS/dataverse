package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseMetadataBlockFacet;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    public void beforeEachTest() {
        dataverseRequest = Mockito.mock(DataverseRequest.class);
        dataverse = Mockito.mock(Dataverse.class);
    }

    @Test
    void should_throw_IllegalCommandException_when_dataverse_is_not_metadata_facet_root() {
        Mockito.when(dataverse.isMetadataBlockFacetRoot()).thenReturn(false);

        UpdateMetadataBlockFacetsCommand target = new UpdateMetadataBlockFacetsCommand(dataverseRequest, dataverse, Collections.emptyList());

        CommandContext context = Mockito.mock(CommandContext.class, Mockito.RETURNS_DEEP_STUBS);
        Assertions.assertThrows(IllegalCommandException.class, () -> target.execute(context));
    }

    @Test
    public void should_update_facets() throws CommandException {
        Mockito.when(dataverse.isMetadataBlockFacetRoot()).thenReturn(true);
        DataverseMetadataBlockFacet metadataBlockFacet = new DataverseMetadataBlockFacet();
        metadataBlockFacet.setId(MocksFactory.nextId());
        List<DataverseMetadataBlockFacet> metadataBlockFacets = Arrays.asList(metadataBlockFacet);

        UpdateMetadataBlockFacetsCommand target = new UpdateMetadataBlockFacetsCommand(dataverseRequest, dataverse, metadataBlockFacets);

        CommandContext context = Mockito.mock(CommandContext.class, Mockito.RETURNS_DEEP_STUBS);
        target.execute(context);

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