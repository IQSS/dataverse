package edu.harvard.iq.dataverse.guestbook;


import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseGuestbookCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuestbookServiceTest {
    @InjectMocks
    private GuestbookService guestbookService;

    @Mock
    private EjbDataverseEngine engineService;
    @Mock
    private DataverseRequestServiceBean dvRequestService;

    private Dataverse dataverse = new Dataverse();

    @BeforeEach
    public void setUp() {
        dataverse.setId(1L);
        when(engineService.submit(Mockito.any(UpdateDataverseGuestbookCommand.class))).thenReturn(this.dataverse);
        when(engineService.submit(Mockito.any(UpdateDataverseCommand.class))).thenReturn(this.dataverse);
    }

    @Test
    public void saveGuestbook() {
        // given
        prepareTestDataverse();
        Guestbook newGuestbook = new Guestbook();
        newGuestbook.setName("newGuestbook");
        newGuestbook.setId(2L);
        newGuestbook.setDataverse(this.dataverse);

        // when
        Dataverse dv = guestbookService.saveGuestbook(newGuestbook);

        // then
        verify(engineService, times(1)).submit(Mockito.any(UpdateDataverseCommand.class));
        Assert.assertSame(this.dataverse, dv);
        Assert.assertEquals(2, dataverse.getGuestbooks().size());
        Assert.assertThat(dataverse.getGuestbooks(), hasItem(newGuestbook));
    }

    @Test
    public void editGuestbook() {
        // given
        prepareTestDataverse();
        Guestbook guestbook = dataverse.getGuestbooks().get(0);
        guestbook.setName("editeGuestbook");

        // when
        Dataverse dv = guestbookService.editGuestbook(guestbook);

        // then
        verify(engineService, times(1)).submit(Mockito.any(UpdateDataverseGuestbookCommand.class));
        Assert.assertEquals(1, dataverse.getGuestbooks().size());
    }

    // -------------------- PRIVATE ---------------------
    private void prepareTestDataverse() {
        dataverse.setName("testDv");

        Guestbook guestbook = new Guestbook();
        guestbook.setName("testGuestbook");
        guestbook.setId(1L);
        guestbook.setDataverse(dataverse);

        dataverse.setGuestbooks(Lists.newArrayList(guestbook));
    }

}
