package edu.harvard.iq.dataverse.engine.command.exception.move;

public interface AdditionalMoveStatus {

    String getMessageKey();

    boolean isPassByForcePossible();
}
