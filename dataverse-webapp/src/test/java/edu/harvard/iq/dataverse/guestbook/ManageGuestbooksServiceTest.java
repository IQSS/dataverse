package edu.harvard.iq.dataverse.guestbook;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteGuestbookCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseGuestbookRootCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ManageGuestbooksServiceTest {
    @InjectMocks
    private ManageGuestbooksService manageGuestbooksService;

    @Mock
    private EjbDataverseEngine engineService;
    @Mock
    private GuestbookServiceBean guestbookService;
    @Mock
    private DataverseRequestServiceBean dataverseRequestService;
    @Mock
    private DataverseServiceBean dataverseService;

    @Captor
    private ArgumentCaptor<UpdateDataverseGuestbookRootCommand> commandArgumentCaptor;

    @BeforeEach
    void setUp() throws CommandException {
        when(engineService.submit(Mockito.any(UpdateDataverseCommand.class))).thenReturn(new Dataverse());
        when(engineService.submit(Mockito.any(DeleteGuestbookCommand.class))).thenReturn(new Dataverse());
        when(dataverseService.find(Mockito.any())).thenReturn(createTestDataverse(false));
        when(dataverseRequestService.getDataverseRequest()).thenReturn(null);
        when(guestbookService.find(Mockito.any())).thenReturn(createTestGuestbook("anyTestGb"));
    }

    @Test
    public void enableGuestbook() throws CommandException {
        // given & when
        Guestbook result = manageGuestbooksService.enableGuestbook(1L);

        // then
        verify(engineService, times(1)).submit(Mockito.any(UpdateDataverseCommand.class));
        Assert.assertTrue(result.isEnabled());
    }

    @Test
    public void disableGuestbook() throws CommandException {
        // given & when
        Guestbook result = manageGuestbooksService.disableGuestbook(1L);

        // then
        verify(engineService, times(1)).submit(Mockito.any(UpdateDataverseCommand.class));
        Assert.assertFalse(result.isEnabled());
    }

    @Test
    public void deleteGuestbook() throws CommandException {
        // given & when
        Dataverse result = manageGuestbooksService.deleteGuestbook(1L);

        // when & then
        verify(engineService, times(1)).submit(Mockito.any(DeleteGuestbookCommand.class));
        Assert.assertNull(result.getGuestbooks());
    }


    @Test
    public void updateAllowGuestbooksFromRootStatus_notAllowed() throws CommandException {
        // given
        Dataverse dv = Mockito.mock(Dataverse.class);
        when(engineService.submit(Mockito.any(UpdateDataverseGuestbookRootCommand.class))).thenReturn(dv);

        // when
        Dataverse result = manageGuestbooksService.updateAllowGuestbooksFromRootStatus(1L, false);

        // then
        verify(engineService, times(1)).submit(commandArgumentCaptor.capture());
        Assert.assertFalse(commandArgumentCaptor.getValue().isNewValue());

        Assert.assertSame(dv, result);
    }

    @Test
    public void updateAllowGuestbooksFromRootStatus_allowed() throws CommandException {
        // given
        Dataverse dv = Mockito.mock(Dataverse.class);
        when(engineService.submit(Mockito.any(UpdateDataverseGuestbookRootCommand.class))).thenReturn(dv);

        // when
        Dataverse result = manageGuestbooksService.updateAllowGuestbooksFromRootStatus(1L, true);

        // then
        verify(engineService, times(1)).submit(commandArgumentCaptor.capture());
        Assert.assertTrue(commandArgumentCaptor.getValue().isNewValue());

        Assert.assertSame(dv, result);
    }

    // -------------------- PRIVATE --------------------

    private Guestbook createTestGuestbook(String guestBookName) {
        Guestbook gb = new Guestbook();
        gb.setName(guestBookName);
        gb.setId(1L);
        gb.setDataverse(createTestDataverse(false));
        gb.getDataverse().setGuestbooks(Lists.newArrayList(gb));

        return gb;
    }

    private Dataverse createTestDataverse(boolean isGuestbookRoot) {
        Dataverse dv = new Dataverse();
        dv.setName("testDv");
        dv.setId(1L);
        dv.setGuestbookRoot(isGuestbookRoot);
        return dv;
    }
}
