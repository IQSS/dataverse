package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.Set;
import static java.util.stream.Collectors.joining;

public class GroupUtil {

    /**
     * @param authenticatedUser An non-null AuthenticatedUser.
     * @param groups The groups associated with an AuthenticatedUser.
     * @return A list of identifiers for the user including groups, single
     * quoted, and separated by commas. Null if a null AuthenticatedUser is
     * passed.
     */
    public static String getAllIdentifiersForUser(AuthenticatedUser authenticatedUser, Set<Group> groups) {
        if (authenticatedUser == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(authenticatedUser.getIdentifier()).append("'");
        if (groups != null) {
            sb.append(groups.stream().map(Group::getIdentifier).collect(joining("','", ", '", "'")));
        }
        return sb.toString();
    }

}
