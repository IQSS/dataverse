package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateHarvestingClientCommand;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
}
