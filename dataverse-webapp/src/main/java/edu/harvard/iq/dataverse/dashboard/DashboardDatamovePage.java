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
import edu.harvard.iq.dataverse.engine.command.exception.move.AdditionalMoveStatus;
import edu.harvard.iq.dataverse.engine.command.exception.move.MoveException;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDataverseCommand;
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

    // Following two are for dataset move
    private Dataset sourceDataset;

    private Dataverse targetDataverse;

    // And following two for dataverse move

    private Dataverse source;

    private Dataverse target;

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

    public Dataverse getSource() {
        return source;
    }

    public Dataverse getTarget() {
        return target;
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

    public List<Dataverse> completeDataverse(String query) {
        return dataverseDao.filterByAliasQuery(query);
    }

    public void moveDataset() {
        if (sourceDataset == null || targetDataverse == null) {
            // We should never get here, but in case of some unexpected failure we should be prepared nevertheless
            JsfHelper.addFlashErrorMessage(getStringFromBundle("dashboard.datamove.empty.fields"));
            return;
        }

        Summary summary = new Summary(Summary.Mode.DATASET)
                .addParameter(sourceDataset.getDisplayName())
                .addParameter(extractSourcePersistentId())
                .addParameter(targetDataverse.getName());

        try {
            DataverseRequest dataverseRequest = requestService.getDataverseRequest();
            commandEngine.submit(new MoveDatasetCommand(dataverseRequest, sourceDataset, targetDataverse, forceMove));
            logger.info(createMessageWithDatasetMoveInfo("Moved"));
            resetDatasetMoveFields();
            summary.showSuccessMessage();
        } catch (MoveException me) {
            logger.log(Level.WARNING, createMessageWithDatasetMoveInfo("Unable to move"), me);
            summary.addParameter(me)
                    .addParameter(createForceInfoIfApplicable(me))
                    .showFailureMessage();
        } catch (CommandException ce) {
            logger.log(Level.WARNING, createMessageWithDatasetMoveInfo("Unable to move"), ce);
            JsfHelper.addErrorMessage(null,
                    getStringFromBundle("dashboard.datamove.dataset.message.failure.summary"), StringUtils.EMPTY);
        }
    }

    public void moveDataverse() {
        if (source == null || target == null) {
            JsfHelper.addFlashErrorMessage(getStringFromBundle("dashboard.datamove.empty.fields"));
            return;
        }

        Summary summary = new Summary(Summary.Mode.DATAVERSE)
                .addParameter(extractDataverseAlias(source))
                .addParameter(extractDataverseAlias(target));

        try {
            DataverseRequest dataverseRequest = requestService.getDataverseRequest();
            commandEngine.submit(new MoveDataverseCommand(dataverseRequest, source, target, forceMove));
            logger.info(createMessageWithDataverseMoveInfo("Moved"));
            resetDataverseMoveFields();
            summary.showSuccessMessage();
        } catch (MoveException me) {
            logger.log(Level.WARNING, createMessageWithDataverseMoveInfo("Unable to move"), me);
            summary.addParameter(me)
                    .addParameter(createForceInfoIfApplicable(me))
                    .showFailureMessage();
        } catch (CommandException ce) {
            logger.log(Level.WARNING, createMessageWithDataverseMoveInfo("Unable to move"), ce);
            JsfHelper.addErrorMessage(null,
                    getStringFromBundle("dashboard.datamove.dataverse.message.failure.summary"), StringUtils.EMPTY);
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

    private String extractDataverseAlias(Dataverse dataverse) {
        return Optional.ofNullable(dataverse)
                .map(Dataverse::getAlias)
                .orElse(StringUtils.EMPTY);
    }

    private String createMessageWithDatasetMoveInfo(String message) {
        return message + " " +
                extractSourcePersistentId() +
                " from " + extractSourceAlias() +
                " to " + extractDataverseAlias(targetDataverse);
    }

    private String createMessageWithDataverseMoveInfo(String message) {
        return message + " " +
                extractDataverseAlias(source) +
                " to " + extractDataverseAlias(target);
    }

    private String createForceInfoIfApplicable(MoveException mde) {
        return isForcingPossible(mde)
                ? getStringFromBundle("dashboard.datamove.command.suggestForce",
                settings.getGuidesBaseUrl(), settings.getGuidesVersion())
                : StringUtils.EMPTY;
    }

    private boolean isForcingPossible(MoveException mde) {
        return mde.getDetails().stream()
                .allMatch(AdditionalMoveStatus::isPassByForcePossible);
    }

    private void resetDatasetMoveFields() {
        sourceDataset = null;
        targetDataverse = null;
    }

    private void resetDataverseMoveFields() {
        source = null;
        target = null;
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

    public void setSource(Dataverse source) {
        this.source = source;
    }

    public void setTarget(Dataverse target) {
        this.target = target;
    }

    // -------------------- INNER CLASSES ---------------------

    private static class Summary {
        public enum Mode {
            DATAVERSE("dashboard.datamove.dataverse"),
            DATASET("dashboard.datamove.dataset");

            private String key;

            Mode(String key) {
                this.key = key;
            }

            public String getKey() {
                return key;
            }
        }

        private Mode mode;

        private final List<String> summaryParameters = new ArrayList<>();

        // -------------------- CONSTRUCTORS --------------------

        public Summary(Mode mode) {
            this.mode = mode;
        }

        // -------------------- LOGIC --------------------

        public Summary addParameter(String param) {
            summaryParameters.add(param != null ? param : StringUtils.EMPTY);
            return this;
        }

        public Summary addParameter(MoveException mde) {
            summaryParameters.add(mde.getDetails().stream()
                    .map(AdditionalMoveStatus::getMessageKey)
                    .map(BundleUtil::getStringFromBundle)
                    .collect(Collectors.joining(" ")));
            return this;
        }

        public void showSuccessMessage() {
            JsfHelper.addFlashSuccessMessage(getStringFromBundle(buildKey("message.success"), summaryParameters));
        }

        public void showFailureMessage() {
            JsfHelper.addErrorMessage(null, getStringFromBundle(buildKey("message.failure.summary")),
                    getStringFromBundle(buildKey("message.failure.details"), summaryParameters));
        }

        // -------------------- PRIVATE --------------------

        private String buildKey(String postfix) {
            return mode.getKey() + '.' + postfix;
        }
    }
}
