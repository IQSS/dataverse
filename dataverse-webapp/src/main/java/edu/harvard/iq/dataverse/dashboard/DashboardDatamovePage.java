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
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
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

    @Inject
    private SystemConfig systemConfig;

    private boolean forceMove = false;

    // Following two are for dataset move
    private List<Dataset> sourceDatasets;

    private Dataverse targetDataverse;

    // And following two for dataverse move

    private Dataverse sourceDataverse;

    // -------------------- GETTERS --------------------

    public boolean isForceMove() {
        return forceMove;
    }

    public List<Dataset> getSourceDatasets() {
        return sourceDatasets;
    }

    public Dataverse getTargetDataverse() {
        return targetDataverse;
    }

    public Dataverse getSourceDataverse() {
        return sourceDataverse;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        User user = session.getUser();
        if (!user.isSuperuser() || systemConfig.isReadonlyMode()) {
            return permissionsWrapper.notAuthorized();
        }
        sourceDatasets = new ArrayList<>();
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
        if (sourceDatasets == null || sourceDatasets.isEmpty() || targetDataverse == null) {
            // We should never get here, but in case of some unexpected failure we should be prepared nevertheless
            JsfHelper.addErrorMessage(getStringFromBundle("dashboard.datamove.empty.fields"));
            return;
        }

        List<String> successfulIds = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();
        for (Dataset source : sourceDatasets) {
            Summary summary = new Summary(Summary.Mode.DATASET);
            try {
                summary.addParameter(source.getDisplayName())
                        .addParameter(extractSourcePersistentId(source))
                        .addParameter(targetDataverse.getDisplayName());

                DataverseRequest dataverseRequest = requestService.getDataverseRequest();
                String previousSourceAlias = extractSourceAlias(source);
                commandEngine.submit(new MoveDatasetCommand(dataverseRequest, source, targetDataverse, forceMove));
                logger.info(createMessageWithDatasetMoveInfo(source, "Moved", previousSourceAlias));
                successfulIds.add(extractSourcePersistentId(source));
            } catch (MoveException me) {
                logger.log(Level.WARNING, createMessageWithDatasetMoveInfo(source, "Unable to move"), me);
                summary.addParameter(me).addParameter(createForceInfoIfApplicable(me));
                failureMessages.add(summary.getFailureMessageDetail());
            } catch (CommandException ce) {
                logger.log(Level.WARNING, createMessageWithDatasetMoveInfo(source, "Unable to move"), ce);
                summary.addParameter(getStringFromBundle("dashboard.datamove.dataset.message.failure.summary"));
                failureMessages.add(summary.getFailureMessageDetail());
            }
        }

        showDatasetsMovedMessage(successfulIds, failureMessages, targetDataverse.getDisplayName());
    }

    public void moveDataverse() {
        if (sourceDataverse == null || targetDataverse == null) {
            JsfHelper.addErrorMessage(getStringFromBundle("dashboard.datamove.empty.fields"));
            return;
        }

        Summary summary = new Summary(Summary.Mode.DATAVERSE)
                .addParameter(extractDataverseAlias(sourceDataverse))
                .addParameter(extractDataverseAlias(targetDataverse));

        try {
            DataverseRequest dataverseRequest = requestService.getDataverseRequest();
            commandEngine.submit(new MoveDataverseCommand(dataverseRequest, sourceDataverse, targetDataverse, forceMove));
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
            JsfHelper.addErrorMessage(getStringFromBundle("dashboard.datamove.dataverse.message.failure.summary"), StringUtils.EMPTY);
        }
    }

    public String getMessageDetails() {
        return getStringFromBundle("dashboard.datamove.message.details", settings.getGuidesBaseUrl(), settings.getGuidesVersion());
    }

    // -------------------- PRIVATE --------------------

    private static void showDatasetsMovedMessage(List<String> successfulIds, List<String> failureMessages, String dataverseName) {
        StringBuilder sb = new StringBuilder();

        if (!successfulIds.isEmpty()) {
            sb.append(getStringFromBundle("dashboard.datamove.dataset.message.success.multiple",
                            StringUtils.join(successfulIds, ", "), dataverseName));
        }

        if (!failureMessages.isEmpty()) {
            if (!successfulIds.isEmpty()) {
                sb.append("<hr>");
            }
            sb.append(String.join("<hr>", failureMessages));
        }

        JsfHelper.addInfoMessage(sb.toString().replaceAll("<br><br>", "<br>"));
    }

    private static String extractSourcePersistentId(Dataset source) {
        return Optional.ofNullable(source)
                .map(Dataset::getGlobalId)
                .map(GlobalId::asString)
                .orElse(StringUtils.EMPTY);
    }

    private static String extractSourceAlias(Dataset source) {
        return Optional.ofNullable(source)
                .map(Dataset::getOwner)
                .map(Dataverse::getAlias)
                .orElse(StringUtils.EMPTY);
    }

    private static String extractDataverseAlias(Dataverse dataverse) {
        return Optional.ofNullable(dataverse)
                .map(Dataverse::getAlias)
                .orElse(StringUtils.EMPTY);
    }

    private String createMessageWithDatasetMoveInfo(Dataset sourceDs, String message, String source) {
        return String.format("%s %s from %s to %s",
                message, extractSourcePersistentId(sourceDs), source, extractDataverseAlias(targetDataverse));
    }

    private String createMessageWithDatasetMoveInfo(Dataset sourceDs, String message) {
        return createMessageWithDatasetMoveInfo(sourceDs, message, extractSourceAlias(sourceDs));
    }

    private String createMessageWithDataverseMoveInfo(String message) {
        return String.format("%s %s to %s",
                message, extractDataverseAlias(sourceDataverse), extractDataverseAlias(targetDataverse));
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
        sourceDatasets = new ArrayList<>();
        targetDataverse = null;
    }

    private void resetDataverseMoveFields() {
        sourceDataverse = null;
        targetDataverse = null;
    }

    // -------------------- SETTERS --------------------

    public void setForceMove(boolean forceMove) {
        this.forceMove = forceMove;
    }

    public void setSourceDatasets(List<Dataset> sourceDatasets) {
        this.sourceDatasets = sourceDatasets;
    }

    public void setTargetDataverse(Dataverse targetDataverse) {
        this.targetDataverse = targetDataverse;
    }

    public void setSourceDataverse(Dataverse sourceDataverse) {
        this.sourceDataverse = sourceDataverse;
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
            JsfHelper.addFlashSuccessMessage(getStringFromBundle(buildKey("message.success"), summaryParameters.toArray()));
        }

        public String getFailureMessageDetail() {
            return getStringFromBundle(buildKey("message.failure.details"), summaryParameters.toArray());
        }

        public void showFailureMessage() {
            JsfHelper.addErrorMessage(
                    getStringFromBundle(buildKey("message.failure.summary")),
                    getFailureMessageDetail());
        }

        // -------------------- PRIVATE --------------------

        private String buildKey(String postfix) {
            return mode.getKey() + '.' + postfix;
        }
    }
}
