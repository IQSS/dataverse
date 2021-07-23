package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecord;
import org.dspace.xoai.dataprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.model.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class XitemRepositoryTest {

    @InjectMocks
    private XitemRepository xitemRepository;

    @Mock
    private OAIRecordServiceBean recordService;
    @Mock
    private DatasetDao datasetDao;
    
    // -------------------- TESTS --------------------
    
    @Test
    void getRecord() throws IdDoesNotExistException, OAIException {
        // given
        OAIRecord oaiRecord1 = new OAIRecord("", "id", Date.from(Instant.parse("1990-01-01T10:00:00.00Z")));
        OAIRecord oaiRecord2 = new OAIRecord("set2", "id", Date.from(Instant.parse("1990-01-01T11:00:00.00Z")));
        OAIRecord oaiRecord3 = new OAIRecord("set3", "id", Date.from(Instant.parse("1990-01-01T12:00:00.00Z")));
        Dataset dataset = new Dataset();
        dataset.setId(1L);
        when(recordService.findOaiRecordsByGlobalId("id")).thenReturn(Lists.newArrayList(oaiRecord1, oaiRecord2, oaiRecord3));
        when(datasetDao.findByGlobalId("id")).thenReturn(dataset);
        
        // when
        Item item = xitemRepository.getItem("id");
        
        // then
        assertThat(item).isInstanceOf(Xitem.class);
        assertThat(item.getIdentifier()).isEqualTo("id");
        assertThat(((Xitem)item).getDataset()).isEqualTo(dataset);
        assertThat(item.getDatestamp()).hasSameTimeAs("1990-01-01T10:00:00Z");
        assertThat(item.getSets()).extracting(Set::getSpec)
            .containsExactlyInAnyOrder("set2", "set3");
    }

    @Test
    void getRecord_no_oai_record_present() throws IdDoesNotExistException, OAIException {
        // given
        when(recordService.findOaiRecordsByGlobalId("id")).thenReturn(Lists.newArrayList());
        
        // when & then
        assertThatThrownBy(() -> xitemRepository.getItem("id"))
            .isInstanceOf(IdDoesNotExistException.class);
    }

    @Test
    void getRecord_no_dataset() throws IdDoesNotExistException, OAIException {
        // given
        OAIRecord oaiRecord1 = new OAIRecord("", "id", Date.from(Instant.parse("1990-01-01T10:00:00.00Z")));
        when(recordService.findOaiRecordsByGlobalId("id")).thenReturn(Lists.newArrayList(oaiRecord1));
        
        // when & then
        assertThatThrownBy(() -> xitemRepository.getItem("id"))
            .isInstanceOf(IdDoesNotExistException.class);
    }

}
