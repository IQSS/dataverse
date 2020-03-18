package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.MoveDatasetException;
import edu.harvard.iq.dataverse.engine.command.exception.MoveDatasetException.AdditionalStatus;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDatasetCommand;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import edu.harvard.iq.dataverse.util.JsfHelper;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

@ViewScoped
@Named("DashboardDatamovePage")
public class DashboardDatamovePage implements Serializable {
    private static final Logger logger = Logger.getLogger(DashboardDatamovePage.class.getCanonicalName());

    @Inject
    DataverseRequestServiceBean requestService;

    @EJB
    private DatasetDao datasetDao;

    @EJB
    private DataverseDao dataverseDao;

    @Inject
    private DataverseSession session;

    @Inject
    private PermissionsWrapper permissionsWrapper;

    @EJB
    private EjbDataverseEngine commandEngine;

    @Inject
    private SettingsWrapper settings;

    private boolean forceMove = false;

    private Dataset sourceDataset;

    private Dataverse targetDataverse;

    // -------------------- GETTERS --------------------

    public boolean isForceMove() {
        return forceMove;
    }

    public Dataset getSourceDataset() {
        return sourceDataset;
    }

    public Dataverse getTargetDataverse() {
        return targetDataverse;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        User user = session.getUser();
        if (user == null || !user.isAuthenticated() || !user.isSuperuser()) {
            return permissionsWrapper.notAuthorized();
        }

        JsfHelper.addMessage(FacesMessage.SEVERITY_INFO,  getStringFromBundle("dashboard.datamove.manage"),
                getStringFromBundle("dashboard.datamove.message", settings.getGuidesBaseUrl(), settings.getGuidesVersion()));
        return StringUtils.EMPTY;
    }

    public List<Dataset> completeSourceDataset(String query) {
        if (query.contains("/")) {
            Dataset ds = datasetDao.findByGlobalId(query);
            return ds != null ? Collections.singletonList(ds) : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public List<Dataverse> completeTargetDataverse(String query) {
        return dataverseDao.filterByAliasQuery(query);
    }

    public void move() {
        if (sourceDataset == null || targetDataverse == null) {
            // We should never get here, but in case of some unexpected failure we should be prepared nevertheless
            JsfHelper.addFlashErrorMessage(getStringFromBundle("dashboard.datamove.empty.fields"));
            return;
        }

        Summary summary = new Summary().addParameter(sourceDataset.getDisplayName())
                .addParameter(extractSourcePersistentId())
                .addParameter(targetDataverse.getName());

        try {
            DataverseRequest dataverseRequest = requestService.getDataverseRequest();
            commandEngine.submit(new MoveDatasetCommand(dataverseRequest, sourceDataset, targetDataverse, forceMove));
            logger.info(createMessageWithMoveInfo("Moved"));
            resetFields();
            summary.showSuccessMessage();
        } catch (MoveDatasetException mde) {
            logger.log(Level.WARNING, createMessageWithMoveInfo("Unable to move"), mde);
            summary.addParameter(mde)
                    .addParameter(createForceInfoIfApplicable(mde))
                    .showFailureMessage();
        } catch (CommandException ce) {
            logger.log(Level.WARNING, createMessageWithMoveInfo("Unable to move"), ce);
            JsfHelper.addErrorMessage(null,
                    getStringFromBundle("dashboard.datamove.message.failure.summary"), StringUtils.EMPTY);
        }
    }


    // -------------------- PRIVATE --------------------
    private String extractSourcePersistentId() {
        return Optional.ofNullable(sourceDataset)
                .map(Dataset::getGlobalId)
                .map(GlobalId::asString)
                .orElse(StringUtils.EMPTY);
    }

    private String extractSourceAlias() {
        return Optional.ofNullable(sourceDataset)
                .map(Dataset::getOwner)
                .map(Dataverse::getAlias)
                .orElse(StringUtils.EMPTY);
    }

    private String extractTargetAlias() {
        return Optional.ofNullable(targetDataverse)
                .map(Dataverse::getAlias)
                .orElse(StringUtils.EMPTY);
    }

    private String createMessageWithMoveInfo(String message) {
        return message + " " +
                extractSourcePersistentId() +
                " from " + extractSourceAlias() +
                " to " + extractTargetAlias();
    }

    private String createForceInfoIfApplicable(MoveDatasetException mde) {
        return isForcingPossible(mde)
                ? getStringFromBundle("dashboard.datamove.dataset.command.suggestForce",
                settings.getGuidesBaseUrl(), settings.getGuidesVersion())
                : StringUtils.EMPTY;
    }

    private boolean isForcingPossible(MoveDatasetException mde) {
        return mde.getDetails().stream()
                .allMatch(AdditionalStatus::isPassByForcePossible);
    }

    private void resetFields() {
        sourceDataset = null;
        targetDataverse = null;
    }

    // -------------------- SETTERS --------------------

    public void setForceMove(boolean forceMove) {
        this.forceMove = forceMove;
    }

    public void setSourceDataset(Dataset sourceDataset) {
        this.sourceDataset = sourceDataset;
    }

    public void setTargetDataverse(Dataverse targetDataverse) {
        this.targetDataverse = targetDataverse;
    }

    // -------------------- INNER CLASSES ---------------------

    private static class Summary {
        private final List<String> summaryParameters = new ArrayList<>();

        public Summary addParameter(String param) {
            summaryParameters.add(param != null ? param : StringUtils.EMPTY);
            return this;
        }

        public Summary addParameter(MoveDatasetException mde) {
            summaryParameters.add(mde.getDetails().stream()
                    .map(AdditionalStatus::getMessageKey)
                    .map(BundleUtil::getStringFromBundle)
                    .collect(Collectors.joining(" ")));
            return this;
        }

        public void showSuccessMessage() {
            JsfHelper.addFlashSuccessMessage(getStringFromBundle("dashboard.datamove.message.success", summaryParameters));
        }

        public void showFailureMessage() {
            JsfHelper.addErrorMessage(null, getStringFromBundle("dashboard.datamove.message.failure.summary"),
                    getStringFromBundle("dashboard.datamove.message.failure.details", summaryParameters));
        }
    }
}
