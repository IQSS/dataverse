package edu.harvard.iq.dataverse.engine.command.exception.move;

public enum DataverseMoveStatus implements AdditionalMoveStatus {
    TRYING_TO_MOVE_INTO_DESCENDANT_DATAVERSE("dahsboard.datamove.dataverse.command.error.trying.to.move.into.descendant.dataverse"),
    TRYING_TO_MOVE_INTO_ITSELF("dahsboard.datamove.dataverse.command.error.trying.to.move.into.itself"),
    ALREADY_IN_DATAVERSE("dahsboard.datamove.dataverse.command.error.already.in.dataverse"),
    UNPUBLISHED_TARGET_DATAVERSE("dahsboard.datamove.dataverse.command.error.unpublished.target.dataverse"),
    DATASET_GUESTBOOK_NOT_IN_TARGET_DATAVERSE("dahsboard.datamove.dataverse.command.error.dataset.guestbook.not.in.target.dataverse", true),
    DATAVERSE_TEMPLATE_NOT_IN_TARGET_DATAVERSE("dahsboard.datamove.dataverse.command.error.dataverse.template.not.in.target.dataverse", true),
    DATAVERSE_FEATURED_IN_CURRENT_DATAVERSE("dahsboard.datamove.dataverse.command.error.dataverse.featured.in.current.dataverse", true),
    DATAVERSE_METADATA_BLOCK_NOT_IN_TARGET_DATAVERSE("dahsboard.datamove.dataverse.command.error.dataverse.metadata.block.not.in.target.dataverse", true),
    DATAVERSE_LINKED_TO_TARGET_DATAVERSE("dahsboard.datamove.dataverse.command.error.dataverse.linked.to.target.dataverse", true),
    DATASET_LINKED_TO_TARGET_DATAVERSE("dahsboard.datamove.dataverse.command.error.dataset.linked.to.target.dataverse", true)
    ;

    private String messageKey;

    private boolean passByForcePossible;

    DataverseMoveStatus(String messageKey, boolean passByForcePossible) {
        this.messageKey = messageKey;
        this.passByForcePossible = passByForcePossible;
    }

    DataverseMoveStatus(String messageKey) {
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
