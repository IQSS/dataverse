package edu.harvard.iq.dataverse.persistence.group;

import javax.ejb.ApplicationException;

/**
 * When the groups library throws an exception, it has to be a subclass of this guy.
 *
 * @author michael
 */
@ApplicationException(rollback = true)
public class GroupException extends RuntimeException {

    private final Group theGroup;

    public GroupException(Group aGroup, String message) {
        this(aGroup, message, null);
    }

    public GroupException(Group aGroup, String message, Throwable cause) {
        super(message, cause);
        theGroup = aGroup;
    }

    public Group getGroup() {
        return theGroup;
    }

    @Override
    public String toString() {
        return super.toString() + "[ Group: " + getGroup() + "]";
    }
}
