package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the pure parsing of the Keycloak group claim. Everything past this point needs a
 * container, but this is where a silent mistake -- a stray slash, an unexpected depth --
 * would quietly hand out or withhold permissions.
 */
@LocalJvmSettings
class KeycloakGroupSyncServiceBeanTest {

    private final KeycloakGroupSyncServiceBean sut = new KeycloakGroupSyncServiceBean();

    @Nested
    @LocalJvmSettings
    class ParseTenantAssignments {

        @Test
        void readsRoleAndTenantFromAFullPath() {
            List<KeycloakGroupSyncServiceBean.TenantAssignment> assignments =
                    sut.parseTenantAssignments(List.of("/platica/tenant-users/tenant-1/admins"));

            assertEquals(1, assignments.size());
            assertEquals("platica/tenant-users/tenant-1", assignments.get(0).getTenantGroupPath());
            assertEquals(KeycloakGroupSyncServiceBean.TenantRole.ADMIN, assignments.get(0).getRole());
        }

        @Test
        void acceptsPathsWithAndWithoutALeadingSlash() {
            assertEquals(1, sut.parseTenantAssignments(List.of("platica/tenant-users/tenant-2/curators")).size());
            assertEquals(1, sut.parseTenantAssignments(List.of("/platica/tenant-users/tenant-2/curators")).size());
        }

        @Test
        void recognisesAllThreeTenantRoles() {
            List<KeycloakGroupSyncServiceBean.TenantAssignment> assignments = sut.parseTenantAssignments(List.of(
                    "/platica/tenant-users/tenant-1/admins",
                    "/platica/tenant-users/tenant-1/curators",
                    "/platica/tenant-users/tenant-1/users"));

            assertEquals(List.of(
                            KeycloakGroupSyncServiceBean.TenantRole.ADMIN,
                            KeycloakGroupSyncServiceBean.TenantRole.CURATOR,
                            KeycloakGroupSyncServiceBean.TenantRole.USER),
                    assignments.stream().map(KeycloakGroupSyncServiceBean.TenantAssignment::getRole).toList());
        }

        @Test
        void keepsTheTenantsApart() {
            List<KeycloakGroupSyncServiceBean.TenantAssignment> assignments = sut.parseTenantAssignments(List.of(
                    "/platica/tenant-users/tenant-1/admins",
                    "/platica/tenant-users/tenant-200/users"));

            assertEquals(List.of("platica/tenant-users/tenant-1", "platica/tenant-users/tenant-200"),
                    assignments.stream().map(KeycloakGroupSyncServiceBean.TenantAssignment::getTenantGroupPath).toList());
        }

        @Test
        void ignoresGroupsThatDoNotConcernDataverse() {
            assertTrue(sut.parseTenantAssignments(List.of(
                    "/platica/content-admins",
                    "/platica/admins",
                    "/opencdmp-app/whatever",
                    "/platica/tenant-users",                          // the tenants container itself
                    "/platica/tenant-users/tenant-1",                 // a tenant, but no role
                    "/platica/tenant-users/tenant-1/admins/extra",    // deeper than we model
                    "/platica/tenant-users/tenant-1/reviewers",       // role we do not know
                    "",
                    "   "
            )).isEmpty());
        }

        @Test
        void toleratesNullEntriesInTheClaim() {
            List<String> paths = new java.util.ArrayList<>();
            paths.add(null);
            paths.add("/platica/tenant-users/tenant-1/admins");

            assertEquals(1, sut.parseTenantAssignments(paths).size());
        }

        @Test
        @JvmSetting(key = JvmSettings.OIDC_SYNC_PARENT_GROUP, value = "otra-plataforma")
        @JvmSetting(key = JvmSettings.OIDC_SYNC_TENANTS_GROUP, value = "orgs")
        void honoursTheConfiguredGroupNames() {
            assertTrue(sut.parseTenantAssignments(List.of("/platica/tenant-users/tenant-1/admins")).isEmpty());
            assertEquals(1, sut.parseTenantAssignments(List.of("/otra-plataforma/orgs/tenant-1/admins")).size());
        }
    }

    @Nested
    @LocalJvmSettings
    class SuperuserGroup {

        @Test
        void detectsThePlatformWideAdminsGroup() {
            assertTrue(sut.holdsSuperuserGroup(List.of("/platica/admins")));
            assertTrue(sut.holdsSuperuserGroup(List.of("platica/admins")));
        }

        @Test
        void doesNotConfuseATenantAdminWithASuperuser() {
            assertFalse(sut.holdsSuperuserGroup(List.of("/platica/tenant-users/tenant-1/admins")));
        }

        @Test
        void doesNotMatchSimilarlyNamedGroups() {
            assertFalse(sut.holdsSuperuserGroup(List.of("/platica/content-admins")));
            assertFalse(sut.holdsSuperuserGroup(List.of("/platica/admins-emeritus")));
            assertFalse(sut.holdsSuperuserGroup(List.of()));
        }

        @Test
        @JvmSetting(key = JvmSettings.OIDC_SYNC_SUPERUSER_GROUP, value = "superadmins")
        void honoursTheConfiguredGroupName() {
            assertFalse(sut.holdsSuperuserGroup(List.of("/platica/admins")));
            assertTrue(sut.holdsSuperuserGroup(List.of("/platica/superadmins")));
        }
    }

    @Nested
    @LocalJvmSettings
    class RemovalSafetyLimit {

        @Test
        void allowsAFifthOfTheMembershipsToGoAtOnce() {
            assertEquals(20, sut.removalLimit(100));
            assertEquals(40, sut.removalLimit(200));
        }

        @Test
        void roundsUpSoTheLimitIsNeverZero() {
            assertEquals(5, sut.removalLimit(1));
            assertEquals(5, sut.removalLimit(0));
        }

        @Test
        void keepsAnAbsoluteFloorForSmallInstallations() {
            // 20% of 10 is 2, which would make routine cleanup impossible on a small install.
            assertEquals(5, sut.removalLimit(10));
        }

        @Test
        @JvmSetting(key = JvmSettings.OIDC_SYNC_MIN_REMOVALS, value = "0")
        @JvmSetting(key = JvmSettings.OIDC_SYNC_MAX_REMOVAL_RATIO, value = "0.5")
        void honoursTheConfiguredThresholds() {
            assertEquals(50, sut.removalLimit(100));
            assertEquals(0, sut.removalLimit(0));
        }

        @Test
        @JvmSetting(key = JvmSettings.OIDC_SYNC_MIN_REMOVALS, value = "1000000")
        void canBeSetHighEnoughToEffectivelyDisableTheGuard() {
            assertEquals(1000000, sut.removalLimit(100));
        }
    }

    @Nested
    @LocalJvmSettings
    class NormalisePath {

        @Test
        void stripsLeadingAndTrailingSlashes() {
            assertEquals("a/b/c", KeycloakAdminClient.normalisePath("/a/b/c"));
            assertEquals("a/b/c", KeycloakAdminClient.normalisePath("a/b/c/"));
            assertEquals("a/b/c", KeycloakAdminClient.normalisePath("  //a/b/c//  "));
        }
    }
}
