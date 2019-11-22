package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DatasetServiceTest {

    @InjectMocks
    private DatasetService datasetService;

    @Mock
    private DataverseRequestServiceBean dvRequestService;

    @Mock
    private EjbDataverseEngine commandEngine;

    @Test
    public void changeDatasetThumbnail() {
        //given
        Dataset dataset = new Dataset();
        DataFile thumbnailFile = new DataFile();

        when(commandEngine.submit(any(UpdateDatasetThumbnailCommand.class))).thenReturn(new DatasetThumbnail("", thumbnailFile));

        //when
        datasetService.changeDatasetThumbnail(dataset, thumbnailFile);

        //then
        verify(commandEngine, times(1)).submit(any(UpdateDatasetThumbnailCommand.class));

    }
}