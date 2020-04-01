package edu.harvard.iq.dataverse.engine.command.exception.move;

public enum DatasetMoveStatus implements AdditionalMoveStatus {
        ALREADY_IN_TARGET_DATAVERSE("dashboard.datamove.dataset.command.error.targetDataverseSameAsOriginalDataverse"),
        UNPUBLISHED_TARGET_DATAVERSE("dashboard.datamove.dataset.command.error.targetDataverseUnpublishedDatasetPublished"),
        NO_GUESTBOOK_IN_TARGET_DATAVERSE("dashboard.datamove.dataset.command.error.datasetGuestbookNotInTargetDataverse", true),
        LINKED_TO_TARGET_DATAVERSE("dashboard.datamove.dataset.command.error.linkedToTargetDataverseOrOneOfItsParents", true),
        INDEXING_ISSUE("dashboard.datamove.dataset.command.error.indexingProblem")
        ;

        private final String messageKey;

        private final boolean passByForcePossible;

        DatasetMoveStatus(String messageKey, boolean passByForcePossible) {
            this.messageKey = messageKey;
            this.passByForcePossible = passByForcePossible;
        }

        DatasetMoveStatus(String messageKey) {
            this(messageKey, false);
        }

        @Override
        public String getMessageKey() {
            return messageKey;
        }

        @Override
        public boolean isPassByForcePossible() {
            return passByForcePossible;
        }
}
