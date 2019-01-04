package edu.harvard.iq.dataverse.dataverse.messages;

import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseLocalizedMessageDto;
import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseMessagesMapper;
import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseTextMessageDto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        mapper = new DataverseMessagesMapper();
        service = new DataverseTextMessageServiceBean(em, mapper);
    }

    @Test
    public void shouldReturnNewTextMessageDto() {
        // when
        DataverseTextMessageDto dto = service.newTextMessage(1L);

        // then
        verifyNewDto(dto);
    }

    @Test
    public void shouldReturnTextMessageDto() {
        // given
        DataverseTextMessage message = new DataverseTextMessage();
        message.setId(1L);
        Dataverse dataverse = new Dataverse();
        dataverse.setId(100L);
        message.setDataverse(dataverse);

        // and
        when(em.find(DataverseTextMessage.class, 1L)).thenReturn(message);

        // when
        DataverseTextMessageDto dto = service.getTextMessage(1L);

        // then
        assertEquals(new Long(1L), dto.getId());
        verify(em).find(DataverseTextMessage.class, 1L);
        verifyNoMoreInteractions(em);
    }

    @Test
    public void shouldSaveTextMessage() {
        // given
        LocalDateTime now = LocalDateTime.now();
        DataverseTextMessageDto messageDto = aTextMessageDto(now);

        // and
        Dataverse dataverse = new Dataverse();
        dataverse.setId(messageDto.getDataverseId());
        when(em.find(Dataverse.class, messageDto.getDataverseId())).thenReturn(dataverse);

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
        verify(em).persist(argument.capture());

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
        messageDto.setFromTime(now.plusDays(1));
        messageDto.setToTime(now.plusDays(2));
        messageDto.setDataverseId(100L);

        Set<DataverseLocalizedMessageDto> locales = Sets.newHashSet();
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

    private void verifyDefaultLocale(Set<DataverseLocalizedMessageDto> locales, String locale, String language) {
        assertTrue(locales.stream().anyMatch(lm ->
                    lm.getLocale().equals(locale) &&
                    lm.getLanguage().equals(language) &&
                    lm.getMessage().equals("")
                ));
    }

    private void verifyNewDto(DataverseTextMessageDto dto) {
        assertEquals(new Long(1L), dto.getDataverseId());
        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getFromTime());
        assertNull(dto.getToTime());
        assertFalse(dto.isActive());
        verifyDefaultLocales(dto);
    }
}