package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import io.vavr.control.Option;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;

@Stateless
public class SelectGuestBookService {

    private DatasetService datasetService;
    private Clock systemTime = Clock.systemDefaultZone();

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public SelectGuestBookService() {
    }

    @Inject
    public SelectGuestBookService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    // -------------------- LOGIC --------------------

    public Dataset saveGuestbookChanges(Dataset editedDataset,
                                        Option<Guestbook> selectedGuestbook) {

        Option<Guestbook> previousGuestbook = Option.of(editedDataset.getGuestbook());

        if (guestbookIsAddedOrRemoved(selectedGuestbook, previousGuestbook)) {
            editedDataset.setLastChangeForExporterTime(Timestamp.from(Instant.now(systemTime)));
        }

        if(isSelectedGuestbookSameAsPrevious(selectedGuestbook, previousGuestbook)) {
            return editedDataset;
        }

        editedDataset.setGuestbook(selectedGuestbook.getOrNull());

        return datasetService.updateDatasetGuestbook(editedDataset);
    }

    // -------------------- PRIVATE --------------------

    private boolean isSelectedGuestbookSameAsPrevious(Option<Guestbook> selectedGuestbook, Option<Guestbook> previousGuestbook) {
        return isSameGuestbookChosen(selectedGuestbook, previousGuestbook) || isStillNoGuestbookChosen(selectedGuestbook, previousGuestbook);
    }

    private boolean isSameGuestbookChosen(Option<Guestbook> selectedGuestbook, Option<Guestbook> previousGuestbook) {
        return selectedGuestbook.isDefined() && previousGuestbook.isDefined() &&  selectedGuestbook.get().equals(previousGuestbook.get());
    }

    private boolean isStillNoGuestbookChosen(Option<Guestbook> selectedGuestbook, Option<Guestbook> previousGuestbook) {
        return selectedGuestbook.isEmpty() && previousGuestbook.isEmpty();
    }

    private boolean guestbookIsAddedOrRemoved(Option<Guestbook> selectedGuestbook, Option<Guestbook> guestbookBeforeChanges) {
        return isGuestbookAdded(selectedGuestbook, guestbookBeforeChanges) || isGuestbookRemoved(selectedGuestbook, guestbookBeforeChanges);
    }

    private boolean isGuestbookAdded(Option<Guestbook> selectedGuestbook, Option<Guestbook> guestbookBeforeChanges) {
        return guestbookBeforeChanges.isEmpty() && selectedGuestbook.isDefined();
    }

    private boolean isGuestbookRemoved(Option<Guestbook> selectedGuestbook, Option<Guestbook> guestbookBeforeChanges) {
        return guestbookBeforeChanges.isDefined() && selectedGuestbook.isEmpty();
    }

    // -------------------- SETTERS --------------------

    public void setSystemTime(Clock systemTime) {
        this.systemTime = systemTime;
    }
}
