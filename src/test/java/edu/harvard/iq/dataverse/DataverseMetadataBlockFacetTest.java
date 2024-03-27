package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 *
 * @author adaybujeda
 */
public class DataverseMetadataBlockFacetTest {

    @Test
    public void equals_should_be_based_on_id() {
        Long sameId = MocksFactory.nextId();
        DataverseMetadataBlockFacet target1 = new DataverseMetadataBlockFacet();
        target1.setId(sameId);
        target1.setDataverse(new Dataverse());
        target1.setMetadataBlock(new MetadataBlock());

        DataverseMetadataBlockFacet target2 = new DataverseMetadataBlockFacet();
        target2.setId(sameId);
        target2.setDataverse(new Dataverse());
        target2.setMetadataBlock(new MetadataBlock());

        MatcherAssert.assertThat(target1.equals(target2), Matchers.is(true));


        Dataverse sameDataverse = new Dataverse();
        MetadataBlock sameMetadataBlock = new MetadataBlock();
        target1 = new DataverseMetadataBlockFacet();
        target1.setId(MocksFactory.nextId());
        target1.setDataverse(sameDataverse);
        target1.setMetadataBlock(sameMetadataBlock);

        target2 = new DataverseMetadataBlockFacet();
        target2.setId(MocksFactory.nextId());
        target2.setDataverse(sameDataverse);
        target2.setMetadataBlock(sameMetadataBlock);

        MatcherAssert.assertThat(target1.equals(target2), Matchers.is(false));
    }

}