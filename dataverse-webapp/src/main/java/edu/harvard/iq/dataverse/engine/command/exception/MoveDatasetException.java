package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.command.Command;

import java.util.ArrayList;
import java.util.List;

public class MoveDatasetException extends IllegalCommandException {
    public enum AdditionalStatus {
        ALREADY_IN_TARGET_DATAVERSE("dashboard.datamove.dataset.command.error.targetDataverseSameAsOriginalDataverse"),
        UNPUBLISHED_TARGET_DATAVERSE("dashboard.datamove.dataset.command.error.targetDataverseUnpublishedDatasetPublished"),
        NO_GUESTBOOK_IN_TARGET_DATAVERSE("dashboard.datamove.dataset.command.error.datasetGuestbookNotInTargetDataverse", true),
        LINKED_TO_TARGET_DATAVERSE("dashboard.datamove.dataset.command.error.linkedToTargetDataverseOrOneOfItsParents", true),
        INDEXING_ISSUE("dashboard.datamove.dataset.command.error.indexingProblem")
        ;

        private final String messageKey;

        private final boolean passByForcePossible;

        AdditionalStatus(String messageKey, boolean passByForcePossible) {
            this.messageKey = messageKey;
            this.passByForcePossible = passByForcePossible;
        }

        AdditionalStatus(String messageKey) {
            this(messageKey, false);
        }

        public String getMessageKey() {
            return messageKey;
        }

        public boolean isPassByForcePossible() {
            return passByForcePossible;
        }
    }

    private List<AdditionalStatus> details = new ArrayList<>();

    public MoveDatasetException(String message, Command aCommand, AdditionalStatus detail) {
        super(message, aCommand);
        this.details.add(detail);
    }

    public MoveDatasetException(String message, Command aCommand, List<AdditionalStatus> details) {
        super(message, aCommand);
        this.details.addAll(details);
    }

    public List<AdditionalStatus> getDetails() {
        return details;
    }
}
