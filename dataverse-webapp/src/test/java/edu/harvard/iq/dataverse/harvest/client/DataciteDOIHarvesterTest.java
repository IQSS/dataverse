package edu.harvard.iq.dataverse.harvest.client;

import com.google.common.collect.ImmutableList;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.globalid.DataCiteFindDoiResponse;
import edu.harvard.iq.dataverse.globalid.DataCiteRestApiClient;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import wiremock.com.google.common.collect.Lists;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataciteDOIHarvesterTest {

    @Mock
    private DataCiteRestApiClient dataCiteRestApiClient;

    @Mock
    private ImportServiceBean importService;

    @Spy
    private DataciteDatasetMapper dataciteDatasetMapper = new DataciteDatasetMapper();

    @Mock
    private HarvestingClient harvestingClient;

    @InjectMocks
    private DataciteDOIHarvester dataciteDOIHarvester = new DataciteDOIHarvester();

    private final List<DataciteHarvesterParams.DOIValue> DOI_REPO = ImmutableList.of(
            newDOI("10.5438/0012"),
            newDOI("10.18150/0013"),
            newDOI("10.18150/0014"),
            newDOI("10.18150/0015"),
            newDOI("10.18150/0016"));

    @Test
    public void harvest__import() throws ImportException, IOException {
        // given
        for (DataciteHarvesterParams.DOIValue doi : DOI_REPO) {
            when(dataCiteRestApiClient.findDoi(doi.getAuthority(), doi.getId())).thenReturn(newResponse(doi));
        }
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setDoiImport(DOI_REPO);
        DataverseRequest dataverseRequest = dataverseRequest();

       // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest, harvestingClient, Logger.getLogger("test"), params);

        // then
        for (DataciteHarvesterParams.DOIValue doi : DOI_REPO) {
            verify(importService, times(1)).doImportHarvestedDataset(any(), any(),
                    eq(doi.getFull()),
                    argThat(dto -> dto.getAuthority().equals(doi.getAuthority()) && dto.getIdentifier().equals(doi.getId())));
        }
        assertThat(result.getNumHarvested()).isEqualTo(5);
        assertThat(result.getNumDeleted()).isEqualTo(0);
        assertThat(result.getNumFailed()).isEqualTo(0);
    }

    @Test
    public void harvest__delete() throws ImportException {
        // given
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setDoiRemove(DOI_REPO);
        DataverseRequest dataverseRequest = dataverseRequest();

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest, harvestingClient, Logger.getLogger("test"), params);

        // then
        for (DataciteHarvesterParams.DOIValue doi : DOI_REPO) {
            verify(importService, times(1)).doDeleteHarvestedDataset(any(), any(),
                    eq(doi.getFull()));
        }
        assertThat(result.getNumHarvested()).isEqualTo(0);
        assertThat(result.getNumDeleted()).isEqualTo(5);
        assertThat(result.getNumFailed()).isEqualTo(0);
    }

    @Test
    public void harvest__failed() throws ImportException, IOException {
        // given
        List<DataciteHarvesterParams.DOIValue> impSuccess = Lists.newArrayList(DOI_REPO.get(0));
        List<DataciteHarvesterParams.DOIValue> impFail = Lists.newArrayList(DOI_REPO.get(1));
        List<DataciteHarvesterParams.DOIValue> delSuccess = Lists.newArrayList(DOI_REPO.get(2), DOI_REPO.get(4));
        List<DataciteHarvesterParams.DOIValue> delFail = Lists.newArrayList(DOI_REPO.get(3));

        for (DataciteHarvesterParams.DOIValue s : impSuccess) {
            when(dataCiteRestApiClient.findDoi(s.getAuthority(), s.getId())).thenReturn(newResponse(s));
        }

        for (DataciteHarvesterParams.DOIValue f : impFail) {
            when(dataCiteRestApiClient.findDoi(f.getAuthority(), f.getId())).thenThrow(new IOException("Boom!"));
        }

        for (DataciteHarvesterParams.DOIValue f : delSuccess) {
            doNothing().when(importService).doDeleteHarvestedDataset(any(), any(), eq(f.getFull()));
        }

        for (DataciteHarvesterParams.DOIValue f : delFail) {
            doThrow(new ImportException("Boom!")).when(importService).doDeleteHarvestedDataset(any(), any(), eq(f.getFull()));
        }

        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setDoiImport(ListUtils.union(impSuccess, impFail));
        params.setDoiRemove(ListUtils.union(delSuccess, delFail));
        DataverseRequest dataverseRequest = dataverseRequest();

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest, harvestingClient, Logger.getLogger("test"), params);

        // then
        for (DataciteHarvesterParams.DOIValue rs : impSuccess) {
            verify(importService, times(1)).doImportHarvestedDataset(any(), any(),
                    eq(rs.getFull()),
                    argThat(dto -> dto.getAuthority().equals(rs.getAuthority()) && dto.getIdentifier().equals(rs.getId())));
        }
        for (DataciteHarvesterParams.DOIValue rs : delSuccess) {
            verify(importService, times(1)).doDeleteHarvestedDataset(any(), any(),
                    eq(rs.getFull()));
        }
        assertThat(result.getNumHarvested()).isEqualTo(1);
        assertThat(result.getNumDeleted()).isEqualTo(2);
        assertThat(result.getNumFailed()).isEqualTo(2);
    }

    @Test
    public void harvest__empty_input() throws ImportException {
        // given
        DataciteHarvesterParams params = new DataciteHarvesterParams();

        // when & then
        assertThatThrownBy(() ->
                dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"), params))
                .hasMessage("Missing DOI's")
                .isInstanceOf(ImportException.class);
    }

    private DataverseRequest dataverseRequest() {
        return new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.1.2"));
    }

    private DataciteHarvesterParams.DOIValue newDOI(String doi) {
        return new DataciteHarvesterParams.DOIValue(doi);
    }

    private static DataCiteFindDoiResponse newResponse(DataciteHarvesterParams.DOIValue doi) {
        DataCiteFindDoiResponse response = new DataCiteFindDoiResponse();
        response.setId(RandomStringUtils.random(5));
        response.getAttributes().setCitationCount(3);
        response.getAttributes().setPrefix(doi.getAuthority());
        response.getAttributes().setSuffix(doi.getId());
        response.getAttributes().setContributors(Collections.emptyList());
        return response;
    }
}
