package edu.harvard.iq.dataverse.api;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean.RequestPermissionQuery;
import edu.harvard.iq.dataverse.dataset.EmbargoAccessService;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.RestrictType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccessTest {

    @InjectMocks
    private Access access;
    
    @Mock
    private EmbargoAccessService embargoAccessService;
    
    @Mock
    private PermissionServiceBean permissionService;
    
    @Mock
    private DataverseSession dataverseSession;
    
    @Mock
    private HttpServletRequest request;
    

    // -------------------- LOGIC --------------------

    @Test
    void isAccessAuthorized_file_in_released_dataset() {
        // given
        DataFile dataFile = createDatafile();
        Dataset dataset = dataFile.getOwner();
        dataset.getLatestVersion().setVersionState(VersionState.RELEASED);

        when(embargoAccessService.isRestrictedByEmbargo(dataset)).thenReturn(false);

        // when & then
        assertThat(access.isAccessAuthorized(dataFile)).isTrue();

    }

    @Test
    void isAccessAuthorized_file_in_draft_dataset_and_user_with_right_permission() {
        // given
        DataFile dataFile = createDatafile();
        Dataset dataset = dataFile.getOwner();

        DataverseRequest dvRequest = createAuthenticatedUserDvRequest();

        when(dataverseSession.getUser()).thenReturn(dvRequest.getUser());

        when(embargoAccessService.isRestrictedByEmbargo(dataset)).thenReturn(false);

        mockPermissionResponse(dvRequest, dataset, Permission.ViewUnpublishedDataset, true);

        // when & then
        assertThat(access.isAccessAuthorized(dataFile)).isTrue();

    }

    @Test
    void isAccessAuthorized_file_in_draft_dataset_and_user_without_right_permission() {
        // given
        DataFile dataFile = createDatafile();
        Dataset dataset = dataFile.getOwner();

        DataverseRequest dvRequest = createAuthenticatedUserDvRequest();

        when(dataverseSession.getUser()).thenReturn(dvRequest.getUser());

        when(embargoAccessService.isRestrictedByEmbargo(dataset)).thenReturn(false);

        mockPermissionResponse(dvRequest, dataset, Permission.ViewUnpublishedDataset, false);
        mockPermissionResponse(createGuestUserDvRequest(), dataset, Permission.ViewUnpublishedDataset, false);

        // when & then
        assertThat(access.isAccessAuthorized(dataFile)).isFalse();

    }

    @Test
    void isAccessAuthorized_restricted_file_in_released_dataset_and_user_without_right_permission() {
        // given
        DataFile dataFile = createDatafile();
        dataFile.getLatestFileMetadata().getTermsOfUse().setLicense(null);
        dataFile.getLatestFileMetadata().getTermsOfUse().setRestrictType(RestrictType.NOT_FOR_REDISTRIBUTION);
        Dataset dataset = dataFile.getOwner();
        dataset.getLatestVersion().setVersionState(VersionState.RELEASED);

        DataverseRequest dvRequest = createAuthenticatedUserDvRequest();

        when(dataverseSession.getUser()).thenReturn(dvRequest.getUser());

        when(embargoAccessService.isRestrictedByEmbargo(dataset)).thenReturn(false);

        mockPermissionResponse(dvRequest, dataFile, Permission.DownloadFile, false);
        mockPermissionResponse(createGuestUserDvRequest(), dataFile, Permission.DownloadFile, false);

        // when & then
        assertThat(access.isAccessAuthorized(dataFile)).isFalse();

    }

    @Test
    void isAccessAuthorized_restricted_file_in_released_dataset_and_user_with_right_permission() {
        // given
        DataFile dataFile = createDatafile();
        dataFile.getLatestFileMetadata().getTermsOfUse().setLicense(null);
        dataFile.getLatestFileMetadata().getTermsOfUse().setRestrictType(RestrictType.NOT_FOR_REDISTRIBUTION);
        Dataset dataset = dataFile.getOwner();
        dataset.getLatestVersion().setVersionState(VersionState.RELEASED);

        DataverseRequest dvRequest = createAuthenticatedUserDvRequest();

        when(dataverseSession.getUser()).thenReturn(dvRequest.getUser());

        when(embargoAccessService.isRestrictedByEmbargo(dataset)).thenReturn(false);

        mockPermissionResponse(dvRequest, dataFile, Permission.DownloadFile, true);

        // when & then
        assertThat(access.isAccessAuthorized(dataFile)).isTrue();

    }

    @Test
    void isAccessAuthorized_file_in_embargoed_dataset() {
        // given
        DataFile dataFile = createDatafile();
        Dataset dataset = dataFile.getOwner();

        when(embargoAccessService.isRestrictedByEmbargo(dataset)).thenReturn(true);

        // when & then
        assertThat(access.isAccessAuthorized(dataFile)).isFalse();

    }

    // -------------------- PRIVATE --------------------

    private void mockPermissionResponse(DataverseRequest request, DvObject dvObject, Permission permission, boolean answer) {
        RequestPermissionQuery permissionQuery = mock(RequestPermissionQuery.class);
        when(permissionQuery.has(permission)).thenReturn(answer);
        lenient().when(permissionService.requestOn(request, dvObject)).thenReturn(permissionQuery);
    }

    private DataverseRequest createAuthenticatedUserDvRequest() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setId(1L);
        authenticatedUser.setUserIdentifier("username");
        return new DataverseRequest(authenticatedUser, request);
    }
    private DataverseRequest createGuestUserDvRequest() {
        return new DataverseRequest(GuestUser.get(), request);
    }
    
    private DataFile createDatafile() {
        Dataset dataset = new Dataset();
        dataset.setId(1L);
        DatasetVersion version = dataset.getLatestVersion();

        DataFile dataFile = new DataFile();
        dataFile.setId(2L);
        dataFile.setOwner(dataset);

        FileMetadata fileMetadata = new FileMetadata();
        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        termsOfUse.setLicense(new License());
        fileMetadata.setTermsOfUse(termsOfUse);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(version);
        version.addFileMetadata(fileMetadata);

        dataFile.setFileMetadatas(Lists.newArrayList(fileMetadata));

        return dataFile;
    }
}
