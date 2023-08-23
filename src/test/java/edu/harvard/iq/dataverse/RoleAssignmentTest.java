package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class RoleAssignmentTest {

    private RoleAssignment roleAssignment;
    private DataverseRole dataverseRole;
    private RoleAssignee roleAssignee;
    private Dataset dataset;
    private String privateUrlToken;

    @BeforeEach
    public void before() {
        this.dataverseRole = new DataverseRole();
        this.roleAssignee = GuestUser.get();
        this.dataset = new Dataset();
        this.privateUrlToken = "some-token";
    }

    @AfterEach
    public void after() {
        this.dataverseRole = null;
        this.roleAssignee = null;
        this.dataset = null;
        this.privateUrlToken = null;
    }

    @Test
    public void testSetAndGetId() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        Long id = 12345L;
        this.roleAssignment.setId(id);

        assertEquals(id, this.roleAssignment.getId());
    }

    @Test
    public void testGetAssigneeIdentifierFromConstructor() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        assertEquals(this.roleAssignee.getIdentifier(), this.roleAssignment.getAssigneeIdentifier());
    }

    @Test
    public void testSetAndGetAssigneeIdentifier() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        String assigneeIdentifier = ":admin";
        this.roleAssignment.setAssigneeIdentifier(assigneeIdentifier);

        assertEquals(assigneeIdentifier, this.roleAssignment.getAssigneeIdentifier());
    }

    @Test
    public void testGetRoleFromConstructor() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        assertEquals(this.dataverseRole, this.roleAssignment.getRole());
    }

    @Test
    public void testSetAndGetRole() {
        this.roleAssignment = new RoleAssignment(null, this.roleAssignee, this.dataset, this.privateUrlToken);
        this.roleAssignment.setRole(this.dataverseRole);

        assertEquals(this.dataverseRole, this.roleAssignment.getRole());
    }

    @Test
    public void testGetDefinitionPointFromConstructor() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        assertEquals(this.dataset, this.roleAssignment.getDefinitionPoint());
    }

    @Test
    public void testSetAndGetDefinitionPoint() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, null, this.privateUrlToken);
        this.roleAssignment.setDefinitionPoint(this.dataset);

        assertEquals(this.dataset, this.roleAssignment.getDefinitionPoint());
    }

    @Test
    public void testGetPrivateUrlTokenFromConstructor() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        assertEquals(this.privateUrlToken, this.roleAssignment.getPrivateUrlToken());
    }

    @Test
    public void testHashCodeIdentityOfSameObject() {
        // Whenever it is invoked on the same object more than once during an execution of
        // a Java application, the hashCode method must consistently return the same
        // integer, ...
        // according to:
        // https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#hashCode--

        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        int firstHash = this.roleAssignment.hashCode();
        int secondHash = this.roleAssignment.hashCode();
        assertEquals(firstHash, secondHash);
    }

    @Test
    public void testHashCodeIdentityOfDifferentObjects() {
        // If two objects are equal according to the equals(Object) method, then calling
        // the hashCode method on each of the two objects must produce the same integer
        // result.
        // according to:
        // https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#hashCode--

        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        RoleAssignment roleAssignment2 = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        int firstHash = this.roleAssignment.hashCode();
        int secondHash = roleAssignment2.hashCode();
        assertEquals(firstHash, secondHash);
    }

    @Test
    public void testEqualsWithNull() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        assertFalse(this.roleAssignment.equals(null));
    }

    @Test
    public void testEqualsWithAnotherClass() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        String roleAssignment2 = new String("some string");

        assertFalse(this.roleAssignment.equals(roleAssignment2));
    }

    @Test
    public void testEqualsWithDifferentRoles() {
        DataverseRole dataverseRole1 = new DataverseRole();
        dataverseRole1.setId(1L);
        this.roleAssignment = new RoleAssignment(dataverseRole1, this.roleAssignee, this.dataset, this.privateUrlToken);

        DataverseRole dataverseRole2 = new DataverseRole();
        dataverseRole2.setId(2L);
        RoleAssignment roleAssignment2 = new RoleAssignment(dataverseRole2, this.roleAssignee, this.dataset, this.privateUrlToken);

        assertFalse(this.roleAssignment.equals(roleAssignment2));
    }

    @Test
    public void testEqualsWithDifferentAssigneeIdentifiers() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        RoleAssignee roleAssignee = new PrivateUrlUser(13L);
        RoleAssignment roleAssignment2 = new RoleAssignment(this.dataverseRole, roleAssignee, this.dataset, this.privateUrlToken);

        assertFalse(this.roleAssignment.equals(roleAssignment2));
    }

    @Test
    public void testEqualsWithDifferentDefinitionPoints() {
        Dataset dataset1 = new Dataset();
        dataset1.setId(1L);
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, dataset1, this.privateUrlToken);

        Dataset dataset2 = new Dataset();
        dataset2.setId(2L);
        RoleAssignment roleAssignment2 = new RoleAssignment(this.dataverseRole, this.roleAssignee, dataset2, this.privateUrlToken);

        assertFalse(this.roleAssignment.equals(roleAssignment2));
    }

    @Test
    public void testEqualsWithSameObject() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        assertTrue(this.roleAssignment.equals(this.roleAssignment));
    }

    @Test
    public void testEqualsWithAnotherButEqualObject() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        RoleAssignment roleAssignment2 = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        assertTrue(this.roleAssignment.equals(roleAssignment2));
    }

    @Test
    public void testToString() {
        this.roleAssignment = new RoleAssignment(this.dataverseRole, this.roleAssignee, this.dataset, this.privateUrlToken);

        String id = String.valueOf(this.roleAssignment.getId());
        String assigneeIdentifier = this.roleAssignment.getAssigneeIdentifier();
        String role = this.roleAssignment.getRole().toString();
        String definitionPoint = this.roleAssignment.getDefinitionPoint().toString();
        String expectedString = "RoleAssignment{" + "id=" + id + ", assignee=" + assigneeIdentifier + ", role=" + role + ", definitionPoint=" + definitionPoint + '}';

        assertEquals(expectedString, this.roleAssignment.toString());
    }
}