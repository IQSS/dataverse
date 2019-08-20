package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.util.JsfHelper;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

@ViewScoped
@Named("selectGuestbookPage")
public class SelectGuestbookPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(SelectGuestbookPage.class.getCanonicalName());

    @EJB
    private DatasetServiceBean datasetService;

    @Inject
    private PermissionsWrapper permissionsWrapper;

    @Inject
    private DataverseRequestServiceBean dvRequestService;

    @Inject
    private PermissionServiceBean permissionService;

    @EJB
    EjbDataverseEngine commandEngine;

    private String persistentId;
    private Long datasetId;

    private Dataset dataset;
    private DatasetVersion workingVersion;
    private DatasetVersion clone;
    private Guestbook selectedGuestbook;
    private List<Guestbook> availableGuestbooks;

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
            dataset = datasetService.findByGlobalId(persistentId);
        } else if (datasetId != null) {
            dataset = datasetService.find(datasetId);
        }

        if (dataset == null) {
            return permissionsWrapper.notFound();
        }

        // Check permisisons
        if (!permissionsWrapper.canUpdateDataset(dvRequestService.getDataverseRequest(), dataset)) {
            return permissionsWrapper.notAuthorized();
        }
        if (datasetService.isInReview(dataset) && !permissionsWrapper.canUpdateAndPublishDataset(dvRequestService.getDataverseRequest(), dataset)) {
            return permissionsWrapper.notAuthorized();
        }

        dataset = datasetService.find(datasetId);
        workingVersion = dataset.getEditVersion();
        clone = workingVersion.cloneDatasetVersion();
        availableGuestbooks = dataset.getDataverseContext().getAvailableGuestbooks();

        JH.addMessage(FacesMessage.SEVERITY_INFO,
                BundleUtil.getStringFromBundle("dataset.message.editTerms.label"),
                BundleUtil.getStringFromBundle("dataset.message.editTerms.message"));

        return StringUtils.EMPTY;
    }

    public String save() {

        try {
            UpdateDatasetVersionCommand cmd = new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest(), new ArrayList<>(), clone);
            cmd.setValidateLenient(true);
            dataset = commandEngine.submit(cmd);
            logger.fine("Successfully executed SaveDatasetCommand.");
        } catch (EJBException ex) {
            logger.log(Level.SEVERE, "Couldn't edit dataset guestbook: " + ex.getMessage(), ex);
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.message.guestbookFailure"));
            return StringUtils.EMPTY;
        } catch(CommandException ex) {
            logger.log(Level.SEVERE, "CommandException, when attempting to update the dataset: " + ex.getMessage(), ex);
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.message.guestbookFailure"));
            return StringUtils.EMPTY;
        }
        return returnToLatestVersion();
    }

    public String cancel() {
        return returnToLatestVersion();
    }

    public void reset() {
        dataset.setGuestbook(null);
    }

    public void viewSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }

    // -------------------- PRIVATE --------------------
    private String returnToLatestVersion() {
        dataset = datasetService.find(dataset.getId());
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
