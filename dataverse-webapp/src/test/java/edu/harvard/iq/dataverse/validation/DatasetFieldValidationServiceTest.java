package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.validation.field.DatasetFieldValidationDispatcher;
import edu.harvard.iq.dataverse.validation.field.DatasetFieldValidationDispatcherFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetFieldValidationServiceTest {

    @Mock
    private DatasetVersion datasetVersion;

    @Mock
    private DatasetFieldValidationDispatcherFactory dispatcherFactory;

    @Mock
    private DatasetFieldValidationDispatcher dispatcher;

    @InjectMocks
    private DatasetFieldValidationService service;


    @Test
    void validateFieldsOfDatasetVersion() {
        // given
        when(dispatcherFactory.create(anyList())).thenReturn(dispatcher);

        // when
        service.validateFieldsOfDatasetVersion(datasetVersion);

        // then
        verify(dispatcher, only()).executeValidations();
    }
}