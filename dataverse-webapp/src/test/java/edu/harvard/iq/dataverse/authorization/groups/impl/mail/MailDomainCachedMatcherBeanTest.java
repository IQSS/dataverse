package edu.harvard.iq.dataverse.authorization.groups.impl.mail;

import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.harvard.iq.dataverse.authorization.groups.impl.mail.MailDomainGroupTestUtil.createGroup;
import static org.assertj.core.api.Assertions.assertThat;

class MailDomainCachedMatcherBeanTest {

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should rebuild internal index")
    void rebuildIndex() {

        // given
        MailDomainCachedMatcherBean matcher = new MailDomainCachedMatcherBean();

        // when
        Set<MailDomainGroup> found = matcher.matchGroupsForDomain("edu.pl");

        // then
        assertThat(found).isEmpty();

        // when
        MailDomainGroup mailDomainGroup = createGroup("test", new String[] {"edu.pl"}, new String[0]);
        matcher.rebuildIndex(Collections.singleton(mailDomainGroup));
        found = matcher.matchGroupsForDomain("edu.pl");

        // then
        assertThat(found).containsExactly(mailDomainGroup);
    }

    @Test
    @DisplayName("Should find all groups matching fully given domain")
    void matchGroupsForDomain__fullDomainMatch() {

        // given
        MailDomainCachedMatcherBean matcher = new MailDomainCachedMatcherBean();
        matcher.rebuildIndex(
                Stream.of(
                        createGroup("1", new String[] {"icm.edu.pl"}, new String[0]),
                        createGroup("2", new String[] {"icm.edu.pl"}, new String[0]),
                        createGroup("3", new String[] {"icm.edu.pl"}, new String[0]))
                        .collect(Collectors.toSet()));

        // when
        Set<MailDomainGroup> found = matcher.matchGroupsForDomain("icm.edu.pl");

        // then
        assertThat(found)
                .extracting(MailDomainGroup::getPersistedGroupAlias)
                .containsExactlyInAnyOrder("1", "2", "3");
    }

    @Test
    @DisplayName("Should find all groups matching partially given domain")
    void matchGroupsForDomain__partialDomainMatch() {

        // given
        MailDomainCachedMatcherBean matcher = new MailDomainCachedMatcherBean();
        matcher.rebuildIndex(
                Stream.of(
                        createGroup("1", new String[] {".edu.pl"}, new String[0]),
                        createGroup("2", new String[] {".pl"}, new String[0]),
                        createGroup("3", new String[] {".icm.edu.pl"}, new String[0]))
                        .collect(Collectors.toSet()));

        // when
        Set<MailDomainGroup> found = matcher.matchGroupsForDomain("uw.icm.edu.pl");

        // then
        assertThat(found)
                .extracting(MailDomainGroup::getPersistedGroupAlias)
                .containsExactlyInAnyOrder("1", "2", "3");
    }

    @Test
    @DisplayName("Should find all groups matching fully or partially given domain with full or partial exclusions")
    void matchGroupsForDomain__variousConditions() {

        // given
        MailDomainCachedMatcherBean matcher = new MailDomainCachedMatcherBean();
        matcher.rebuildIndex(
                Stream.of(
                        createGroup("1", new String[] {"uw.icm.edu.pl"}, new String[0]),
                        createGroup("2", new String[] {".icm.edu.pl"}, new String[0]),
                        createGroup("3", new String[] {".pl"}, new String[] {".icm.edu.pl"}),
                        createGroup("4", new String[] {".edu.pl"}, new String[] {"uw.icm.edu.pl"}),
                        createGroup("5", new String[] {".icm.edu.pl"}, new String[] {".pl"}))
                        .collect(Collectors.toSet()));

        // when
        Set<MailDomainGroup> found = matcher.matchGroupsForDomain("uw.icm.edu.pl");

        // then
        assertThat(found)
                .extracting(MailDomainGroup::getPersistedGroupAlias)
                .containsExactlyInAnyOrder("1", "2");
    }
}