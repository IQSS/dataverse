package edu.harvard.iq.dataverse.authorization.groups.impl.mail;

import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;
import edu.harvard.iq.dataverse.persistence.group.MailDomainGroupRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignmentRepository;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Stateless
public class MailDomainGroupService {

    private MailDomainGroupRepository mailGroupRepository;

    private RoleAssignmentRepository roleAssignmentRepository;

    private MailDomainCachedMatcherBean matcher;

    private ConfirmEmailServiceBean confirmEmailService;

    private ActionLogServiceBean actionLogService;

    // -------------------- CONSTRUCTORS --------------------

    public MailDomainGroupService() { }

    @Inject
    public MailDomainGroupService(
            MailDomainGroupRepository mailGroupRepository,
            RoleAssignmentRepository roleAssignmentRepository,
            MailDomainCachedMatcherBean matcher,
            ConfirmEmailServiceBean confirmEmailService,
            ActionLogServiceBean actionLogService) {
        this.mailGroupRepository = mailGroupRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.matcher = matcher;
        this.confirmEmailService = confirmEmailService;
        this.actionLogService = actionLogService;
    }

    // -------------------- LOGIC --------------------

    @PostConstruct
    void rebuildMatcher() {
        matcher.rebuildIndex(mailGroupRepository.findAll());
    }

    public List<MailDomainGroup> getAllGroups() {
        return mailGroupRepository.findAll();
    }

    public Optional<MailDomainGroup> getGroup(String alias) {
        return mailGroupRepository.findByAlias(alias);
    }

    public void saveOrUpdateGroup(MailDomainGroup group) {
        mailGroupRepository.findByAlias(group.getPersistedGroupAlias())
                .ifPresent(f -> group.setId(f.getId()));
        mailGroupRepository.saveAndFlush(group);
        rebuildMatcher();
        logAction("add/update mail domain group", group.getPersistedGroupAlias());
    }

    public Optional<MailDomainGroup> deleteGroup(String alias) {
        return mailGroupRepository.findByAlias(alias)
                .map(f -> {
                    mailGroupRepository.mergeAndDelete(f);
                    roleAssignmentRepository.deleteAllByAssigneeIdentifier(f.getIdentifier());
                    rebuildMatcher();
                    logAction("delete mail domain group", alias);
                    return f;
                });
    }

    public Set<MailDomainGroup> getGroupsForUser(AuthenticatedUser user) {
        if (!confirmEmailService.hasVerifiedEmail(user)) {
            return Collections.emptySet();
        } else {
            String email = Optional.ofNullable(user.getEmail()).orElse(StringUtils.EMPTY);
            int positionAfterAt = email.indexOf("@") + 1;
            return positionAfterAt > 0 && positionAfterAt < email.length()
                    ? matcher.matchGroupsForDomain(email.substring(positionAfterAt))
                    : Collections.emptySet();
        }
    }

    // -------------------- PRIVATE --------------------

    private void logAction(String actionSubtype, String info) {
        ActionLogRecord logRecord = new ActionLogRecord(ActionLogRecord.ActionType.GlobalGroups, actionSubtype);
        logRecord.setInfo(info);
        actionLogService.log(logRecord);
    }
}
