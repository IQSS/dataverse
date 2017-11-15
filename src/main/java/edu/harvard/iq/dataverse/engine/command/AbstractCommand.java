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

    private final Map<String, DvObject> affectedDvObjects;
    private final DataverseRequest request;

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

    public AbstractCommand(DataverseRequest aRequest, DvObject anAffectedDvObject) {
        this(aRequest, dv("", anAffectedDvObject));
    }

    public AbstractCommand(DataverseRequest aRequest, DvNamePair dvp, DvNamePair... more) {
        request = aRequest;
        affectedDvObjects = new HashMap<>();
        affectedDvObjects.put(dvp.name, dvp.dvObject);
        for (DvNamePair p : more) {
            affectedDvObjects.put(p.name, p.dvObject);
        }
    }

    public AbstractCommand(DataverseRequest aRequest, Map<String, DvObject> someAffectedDvObjects) {
        request = aRequest;
        affectedDvObjects = someAffectedDvObjects;
    }
    
    @Override
    public Map<String, DvObject> getAffectedDvObjects() {
        return affectedDvObjects;
    }

    @Override
    public DataverseRequest getRequest() {
        return request;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return CH.permissionsRequired(getClass());
    }

    /**
     * Convenience method for getting the user requesting this command.
     * @return the user issuing the command (via the {@link DataverseRequest}).
     */
    protected User getUser() {
       return getRequest().getUser();
    }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, DvObject> ent : affectedDvObjects.entrySet()) {
            DvObject value = ent.getValue();
            sb.append(ent.getKey()).append(":");
            sb.append((value != null) ? value.accept(DvObject.NameIdPrinter) : "<null>");
            sb.append(" ");
        }
        return sb.toString();
    }

}
