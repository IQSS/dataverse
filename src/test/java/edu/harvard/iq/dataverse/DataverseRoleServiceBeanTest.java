package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean.RoleAssignmentHistoryConsolidatedEntry;
import edu.harvard.iq.dataverse.RoleAssignmentHistory;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataverseRoleServiceBeanTest {

    @InjectMocks
    private DataverseRoleServiceBean dataverseRoleServiceBean;

    @Mock
    private EntityManager em;

    private Method processRoleAssignmentEntriesMethod;

    @BeforeEach
    public void setUp() throws Exception {
        // Get the private method using reflection
        processRoleAssignmentEntriesMethod = DataverseRoleServiceBean.class.getDeclaredMethod(
                "processRoleAssignmentEntries", List.class, boolean.class);
        processRoleAssignmentEntriesMethod.setAccessible(true);
    }

    @Test
    public void testProcessRoleAssignmentEntries_WithoutCombining() throws Exception {
        // Create test data
        List<RoleAssignmentHistory> entries = new ArrayList<>();

        // Create two entries for the same role assignment (assign and revoke)
        RoleAssignmentHistory entry1 = createRoleAssignmentHistoryEntry(
                1L, "user1", "role1", 101L, "admin1",
                new Date(), RoleAssignmentHistory.ActionType.ASSIGN);

        RoleAssignmentHistory entry2 = createRoleAssignmentHistoryEntry(
                1L, "user1", "role1", 101L, "admin2",
                new Date(), RoleAssignmentHistory.ActionType.REVOKE);

        // Create another role assignment for a different user
        RoleAssignmentHistory entry3 = createRoleAssignmentHistoryEntry(
                2L, "user2", "role2", 102L, "admin1",
                new Date(), RoleAssignmentHistory.ActionType.ASSIGN);

        entries.add(entry1);
        entries.add(entry2);
        entries.add(entry3);

        // Call the private method
        @SuppressWarnings("unchecked")
        List<RoleAssignmentHistoryConsolidatedEntry> result = (List<RoleAssignmentHistoryConsolidatedEntry>) processRoleAssignmentEntriesMethod.invoke(
                dataverseRoleServiceBean, entries, false);

        // Verify results
        assertEquals(2, result.size(), "Should have 2 consolidated entries");

        // Find the entry for user1 - should always be second since it it revoked and the user2 role is not.
        RoleAssignmentHistoryConsolidatedEntry user1Entry = result.get(1);

        assertNotNull(user1Entry, "Should have an entry for user1");
        assertEquals("role1", user1Entry.getRoleName());
        assertEquals("admin1", user1Entry.getAssignedBy());
        assertEquals("admin2", user1Entry.getRevokedBy());
        assertNotNull(user1Entry.getAssignedAt());
        assertNotNull(user1Entry.getRevokedAt());
        assertEquals(1, user1Entry.getDefinitionPointIds().size());
        assertEquals(101L, user1Entry.getDefinitionPointIds().get(0));

        // Find the entry for user2 - always fist since it is not revoked
        RoleAssignmentHistoryConsolidatedEntry user2Entry = result.get(0);

        assertNotNull(user2Entry, "Should have an entry for user2");
        assertEquals("role2", user2Entry.getRoleName());
        assertEquals("admin1", user2Entry.getAssignedBy());
        assertNull(user2Entry.getRevokedBy());
        assertNotNull(user2Entry.getAssignedAt());
        assertNull(user2Entry.getRevokedAt());
        assertEquals(1, user2Entry.getDefinitionPointIds().size());
        assertEquals(102L, user2Entry.getDefinitionPointIds().get(0));
    }

    @Test
    public void testProcessRoleAssignmentEntries_WithCombining() throws Exception {
        // Create test data
        List<RoleAssignmentHistory> entries = new ArrayList<>();

        // Create entries for the same minute but different definition points
        Date baseTime = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(baseTime);

        // First role assignment
        RoleAssignmentHistory entry1 = createRoleAssignmentHistoryEntry(
                1L, "user1", "role1", 101L, "admin1",
                baseTime, RoleAssignmentHistory.ActionType.ASSIGN);

        // Second role assignment with same minute
        RoleAssignmentHistory entry2 = createRoleAssignmentHistoryEntry(
                2L, "user1", "role1", 102L, "admin1",
                baseTime, RoleAssignmentHistory.ActionType.ASSIGN);

        // Third role assignment with different minute
        cal.add(Calendar.MINUTE, 1);
        Date laterTime = cal.getTime();
        RoleAssignmentHistory entry3 = createRoleAssignmentHistoryEntry(
                3L, "user1", "role1", 103L, "admin1",
                laterTime, RoleAssignmentHistory.ActionType.ASSIGN);

        // Fourth role assignment with different role
        RoleAssignmentHistory entry4 = createRoleAssignmentHistoryEntry(
                4L, "user1", "role2", 104L, "admin1",
                baseTime, RoleAssignmentHistory.ActionType.ASSIGN);

        entries.add(entry1);
        entries.add(entry2);
        entries.add(entry3);
        entries.add(entry4);

        // Call the private method
        @SuppressWarnings("unchecked")
        List<RoleAssignmentHistoryConsolidatedEntry> result = (List<RoleAssignmentHistoryConsolidatedEntry>) processRoleAssignmentEntriesMethod.invoke(
                dataverseRoleServiceBean, entries, true);

        // Verify results
        assertEquals(3, result.size(), "Should have 3 consolidated entries");

        // Find the entry for user1 with role1 and baseTime
        RoleAssignmentHistoryConsolidatedEntry combinedEntry = result.stream()
                .filter(e -> e.getAssigneeIdentifier().equals("user1") &&
                        e.getRoleName().equals("role1") &&
                        e.getDefinitionPointIds().contains(101L))
                .findFirst()
                .orElse(null);

        assertNotNull(combinedEntry, "Should have a combined entry for user1 with role1");
        assertEquals(2, combinedEntry.getDefinitionPointIds().size(),
                "Combined entry should have 2 definition points");
        assertTrue(combinedEntry.getDefinitionPointIds().contains(101L),
                "Combined entry should contain definition point 101");
        assertTrue(combinedEntry.getDefinitionPointIds().contains(102L),
                "Combined entry should contain definition point 102");

        // Find the entry for user1 with role1 and laterTime
        RoleAssignmentHistoryConsolidatedEntry laterEntry = result.stream()
                .filter(e -> e.getAssigneeIdentifier().equals("user1") &&
                        e.getRoleName().equals("role1") &&
                        !e.getDefinitionPointIds().contains(101L))
                .findFirst()
                .orElse(null);

        assertNotNull(laterEntry, "Should have an entry for user1 with role1 at later time");
        assertEquals(1, laterEntry.getDefinitionPointIds().size());
        assertEquals(103L, laterEntry.getDefinitionPointIds().get(0));

        // Find the entry for user1 with role2
        RoleAssignmentHistoryConsolidatedEntry role2Entry = result.stream()
                .filter(e -> e.getAssigneeIdentifier().equals("user1") &&
                        e.getRoleName().equals("role2"))
                .findFirst()
                .orElse(null);

        assertNotNull(role2Entry, "Should have an entry for user1 with role2");
        assertEquals(1, role2Entry.getDefinitionPointIds().size());
        assertEquals(104L, role2Entry.getDefinitionPointIds().get(0));
    }

    @Test
    public void testProcessRoleAssignmentEntries_WithAssignAndRevokeAndCombining() throws Exception {
        // Create test data
        List<RoleAssignmentHistory> entries = new ArrayList<>();

        // Create base time
        Date baseTime = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(baseTime);

        // Create revoke time (5 minutes later)
        cal.add(Calendar.MINUTE, 5);
        Date revokeTime = cal.getTime();

        // First role assignment and revoke
        RoleAssignmentHistory entry1 = createRoleAssignmentHistoryEntry(
                1L, "user1", "role1", 101L, "admin1",
                baseTime, RoleAssignmentHistory.ActionType.ASSIGN);

        RoleAssignmentHistory entry2 = createRoleAssignmentHistoryEntry(
                1L, "user1", "role1", 101L, "admin2",
                revokeTime, RoleAssignmentHistory.ActionType.REVOKE);

        // Second role assignment and revoke (same times)
        RoleAssignmentHistory entry3 = createRoleAssignmentHistoryEntry(
                2L, "user1", "role1", 102L, "admin1",
                baseTime, RoleAssignmentHistory.ActionType.ASSIGN);

        RoleAssignmentHistory entry4 = createRoleAssignmentHistoryEntry(
                2L, "user1", "role1", 102L, "admin2",
                revokeTime, RoleAssignmentHistory.ActionType.REVOKE);

        // Third role assignment with different revoke time
        cal.add(Calendar.MINUTE, 1);
        Date laterRevokeTime = cal.getTime();

        RoleAssignmentHistory entry5 = createRoleAssignmentHistoryEntry(
                3L, "user1", "role1", 103L, "admin1",
                baseTime, RoleAssignmentHistory.ActionType.ASSIGN);

        RoleAssignmentHistory entry6 = createRoleAssignmentHistoryEntry(
                3L, "user1", "role1", 103L, "admin2",
                laterRevokeTime, RoleAssignmentHistory.ActionType.REVOKE);

        entries.add(entry1);
        entries.add(entry2);
        entries.add(entry3);
        entries.add(entry4);
        entries.add(entry5);
        entries.add(entry6);

        // Call the private method
        @SuppressWarnings("unchecked")
        List<RoleAssignmentHistoryConsolidatedEntry> result = (List<RoleAssignmentHistoryConsolidatedEntry>) processRoleAssignmentEntriesMethod.invoke(
                dataverseRoleServiceBean, entries, true);

        // Verify results
        assertEquals(2, result.size(), "Should have 2 consolidated entries");

        // Find the entry for user1 with role1 and baseTime - oldest == last
        RoleAssignmentHistoryConsolidatedEntry combinedEntry = result.get(1);

        assertNotNull(combinedEntry, "Should have a combined entry for user1 with role1");
        assertEquals(2, combinedEntry.getDefinitionPointIds().size(),
                "Combined entry should have 2 definition points");
        assertTrue(combinedEntry.getDefinitionPointIds().contains(101L),
                "Combined entry should contain definition point 101");
        assertTrue(combinedEntry.getDefinitionPointIds().contains(102L),
                "Combined entry should contain definition point 102");

        // Find the entry for user1 with role1 and laterTime
        RoleAssignmentHistoryConsolidatedEntry laterEntry = result.stream()
                .filter(e -> e.getAssigneeIdentifier().equals("user1") &&
                        e.getRoleName().equals("role1") &&
                        !e.getDefinitionPointIds().contains(101L))
                .findFirst()
                .orElse(null);

        assertNotNull(laterEntry, "Should have an entry for user1 with role1 at later time");
        assertEquals(1, laterEntry.getDefinitionPointIds().size());
        assertEquals(103L, laterEntry.getDefinitionPointIds().get(0));

    }

    private RoleAssignmentHistory createRoleAssignmentHistoryEntry(
            Long roleAssignmentId, String assigneeIdentifier, String roleAlias,
            Long definitionPointId, String actionByIdentifier,
            Date actionTimestamp, RoleAssignmentHistory.ActionType actionType) {

        // Create a DataverseRole
        DataverseRole role = new DataverseRole();
        role.setAlias(roleAlias);
        role.setId(1L); // Arbitrary ID for testing

        // Create a DvObject for definition point
        DvObject dvObject = new Dataverse();
        dvObject.setId(definitionPointId);

        // Create a RoleAssignee (using a mock since it's an interface)
        RoleAssignee assignee = mock(RoleAssignee.class);
        when(assignee.getIdentifier()).thenReturn(assigneeIdentifier);

        // Create the RoleAssignment
        RoleAssignment roleAssignment = new RoleAssignment(role, assignee, dvObject, null);
        // Set the ID using reflection since it's normally set by the persistence layer
        try {
            java.lang.reflect.Field idField = RoleAssignment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(roleAssignment, roleAssignmentId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set RoleAssignment ID", e);
        }

        // Create the RoleAssignmentHistory entry
        RoleAssignmentHistory history = new RoleAssignmentHistory();
        history.setRoleAssignmentId(roleAssignmentId);
        history.setAssigneeIdentifier(assigneeIdentifier);
        history.setRoleAlias(roleAlias);
        history.setDefinitionPointId(definitionPointId);
        history.setActionByIdentifier(actionByIdentifier);
        history.setActionTimestamp(actionTimestamp);
        history.setActionType(actionType);

        return history;
    }
}