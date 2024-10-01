package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseMetadataBlockFacet;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author adaybujeda
 */
public class ListMetadataBlocksCommandTest {

    private DataverseRequest dataverseRequest;
    private Dataverse dataverse;
    private DataverseMetadataBlockFacet metadataBlockFacet;

    @BeforeEach
    public void beforeEachTest() {
        dataverseRequest = Mockito.mock(DataverseRequest.class);
        dataverse = Mockito.mock(Dataverse.class);
        metadataBlockFacet = new DataverseMetadataBlockFacet();
        metadataBlockFacet.setId(MocksFactory.nextId());
        Mockito.when(dataverse.getMetadataBlockFacets()).thenReturn(Arrays.asList(metadataBlockFacet));
    }

    @Test
    public void execute_should_return_dataverse_metadata_block_facets() throws CommandException {
        ListMetadataBlockFacetsCommand target = new ListMetadataBlockFacetsCommand(dataverseRequest, dataverse);

        List<DataverseMetadataBlockFacet> result = target.execute(Mockito.mock(CommandContext.class));

        MatcherAssert.assertThat(result.size(), Matchers.is(1));
        MatcherAssert.assertThat(result.get(0), Matchers.is(metadataBlockFacet));
    }


    @Test
    public void getRequiredPermissions_should_return_empty_for_all_when_dataverse_is_released() {
        Mockito.when(dataverse.isReleased()).thenReturn(true);
        ListMetadataBlockFacetsCommand target = new ListMetadataBlockFacetsCommand(dataverseRequest, dataverse);

        MatcherAssert.assertThat(target.getRequiredPermissions().get(""), Matchers.emptyCollectionOf(Permission.class));
    }

    @Test
    public void getRequiredPermissions_should_return_ViewUnpublishedDataverse_for_all_when_dataverse_is_not_released() {
        Mockito.when(dataverse.isReleased()).thenReturn(false);
        ListMetadataBlockFacetsCommand target = new ListMetadataBlockFacetsCommand(dataverseRequest, dataverse);

        MatcherAssert.assertThat(target.getRequiredPermissions().get("").size(), Matchers.is(1));
        MatcherAssert.assertThat(target.getRequiredPermissions().get(""), Matchers.hasItems(Permission.ViewUnpublishedDataverse));
    }

}