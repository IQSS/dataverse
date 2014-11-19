package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import static edu.harvard.iq.dataverse.engine.command.CommandHelper.CH;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Convenience class for implementing the {@link Command} interface.
 *
 * @author michael
 * @param <R> The result type of the command.
 */
public abstract class AbstractCommand<R> implements Command<R> {

    private final Map<String, DvObject> affectedDataverses;
    private final User user;

    static protected class DvNamePair {

        final String name;
        final DvObject dvObject;

        public DvNamePair(String name, DvObject dv) {
            this.name = name;
            this.dvObject = dv;
        }
    }

    /**
     * Convenience method to name affected dataverses.
     *
     * @param s the name
     * @param d the dataverse
     * @return the named pair
     */
    protected static DvNamePair dv(String s, DvObject d) {
        return new DvNamePair(s, d);
    }

    public AbstractCommand(User aUser, DvObject anAffectedDvObject) {
        this(aUser, dv("", anAffectedDvObject));
    }

    public AbstractCommand(User aUser, DvNamePair dvp, DvNamePair... more) {
        user = aUser;
        affectedDataverses = new HashMap<>();
        affectedDataverses.put(dvp.name, dvp.dvObject);
        for (DvNamePair p : more) {
            affectedDataverses.put(p.name, p.dvObject);
        }
    }

    public AbstractCommand(User aUser, Map<String, DvObject> someAffectedDvObjects) {
        user = aUser;
        affectedDataverses = someAffectedDvObjects;
    }

    @Override
    public Map<String, DvObject> getAffectedDvObjects() {
        return affectedDataverses;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return CH.permissionsRequired(getClass());
    }

}
