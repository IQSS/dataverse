package edu.harvard.iq.dataverse.engine.command.exception.move;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

import java.util.ArrayList;
import java.util.List;

public class MoveException extends IllegalCommandException {


    private List<AdditionalMoveStatus> details = new ArrayList<>();

    public MoveException(String message, Command aCommand, AdditionalMoveStatus detail) {
        super(message, aCommand);
        this.details.add(detail);
    }

    public MoveException(String message, Command aCommand, List<AdditionalMoveStatus> details) {
        super(message, aCommand);
        this.details.addAll(details);
    }

    public List<AdditionalMoveStatus> getDetails() {
        return details;
    }
}
