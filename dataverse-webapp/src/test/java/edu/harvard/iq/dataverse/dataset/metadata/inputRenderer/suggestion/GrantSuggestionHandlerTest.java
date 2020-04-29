package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import com.google.common.collect.Lists;
import io.vavr.API;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GrantSuggestionHandlerTest {


    GrantSuggestionDao grantSuggestionDao = Mockito.mock(GrantSuggestionDao.class);

    // -------------------- TESTS --------------------

    @Test
    public void fetchSuggestions_WithoutFilters() {
        //given
        GrantSuggestionHandler grantSuggestionHandler = new GrantSuggestionHandler(grantSuggestionDao);

        //when
        Mockito.when(grantSuggestionDao.fetchSuggestions(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).thenReturn(Lists.newArrayList());
        grantSuggestionHandler.generateSuggestions(new HashMap<>(), "", "");

        //then
        Mockito.verify(grantSuggestionDao, Mockito.times(1)).fetchSuggestions(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void fetchSuggestions_WithFilters() {
        //given
        GrantSuggestionHandler grantSuggestionHandler = new GrantSuggestionHandler(grantSuggestionDao);

        //when
        Mockito.when(grantSuggestionDao.fetchSuggestions(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).thenReturn(Lists.newArrayList());
        grantSuggestionHandler.generateSuggestions(API.LinkedMap("","").toJavaMap(), "", "");

        //then
        Mockito.verify(grantSuggestionDao, Mockito.times(1)).fetchSuggestions(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }
}