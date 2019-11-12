package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseGuestbookCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.Date;

@Stateless
public class GuestbookService {

    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;
    private GuestbookServiceBean guestbookService;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public GuestbookService() {
    }

    @Inject
    public GuestbookService(DataverseRequestServiceBean dvRequestService, GuestbookServiceBean guestbookService,
                            EjbDataverseEngine commandEngine) {
        this.dvRequestService = dvRequestService;
        this.guestbookService = guestbookService;
        this.commandEngine = commandEngine;
    }

    // -------------------- LOGIC --------------------
    public Dataverse saveGuestbook(Guestbook guestbook) {
        Dataverse dataverse = guestbook.getDataverse();
        guestbook.setCreateTime(new Timestamp(new Date().getTime()));
        guestbook.setUsageCount(0L);
        guestbook.setEnabled(true);

        dataverse.getGuestbooks().add(guestbook);

        return commandEngine.submit(new UpdateDataverseCommand(dataverse, null, null, dvRequestService.getDataverseRequest(), null));
    }

    public Dataverse editGuestbook(Guestbook guestbook) {
        return commandEngine.submit(new UpdateDataverseGuestbookCommand(guestbook.getDataverse(), guestbook, dvRequestService.getDataverseRequest()));
    }

}
