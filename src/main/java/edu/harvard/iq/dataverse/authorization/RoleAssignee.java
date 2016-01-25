package edu.harvard.iq.dataverse.authorization;

import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsers;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;

/**
 * An entity that can be assigned roles to. In effect, this is either a 
 * {@link User} or a {@link Group}, both of which have numerous subclasses.
 * Note that some {@code RoleAssignee}s, such as {@link GuestUser} and {@link AllUsers}, don't even live in the database.
 * 
 * @author michael
 */
public interface RoleAssignee {

    /**
     * A unique identifier of the role assignee within the installation. This 
     * string is composed of a type prefix (@ for users, & for groups) and then
     * a unique suffix within the type.
     * 
     * @return the unique identifier of the role assignee within the system. 
     */
    public String getIdentifier();

    public RoleAssigneeDisplayInfo getDisplayInfo();

    static Function<RoleAssignee, Predicate<String>> autocompleteMatch = new Function<RoleAssignee, Predicate<String>>() {
		@Override
		public Predicate<String> apply(RoleAssignee ra) {
			return new Predicate<String>() {
				@Override
				public boolean test(String query) {
					return 	ra != null &&
							((ra.getDisplayInfo() != null && StringUtils.containsIgnoreCase(ra.getDisplayInfo().getTitle(), query) )
		                    || StringUtils.containsIgnoreCase(ra.getIdentifier(), query));
				}
			};
		}
	};
}
