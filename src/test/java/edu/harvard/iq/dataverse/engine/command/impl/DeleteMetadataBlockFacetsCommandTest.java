package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

/**
 *
 * @author adaybujeda
 */
public class DeleteMetadataBlockFacetsCommandTest {

    private DataverseRequest dataverseRequest;
    private Dataverse dataverse;

    @Before
    public void beforeEachTest() {
        dataverseRequest = Mockito.mock(DataverseRequest.class);
        dataverse = Mockito.mock(Dataverse.class);
    }

    @Test
    public void should_set_metadataBlockFacetRoot_to_false_and_facets_to_empty_list() throws CommandException {
        DeleteMetadataBlockFacetsCommand target = new DeleteMetadataBlockFacetsCommand(dataverseRequest, dataverse);

        CommandContext context = Mockito.mock(CommandContext.class, Mockito.RETURNS_DEEP_STUBS);
        target.execute(context);

        Mockito.verify(dataverse).setMetadataBlockFacetRoot(false);
        Mockito.verify(dataverse).setMetadataBlockFacets(Collections.emptyList());
        Mockito.verify(context.dataverses()).save(dataverse);
    }

    @Test
    public void getEditedDataverse_should_return_set_dataverse() {
        DeleteMetadataBlockFacetsCommand target = new DeleteMetadataBlockFacetsCommand(dataverseRequest, dataverse);

        MatcherAssert.assertThat(target.getEditedDataverse(), Matchers.is(dataverse));
    }

}