package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateHarvestingClientCommand;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.timer.DataverseTimerServiceBean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HarvestingClientsServiceTest {

    @InjectMocks
    private HarvestingClientsService harvestingClientsService;

    @Mock
    private EjbDataverseEngine commandEngine;
    @Mock
    private DataverseRequestServiceBean dvRequestService;
    @Mock
    private DeleteHarvestingClientService deleteHarvestingClientService;
    @Mock
    private DeleteHarvestedDatasetsService deleteHarvestedDatasetsService;
    @Mock
    private IndexServiceBean indexService;
    @Mock
    private DataverseTimerServiceBean dataverseTimerService;

    @BeforeEach
    public void setUp() {
        when(commandEngine.submit(any(CreateHarvestingClientCommand.class))).thenReturn(new HarvestingClient());
        when(commandEngine.submit(any(UpdateHarvestingClientCommand.class))).thenReturn(new HarvestingClient());
    }

    @Test
    public void createHarvestingClient() {
        // given & when
        harvestingClientsService.createHarvestingClient(new HarvestingClient());

        // then
        verify(commandEngine, times(1)).submit(any(CreateHarvestingClientCommand.class));
    }

    @Test
    public void updateHarvestingClient() {
        // given & when
        harvestingClientsService.updateHarvestingClient(new HarvestingClient());

        // then
        verify(commandEngine, times(1)).submit(any(UpdateHarvestingClientCommand.class));
    }

    @Test
    public void deleteHarvestingClient() {
        // given & when
        HarvestingClient harvestingClient = new HarvestingClient();
        Dataset dataset = new Dataset();
        dataset.setHarvestedFrom(harvestingClient);
        harvestingClient.setHarvestedDatasets(Collections.singletonList(dataset));

        harvestingClientsService.deleteClient(harvestingClient);

        // then
        verify(dataverseTimerService, times(1)).removeHarvestTimer(any(HarvestingClient.class));
        verify(indexService, times(1)).deleteHarvestedDocuments(any(HarvestingClient.class));
        verify(deleteHarvestedDatasetsService, times(1)).removeHarvestedDatasetInNewTransaction(any());
        verify(deleteHarvestingClientService, times(1)).removeHarvestingClientInNewTransaction(any());
    }
}
