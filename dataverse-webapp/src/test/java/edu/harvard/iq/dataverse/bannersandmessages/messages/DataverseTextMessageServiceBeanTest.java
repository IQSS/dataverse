package edu.harvard.iq.dataverse.bannersandmessages.messages;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseLocalizedMessageDto;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseMessagesMapper;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseTextMessageDto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static edu.harvard.iq.dataverse.util.DateUtil.convertToDate;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DataverseTextMessageServiceBeanTest {

    private DataverseTextMessageServiceBean service;
    private EntityManager em;
    private DataverseMessagesMapper mapper;

    @Before
    public void setUp() {
        em = mock(EntityManager.class);
        mapper = mock(DataverseMessagesMapper.class);
        service = new DataverseTextMessageServiceBean(em, mapper);
    }

    @Test
    public void shouldReturnNewTextMessageDto() {
        // given
        DataverseTextMessageDto textMessageDto = mock(DataverseTextMessageDto.class);
        when(mapper.mapToNewTextMessage(1L)).thenReturn(textMessageDto);

        // when
        DataverseTextMessageDto retTextMessageDto = service.newTextMessage(1L);

        // then
        assertSame(textMessageDto, retTextMessageDto);
    }

    @Test
    public void shouldReturnTextMessageDto() {
        // given
        DataverseTextMessage message = new DataverseTextMessage();
        message.setId(1L);

        DataverseTextMessageDto textMessageDto = mock(DataverseTextMessageDto.class);

        when(em.find(DataverseTextMessage.class, 1L)).thenReturn(message);
        when(mapper.mapToDto(message)).thenReturn(textMessageDto);

        // when
        DataverseTextMessageDto retTextMessageDto = service.getTextMessage(1L);

        // then
        assertSame(textMessageDto, retTextMessageDto);
        verify(em).find(DataverseTextMessage.class, 1L);
        verifyNoMoreInteractions(em);
    }

    @Test
    public void shouldSaveTextMessage() {
        // given
        LocalDateTime now = LocalDateTime.now();
        DataverseTextMessageDto messageDto = aTextMessageDto(now);
        messageDto.setId(null);

        // and
        Dataverse dataverse = new Dataverse();
        dataverse.setId(messageDto.getDataverseId());
        when(em.find(Dataverse.class, messageDto.getDataverseId())).thenReturn(dataverse);
        when(em.find(DataverseTextMessage.class, messageDto.getId())).thenReturn(null);

        // when
        service.save(messageDto);

        // then
        verifySaveNewTextMessage(messageDto);
    }

    @Test
    public void shouldDeleteTextMessage() {
        // given
        DataverseTextMessage textMessage = new DataverseTextMessage();
        when(em.find(DataverseTextMessage.class, 1L)).thenReturn(textMessage);

        // when
        service.delete(1L);

        // then
        verify(em).remove(textMessage);
    }

    @Test
    public void shouldDeativateTextMessage() {
        // given
        DataverseTextMessage textMessage = new DataverseTextMessage();
        textMessage.setActive(true);

        // and
        when(em.find(DataverseTextMessage.class, 1L)).thenReturn(textMessage);

        // when
        service.deactivate(1L);

        // then
        assertFalse(textMessage.isActive());
        verify(em).merge(textMessage);
    }

    private void verifySaveNewTextMessage(DataverseTextMessageDto dto) {
        ArgumentCaptor<DataverseTextMessage> argument = ArgumentCaptor.forClass(DataverseTextMessage.class);
        verify(em).merge(argument.capture());

        assertNull(argument.getValue().getId());
        assertEquals(dto.isActive(), argument.getValue().isActive());
        assertEquals(dto.getDataverseId(), argument.getValue().getDataverse().getId());
        assertEquals(dto.getFromTime(), argument.getValue().getFromTime());
        assertEquals(dto.getToTime(), argument.getValue().getToTime());
        assertNull(argument.getValue().getVersion());

        dto.getDataverseLocalizedMessage().forEach(lm -> {
            Set<DataverseLocalizedMessage> messages = argument.getValue().getDataverseLocalizedMessages();
            verifyLocaleMessage(argument.getValue(), messages, lm);
        });
        assertEquals(dto.getDataverseLocalizedMessage().size(), argument.getValue().getDataverseLocalizedMessages().size());
    }

    private void verifyLocaleMessage(DataverseTextMessage textMessage, Set<DataverseLocalizedMessage> messages, DataverseLocalizedMessageDto localeDto) {
        messages.stream().filter(lm ->
                                         lm.getMessage().equals(localeDto.getMessage()) &&
                                                 lm.getLocale().equals(localeDto.getLocale()) &&
                                                 lm.getDataverseTextMessage().equals(textMessage) &&
                                                 lm.getId() == null)
                .findAny()
                .orElseThrow(IllegalStateException::new);
    }

    private DataverseTextMessageDto aTextMessageDto(LocalDateTime now) {
        DataverseTextMessageDto messageDto = new DataverseTextMessageDto();
        messageDto.setId(1L);
        messageDto.setActive(true);
        messageDto.setFromTime(convertToDate(now.plusDays(1)));
        messageDto.setToTime(convertToDate(now.plusDays(2)));
        messageDto.setDataverseId(100L);

        List<DataverseLocalizedMessageDto> locales = Lists.newArrayList();
        locales.add(new DataverseLocalizedMessageDto("pl", "Komunikat", "Polski"));
        locales.add(new DataverseLocalizedMessageDto("en", "Info", "English"));
        messageDto.setDataverseLocalizedMessage(locales);
        return messageDto;
    }

    private void verifyDefaultLocales(DataverseTextMessageDto dto) {
        assertEquals(2, dto.getDataverseLocalizedMessage().size());
        verifyDefaultLocale(dto.getDataverseLocalizedMessage(), "en", "English");
        verifyDefaultLocale(dto.getDataverseLocalizedMessage(), "pl", "Polski");
    }

    private void verifyDefaultLocale(List<DataverseLocalizedMessageDto> locales, String locale, String language) {
        assertTrue(locales.stream().anyMatch(lm ->
                                                     lm.getLocale().equals(locale) &&
                                                             lm.getLanguage().equals(language) &&
                                                             lm.getMessage().equals("")
        ));
    }
}