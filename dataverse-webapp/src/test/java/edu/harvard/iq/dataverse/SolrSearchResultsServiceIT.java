package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class SolrSearchResultsServiceIT extends WebappArquillianDeployment {

    @Inject
    private SolrSearchResultsService service;

    // -------------------- TESTS --------------------

    @Test
    public void populateDataverseSearchCard() {

        // given
        SolrSearchResult result = new SolrSearchResult();
        result.setEntityId(19L);

        // when
        service.populateDataverseSearchCard(Collections.singletonList(result));

        // then
        assertThat(result.getDataverseAlias()).isEqualTo("ownmetadatablocks");
        assertThat(result.getDataverseParentAlias()).isEqualTo("root");
        assertThat(result.getDataverseAffiliation()).isEqualTo("aff");
    }

    @Test
    public void populateDatasetSearchCard() {

        // given
        SolrSearchResult result = new SolrSearchResult();
        result.setDatasetVersionId(43L);
        result.setIdentifier("doi:10.18150/FK2/MLDB99");

        // when
        service.populateDatasetSearchCard(Collections.singletonList(result));

        // then
        DvObject dataset = result.getEntity();
        assertThat(dataset.getProtocol()).isEqualTo("doi");
        assertThat(dataset.getAuthority()).isEqualTo("10.18150");
        assertThat(dataset.getIdentifier()).isEqualTo("FK2/MLDB99");
        assertThat(dataset.getStorageIdentifier()).isEqualTo("file://10.18150/FK2/MLDB99");
        assertThat(dataset.isPreviewImageAvailable()).isFalse();
    }

    @Test
    public void populateDatafileSearchCard() {

        // given
        SolrSearchResult result = new SolrSearchResult();
        result.setEntityId(55L);

        // when
        service.populateDatafileSearchCard(Collections.singletonList(result));

        // then
        DataFile file = (DataFile) result.getEntity();
        assertThat(file.getStorageIdentifier()).isEqualTo("16d24989319-2c86e28809de");
        assertThat(file.getContentType()).isEqualTo("application/zip");
        assertThat(file.getChecksumType()).isEqualTo(DataFile.ChecksumType.MD5);
        assertThat(file.getChecksumValue()).isEqualTo("7ed0097d7e9ee73cf0952a1f0a07c07e");
        assertThat(file.getFilesize()).isEqualTo(3L);
        assertThat(file.getIngestStatus()).isEqualTo('A');
        assertThat(file.getOwner()).isNotNull();
        assertThat(file.getOwner().getStorageIdentifier()).isEqualTo("file://10.18150/FK2/MLXK1N");
    }
}