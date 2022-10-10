package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author adaybujeda
 */
public class DataverseTest {

    private Dataverse OWNER;
    private List<DataverseMetadataBlockFacet> OWNER_METADATABLOCKFACETS;

    @Before
    public void beforeEachTest() {
        OWNER = new Dataverse();
        OWNER.setId(MocksFactory.nextId());
        OWNER.setMetadataBlockRoot(true);

        DataverseMetadataBlockFacet metadataBlockFacet = new DataverseMetadataBlockFacet();
        metadataBlockFacet.setId(MocksFactory.nextId());
        OWNER_METADATABLOCKFACETS = Arrays.asList(metadataBlockFacet);
        OWNER.setMetadataBlockFacets(OWNER_METADATABLOCKFACETS);
    }

    @Test
    public void getMetadataBlockFacets_should_return_internal_metadatablockfacets_when_metadatablockfacetroot_is_true() {
        Dataverse target = new Dataverse();
        target.setId(MocksFactory.nextId());
        target.setMetadataBlockFacetRoot(true);
        target.setOwner(OWNER);

        DataverseMetadataBlockFacet metadataBlockFacet = new DataverseMetadataBlockFacet();
        metadataBlockFacet.setId(MocksFactory.nextId());
        List<DataverseMetadataBlockFacet> internalMetadataBlockFacets = Arrays.asList(metadataBlockFacet);
        target.setMetadataBlockFacets(internalMetadataBlockFacets);
        List<DataverseMetadataBlockFacet> result = target.getMetadataBlockFacets();

        MatcherAssert.assertThat(result, Matchers.is(internalMetadataBlockFacets));
    }

    @Test
    public void getMetadataBlockFacets_should_return_owner_metadatablockfacets_when_metadatablockfacetroot_is_false() {
        Dataverse target = new Dataverse();
        target.setId(MocksFactory.nextId());
        target.setMetadataBlockFacetRoot(false);
        target.setOwner(OWNER);

        DataverseMetadataBlockFacet metadataBlockFacet = new DataverseMetadataBlockFacet();
        metadataBlockFacet.setId(MocksFactory.nextId());
        List<DataverseMetadataBlockFacet> internalMetadataBlockFacets = Arrays.asList(metadataBlockFacet);
        target.setMetadataBlockFacets(internalMetadataBlockFacets);
        List<DataverseMetadataBlockFacet> result = target.getMetadataBlockFacets();

        MatcherAssert.assertThat(result, Matchers.is(OWNER_METADATABLOCKFACETS));
    }

}