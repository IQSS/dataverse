package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

@ViewScoped
@Named("selectGuestbookPage")
public class SelectGuestbookPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(SelectGuestbookPage.class.getCanonicalName());

    private DatasetDao datasetDao;
    private PermissionsWrapper permissionsWrapper;
    private DataverseRequestServiceBean dvRequestService;
    private SelectGuestBookService selectGuestBookService;

    private String persistentId;
    private Long datasetId;

    private Dataset dataset;
    private DatasetVersion workingVersion;
    private Guestbook selectedGuestbook;
    private Guestbook guestbookBeforeChanges;
    private List<Guestbook> availableGuestbooks;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public SelectGuestbookPage() {
    }

    @Inject
    public SelectGuestbookPage(DatasetDao datasetDao, PermissionsWrapper permissionsWrapper,
                               DataverseRequestServiceBean dvRequestService,
                               SelectGuestBookService selectGuestBookService) {
        this.datasetDao = datasetDao;
        this.permissionsWrapper = permissionsWrapper;
        this.dvRequestService = dvRequestService;
        this.selectGuestBookService = selectGuestBookService;
    }

    // -------------------- GETTERS --------------------
    public String getPersistentId() {
        return persistentId;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public DatasetVersion getWorkingVersion() {
        return workingVersion;
    }

    public Guestbook getSelectedGuestbook() {
        return selectedGuestbook;
    }

    public List<Guestbook> getAvailableGuestbooks() {
        return availableGuestbooks;
    }

    // -------------------- LOGIC --------------------
    public String init() {
        if (persistentId != null) {
            dataset = datasetDao.findByGlobalId(persistentId);
        } else if (datasetId != null) {
            dataset = datasetDao.find(datasetId);
        }

        if (dataset == null) {
            return permissionsWrapper.notFound();
        }

        // Check permisisons
        if (!permissionsWrapper.canUpdateDataset(dvRequestService.getDataverseRequest(), dataset)) {
            return permissionsWrapper.notAuthorized();
        }
        if (datasetDao.isInReview(dataset) && !permissionsWrapper.canUpdateAndPublishDataset(dvRequestService.getDataverseRequest(),
                                                                                             dataset)) {
            return permissionsWrapper.notAuthorized();
        }

        workingVersion = dataset.getEditVersion();
        guestbookBeforeChanges = dataset.getGuestbook();
        selectedGuestbook = dataset.getGuestbook();
        availableGuestbooks = dataset.getDataverseContext().getAvailableGuestbooks();

        JH.addMessage(FacesMessage.SEVERITY_INFO,
                      BundleUtil.getStringFromBundle("dataset.message.editTerms.label"),
                      BundleUtil.getStringFromBundle("dataset.message.editTerms.message"));

        return StringUtils.EMPTY;
    }

    public String save() {

        Try<Dataset> selectGuestbookOperation = Try.of(() -> selectGuestBookService.saveGuestbookChanges(workingVersion,
                                                                                                         Option.of(selectedGuestbook),
                                                                                                         Option.of(guestbookBeforeChanges)));

        if (selectGuestbookOperation.isFailure()) {
            Throwable ex = selectGuestbookOperation.getCause();
            logger.log(Level.SEVERE, "CommandException, when attempting to update the dataset: " + ex.getMessage(), ex);
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.message.guestbookFailure"));
            return StringUtils.EMPTY;
        }

        logger.fine("Successfully executed SaveDatasetCommand.");
        dataset = selectGuestbookOperation.get();

        return returnToLatestVersion();
    }

    public String cancel() {
        return returnToLatestVersion();
    }

    public void reset() {
        selectedGuestbook = null;
    }

    public void viewSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }

    // -------------------- PRIVATE --------------------
    private String returnToLatestVersion() {
        dataset = datasetDao.find(dataset.getId());
        workingVersion = dataset.getLatestVersion();
        if (workingVersion.isDeaccessioned() && dataset.getReleasedVersion() != null) {
            workingVersion = dataset.getReleasedVersion();
        }
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&version=" + workingVersion.getFriendlyVersionNumber() + "&faces-redirect=true";
    }

    // -------------------- SETTERS --------------------

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public void setSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }
}
