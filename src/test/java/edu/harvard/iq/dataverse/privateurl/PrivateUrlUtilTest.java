package edu.harvard.iq.dataverse.privateurl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreatePrivateUrlCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class PrivateUrlUtilTest {

    @BeforeAll
    public static void setUp() {
        new PrivateUrlUtil();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "@pete", PrivateUrlUser.PREFIX + "nonNumber"})
    void testIdentifier2roleAssignee_returnsNullForInvalidIdentifier(String identifier) {
        RoleAssignee returnValueFromEmptyString = PrivateUrlUtil.identifier2roleAssignee(identifier);
        assertNull(returnValueFromEmptyString);
    }

    @Test
    void testIdentifier2roleAssignee_fromValidIdentifier() {
        String validIdentifier = PrivateUrlUser.PREFIX + 42;
        RoleAssignee returnFromValidIdentifier = PrivateUrlUtil.identifier2roleAssignee(validIdentifier);
        assertTrue(returnFromValidIdentifier instanceof PrivateUrlUser);
        assertEquals(validIdentifier, returnFromValidIdentifier.getIdentifier());
    }

    @Test
    public void testGetDatasetFromRoleAssignmentNullRoleAssignment() {
        assertNull(PrivateUrlUtil.getDatasetFromRoleAssignment(null));
    }

    @Test
    public void testGetDatasetFromRoleAssignmentNullDefinitionPoint() {
        DataverseRole aRole = null;
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUser;
        DvObject nullDefinitionPoint = null;
        String privateUrlToken = null;
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, nullDefinitionPoint, privateUrlToken);
        assertNull(PrivateUrlUtil.getDatasetFromRoleAssignment(ra));
    }

    @Test
    public void testGetDatasetFromRoleAssignmentNonDataset() {
        DataverseRole aRole = null;
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUser;
        DvObject nonDataset = new Dataverse();
        String privateUrlToken = null;
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, nonDataset, privateUrlToken);
        assertNull(PrivateUrlUtil.getDatasetFromRoleAssignment(ra));
    }

    @Test
    public void testGetDatasetFromRoleAssignmentSuccess() {
        DataverseRole aRole = null;
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUser;
        DvObject dataset = new Dataset();
        String privateUrlToken = null;
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        assertNotNull(PrivateUrlUtil.getDatasetFromRoleAssignment(ra));
        assertEquals("#42", ra.getAssigneeIdentifier());
    }

    @Test
    public void testGetDraftDatasetVersionFromRoleAssignmentNullRoleAssignement() {
        assertNull(PrivateUrlUtil.getDraftDatasetVersionFromRoleAssignment(null));
    }

    @Test
    public void testGetDraftDatasetVersionFromRoleAssignmentNullDataset() {
        DataverseRole aRole = null;
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUser;
        DvObject dataset = null;
        String privateUrlToken = null;
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        DatasetVersion datasetVersion = PrivateUrlUtil.getDraftDatasetVersionFromRoleAssignment(ra);
        assertNull(datasetVersion);
    }

    @Test
    public void testGetDraftDatasetVersionFromRoleAssignmentLastestIsNotDraft() {
        DataverseRole aRole = null;
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUser;
        Dataset dataset = new Dataset();
        List<DatasetVersion> versions = new ArrayList<>();
        DatasetVersion datasetVersionIn = new DatasetVersion();
        datasetVersionIn.setVersionState(DatasetVersion.VersionState.RELEASED);
        versions.add(datasetVersionIn);
        dataset.setVersions(versions);
        String privateUrlToken = null;
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        DatasetVersion datasetVersionOut = PrivateUrlUtil.getDraftDatasetVersionFromRoleAssignment(ra);
        assertNull(datasetVersionOut);
    }

    @Test
    public void testGetDraftDatasetVersionFromRoleAssignmentSuccess() {
        DataverseRole aRole = null;
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUser;
        Dataset dataset = new Dataset();
        List<DatasetVersion> versions = new ArrayList<>();
        DatasetVersion datasetVersionIn = new DatasetVersion();
        datasetVersionIn.setVersionState(DatasetVersion.VersionState.DRAFT);
        versions.add(datasetVersionIn);
        dataset.setVersions(versions);
        String privateUrlToken = null;
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        DatasetVersion datasetVersionOut = PrivateUrlUtil.getDraftDatasetVersionFromRoleAssignment(ra);
        assertNotNull(datasetVersionOut);
        assertEquals("#42", ra.getAssigneeIdentifier());
    }

    @Test
    public void testGetUserFromRoleAssignmentNull() {
        assertNull(PrivateUrlUtil.getPrivateUrlUserFromRoleAssignment(null));
    }

    @Test
    public void testGetUserFromRoleAssignmentNonDataset() {
        DataverseRole aRole = null;
        PrivateUrlUser privateUrlUserIn = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUserIn;
        DvObject nonDataset = new Dataverse();
        nonDataset.setId(123l);
        String privateUrlToken = null;
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, nonDataset, privateUrlToken);
        PrivateUrlUser privateUrlUserOut = PrivateUrlUtil.getPrivateUrlUserFromRoleAssignment(ra);
        assertNull(privateUrlUserOut);
    }

    @Test
    public void testGetUserFromRoleAssignmentSucess() {
        DataverseRole aRole = null;
        PrivateUrlUser privateUrlUserIn = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUserIn;
        DvObject dataset = new Dataset();
        dataset.setId(123l);
        String privateUrlToken = null;
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        PrivateUrlUser privateUrlUserOut = PrivateUrlUtil.getPrivateUrlUserFromRoleAssignment(ra);
        assertNotNull(privateUrlUserOut);
    }

    @Test
    public void testGetPrivateUrlRedirectDataFail() {
        DataverseRole aRole = null;
        long datasetId = 42;
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(datasetId);
        RoleAssignee anAssignee = privateUrlUser;
        Dataset dataset = new Dataset();
        String privateUrlToken = null;
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        ra.setDefinitionPoint(null);
        PrivateUrlRedirectData privateUrlRedirectData = null;
        privateUrlRedirectData = PrivateUrlUtil.getPrivateUrlRedirectData(ra);
        assertNull(privateUrlRedirectData);
    }

    @Test
    public void testGetPrivateUrlRedirectDataSuccess() {
        DataverseRole aRole = null;
        long datasetId = 42;
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(datasetId);
        RoleAssignee anAssignee = privateUrlUser;
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("3L33T");
        dataset.setId(datasetId);
        String privateUrlToken = null;
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        PrivateUrlRedirectData privateUrlRedirectData = PrivateUrlUtil.getPrivateUrlRedirectData(ra);
        assertNotNull(privateUrlRedirectData);
        assertEquals("/dataset.xhtml?persistentId=doi:10.5072/FK2/3L33T&version=DRAFT", privateUrlRedirectData.getDraftDatasetPageToBeRedirectedTo());
        assertEquals(privateUrlUser.getIdentifier(), privateUrlRedirectData.getPrivateUrlUser().getIdentifier());
    }

    @Test
    public void testGetDraftUrlDraftNull() {
        assertEquals("UNKNOWN", PrivateUrlUtil.getDraftUrl(null));
    }

    @Test
    public void testGetDraftUrlDatasetNull() {
        DatasetVersion draft = new DatasetVersion();
        draft.setDataset(null);
        assertEquals("UNKNOWN", PrivateUrlUtil.getDraftUrl(draft));
    }

    @Test
    public void testGetDraftUrlNoGlobalId() throws Exception {
        DatasetVersion draft = new DatasetVersion();
        Dataset dataset = new Dataset();
        draft.setDataset(dataset);
        assertEquals("UNKNOWN", PrivateUrlUtil.getDraftUrl(draft));
    }

    @Test
    public void testGetDraftUrlSuccess() throws Exception {
        DatasetVersion draft = new DatasetVersion();
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("3L33T");
        draft.setDataset(dataset);
        assertEquals("/dataset.xhtml?persistentId=doi:10.5072/FK2/3L33T&version=DRAFT", PrivateUrlUtil.getDraftUrl(draft));
    }

    @Test
    public void testGetPrivateUrlRedirectDataConstructor() throws Exception {
        Exception exception1 = null;
        try {
            PrivateUrlRedirectData privateUrlRedirectData = new PrivateUrlRedirectData(null, null);
        } catch (Exception ex) {
            exception1 = ex;
        }
        assertNotNull(exception1);
        Exception exception2 = null;
        try {
            PrivateUrlUser privateUrlUser = new PrivateUrlUser(42);
            PrivateUrlRedirectData privateUrlRedirectData = new PrivateUrlRedirectData(privateUrlUser, null);
        } catch (Exception ex) {
            exception2 = ex;
        }
        assertNotNull(exception2);
    }

    @Test
    public void testGetPrivateUrlFromRoleAssignmentNoSiteUrl() {
        String dataverseSiteUrl = null;
        RoleAssignment ra = null;
        PrivateUrl privateUrl = PrivateUrlUtil.getPrivateUrlFromRoleAssignment(ra, dataverseSiteUrl);
        assertNull(privateUrl);
    }

    @Test
    public void testGetPrivateUrlFromRoleAssignmentDatasetNull() {
        String dataverseSiteUrl = "https://dataverse.example.edu";
        DataverseRole aRole = null;
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUser;
        DvObject dataset = null;
        String privateUrlToken = null;
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        PrivateUrl privateUrl = PrivateUrlUtil.getPrivateUrlFromRoleAssignment(ra, dataverseSiteUrl);
        assertNull(privateUrl);
    }

    @Test
    public void testGetPrivateUrlFromRoleAssignmentSuccess() {
        String dataverseSiteUrl = "https://dataverse.example.edu";
        DataverseRole aRole = null;
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUser;
        DvObject dataset = new Dataset();
        dataset.setId(42l);
        String privateUrlToken = "cd71e9d7-73a7-4ec8-b890-3d00499e8693";
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        PrivateUrl privateUrl = PrivateUrlUtil.getPrivateUrlFromRoleAssignment(ra, dataverseSiteUrl);
        assertNotNull(privateUrl);
        assertEquals(new Long(42), privateUrl.getDataset().getId());
        assertEquals("https://dataverse.example.edu/privateurl.xhtml?token=cd71e9d7-73a7-4ec8-b890-3d00499e8693", privateUrl.getLink());
    }

    @Test
    public void testGetPrivateUrlUserFromRoleAssignmentAndAssigneeNull() {
        PrivateUrlUser privateUrl = PrivateUrlUtil.getPrivateUrlUserFromRoleAssignment(null, null);
        assertNull(privateUrl);
    }

    @Test
    public void testGetPrivateUrlUserFromRoleAssignmentAndAssigneeNonPrivateUrlUser() {
        DataverseRole aRole = null;
        RoleAssignee assignee = GuestUser.get();
        DvObject dataset = new Dataset();
        String privateUrlToken = "cd71e9d7-73a7-4ec8-b890-3d00499e8693";
        RoleAssignment assignment = new RoleAssignment(aRole, assignee, dataset, privateUrlToken);
        PrivateUrlUser privateUrl = PrivateUrlUtil.getPrivateUrlUserFromRoleAssignment(assignment, assignee);
        assertNull(privateUrl);
    }

    @Test
    public void testGetPrivateUrlUserFromRoleAssignmentAndAssigneeSuccess() {
        DataverseRole aRole = null;
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(42);
        RoleAssignee assignee = privateUrlUser;
        DvObject dataset = new Dataset();
        dataset.setId(42l);
        String privateUrlToken = "cd71e9d7-73a7-4ec8-b890-3d00499e8693";
        RoleAssignment assignment = new RoleAssignment(aRole, assignee, dataset, privateUrlToken);
        PrivateUrlUser privateUrl = PrivateUrlUtil.getPrivateUrlUserFromRoleAssignment(assignment, assignee);
        assertNotNull(privateUrl);
    }

    @Test
    public void testGetRequiredPermissions() {
        CreatePrivateUrlCommand createPrivateUrlCommand = new CreatePrivateUrlCommand(null, null);
        CommandException ex = new CommandException(null, createPrivateUrlCommand);
        List<String> strings = PrivateUrlUtil.getRequiredPermissions(ex);
        assertEquals(Arrays.asList("ManageDatasetPermissions"), strings);
    }

}
