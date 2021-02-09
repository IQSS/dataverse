package edu.harvard.iq.dataverse.authorization.groups.impl.mail;

import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;
import edu.harvard.iq.dataverse.persistence.group.MailDomainItem;
import edu.harvard.iq.dataverse.persistence.group.MailDomainProcessingType;

import java.util.Set;

class MailDomainGroupTestUtil {

    // -------------------- CONSTRUCTORS --------------------

    private MailDomainGroupTestUtil() { }

    // -------------------- LOGIC --------------------

    public static MailDomainGroup createGroup(String alias, String[] inclusions, String[] exclusions) {
        MailDomainGroup group = new MailDomainGroup();
        group.setPersistedGroupAlias(alias);
        Set<MailDomainItem> domainItems = group.getDomainItems();
        for (String inclusion : inclusions) {
            domainItems.add(new MailDomainItem(inclusion, MailDomainProcessingType.INCLUDE, group));
        }
        for (String exclusion : exclusions) {
            domainItems.add(new MailDomainItem(exclusion, MailDomainProcessingType.EXCLUDE, group));
        }
        return group;
    }
}
