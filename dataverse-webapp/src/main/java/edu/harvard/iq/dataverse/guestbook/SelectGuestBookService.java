package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import io.vavr.control.Option;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;

@Stateless
public class SelectGuestBookService {

    private DatasetVersionServiceBean versionService;
    private Clock systemTime = Clock.systemDefaultZone();

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public SelectGuestBookService() {
    }

    @Inject
    public SelectGuestBookService(DatasetVersionServiceBean versionService) {
        this.versionService = versionService;
    }

    // -------------------- LOGIC --------------------

    public Dataset saveGuestbookChanges(DatasetVersion editedDataset,
                                        Option<Guestbook> selectedGuestbook,
                                        Option<Guestbook> guestbookBeforeChanges) {

        Dataset dataset = editedDataset.getDataset();

        if (isGuestbookAddedOrRemoved(selectedGuestbook, guestbookBeforeChanges)) {

            dataset.setGuestbookChangeTime(Timestamp.from(Instant.now(systemTime)));
        }

        dataset.setGuestbook(selectedGuestbook.getOrNull());
        return versionService.updateDatasetVersion(editedDataset, true);
    }

    // -------------------- PRIVATE --------------------

    private boolean isGuestbookAddedOrRemoved(Option<Guestbook> selectedGuestbook, Option<Guestbook> guestbookBeforeChanges) {
        return (guestbookBeforeChanges.isEmpty() && selectedGuestbook.isDefined()) ||
                (guestbookBeforeChanges.isDefined() && selectedGuestbook.isEmpty());
    }

    // -------------------- SETTERS --------------------

    public void setSystemTime(Clock systemTime) {
        this.systemTime = systemTime;
    }
}
