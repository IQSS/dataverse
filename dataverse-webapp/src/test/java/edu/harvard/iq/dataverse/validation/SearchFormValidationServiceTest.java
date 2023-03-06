package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.validation.field.SearchFormValidationDispatcher;
import edu.harvard.iq.dataverse.validation.field.SearchFormValidationDispatcherFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchFormValidationServiceTest {

    @Mock
    private SearchFormValidationDispatcherFactory dispatcherFactory;

    @Mock
    private SearchFormValidationDispatcher dispatcher;

    @InjectMocks
    private SearchFormValidationService service;

    @Test
    void validateSearchForm() {
        // given
        when(dispatcherFactory.create(anyMap(), anyMap())).thenReturn(dispatcher);

        // when
        service.validateSearchForm(Collections.emptyMap(), Collections.emptyMap());

        // then
        verify(dispatcher, only()).executeValidations();
    }
}