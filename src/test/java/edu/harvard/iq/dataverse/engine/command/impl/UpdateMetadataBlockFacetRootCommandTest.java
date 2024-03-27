package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseMetadataBlockFacet;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author adaybujeda
 */
public class UpdateMetadataBlockFacetRootCommandTest {

    private DataverseRequest dataverseRequest;
    private Dataverse dataverse;

    @BeforeEach
    public void beforeEachTest() {
        dataverseRequest = Mockito.mock(DataverseRequest.class);
        dataverse = Mockito.mock(Dataverse.class);
    }

    @Test
    public void should_not_update_dataverse_when_root_value_does_not_change() throws CommandException {
        boolean metadataBlockFacetRoot = true;
        Mockito.when(dataverse.isMetadataBlockFacetRoot()).thenReturn(metadataBlockFacetRoot);
        UpdateMetadataBlockFacetRootCommand target = new UpdateMetadataBlockFacetRootCommand(dataverseRequest, dataverse, metadataBlockFacetRoot);

        CommandContext context = Mockito.mock(CommandContext.class, Mockito.RETURNS_DEEP_STUBS);
        target.execute(context);

        Mockito.verify(dataverse).isMetadataBlockFacetRoot();
        Mockito.verifyNoMoreInteractions(dataverse);
        Mockito.verifyNoInteractions(context.dataverses());
    }

    @Test
    public void should_set_metadataBlockFacetRoot_and_update_metadata_block_facets_to_empty_list_when_root_value_changes_to_false() throws CommandException {
        Mockito.when(dataverse.isMetadataBlockFacetRoot()).thenReturn(true);
        UpdateMetadataBlockFacetRootCommand target = new UpdateMetadataBlockFacetRootCommand(dataverseRequest, dataverse, false);

        CommandContext context = Mockito.mock(CommandContext.class, Mockito.RETURNS_DEEP_STUBS);
        target.execute(context);

        Mockito.verify(dataverse).isMetadataBlockFacetRoot();
        Mockito.verify(dataverse).setMetadataBlockFacetRoot(false);
        Mockito.verify(dataverse).setMetadataBlockFacets(Collections.emptyList());
        Mockito.verifyNoMoreInteractions(dataverse);
        Mockito.verify(context.dataverses()).save(dataverse);
    }

    @Test
    public void should_set_metadataBlockFacetRoot_and_update_metadata_block_facets_to_parent_list_when_root_value_changes_to_true() throws CommandException {
        Dataverse parentDataverse = Mockito.mock(Dataverse.class);
        DataverseMetadataBlockFacet blockFacet1 = new DataverseMetadataBlockFacet();
        MetadataBlock block1 = Mockito.mock(MetadataBlock.class);
        blockFacet1.setDataverse(parentDataverse);
        blockFacet1.setMetadataBlock(block1);
        DataverseMetadataBlockFacet blockFacet2 = new DataverseMetadataBlockFacet();
        MetadataBlock block2 = Mockito.mock(MetadataBlock.class);
        blockFacet2.setDataverse(parentDataverse);
        blockFacet2.setMetadataBlock(block2);
        Mockito.when(dataverse.isMetadataBlockFacetRoot()).thenReturn(false);
        Mockito.when(dataverse.getMetadataBlockFacets()).thenReturn(Arrays.asList(blockFacet1, blockFacet2));
        UpdateMetadataBlockFacetRootCommand target = new UpdateMetadataBlockFacetRootCommand(dataverseRequest, dataverse, true);

        CommandContext context = Mockito.mock(CommandContext.class, Mockito.RETURNS_DEEP_STUBS);
        target.execute(context);

        Mockito.verify(dataverse).isMetadataBlockFacetRoot();
        Mockito.verify(dataverse).getMetadataBlockFacets();
        Mockito.verify(dataverse).setMetadataBlockFacetRoot(true);

        ArgumentCaptor<List<DataverseMetadataBlockFacet>> createdBlockFacets = ArgumentCaptor.forClass(List.class);
        Mockito.verify(dataverse).setMetadataBlockFacets(createdBlockFacets.capture());
        MatcherAssert.assertThat(createdBlockFacets.getValue().size(), Matchers.is(2));
        MatcherAssert.assertThat(createdBlockFacets.getValue().get(0).getDataverse(), Matchers.is(dataverse));
        MatcherAssert.assertThat(createdBlockFacets.getValue().get(0).getMetadataBlock(), Matchers.is(block1));
        MatcherAssert.assertThat(createdBlockFacets.getValue().get(1).getDataverse(), Matchers.is(dataverse));
        MatcherAssert.assertThat(createdBlockFacets.getValue().get(1).getMetadataBlock(), Matchers.is(block2));
        Mockito.verifyNoMoreInteractions(dataverse);
        Mockito.verify(context.dataverses()).save(dataverse);
    }

    @Test
    public void getEditedDataverse_should_return_set_dataverse() {
        UpdateMetadataBlockFacetRootCommand target = new UpdateMetadataBlockFacetRootCommand(dataverseRequest, dataverse, true);

        MatcherAssert.assertThat(target.getEditedDataverse(), Matchers.is(dataverse));
    }

    @Test
    public void getMetadataBlockFacetRoot_should_return_set_metadata_block_facet_root() {
        UpdateMetadataBlockFacetRootCommand target = new UpdateMetadataBlockFacetRootCommand(dataverseRequest, dataverse, true);

        MatcherAssert.assertThat(target.getMetadataBlockFacetRoot(), Matchers.is(true));
    }

}