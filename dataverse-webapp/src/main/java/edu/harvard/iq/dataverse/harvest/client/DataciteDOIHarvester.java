package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.globalid.DataCiteRestApiClient;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@LocalBean
public class DataciteDOIHarvester implements Harvester<DataciteHarvesterParams> {


    @Inject
    private DataCiteRestApiClient dataCiteRestApiClient;

    @Inject
    private ImportServiceBean importService;

    @Inject
    private DataciteDatasetMapper dataciteDatasetMapper;

    // -------------------- LOGIC --------------------

    @Override
    public HarvestType harvestType() {
        return HarvestType.DATACITE_DOI;
    }

    @Override
    public Class<DataciteHarvesterParams> getParamsClass() {
        return DataciteHarvesterParams.class;
    }

    @Override
    public HarvesterResult harvest(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, DataciteHarvesterParams params) throws ImportException {
        if (params.getDoiImport().isEmpty()) {
            throw new ImportException("Missing DOI's");
        }

        HarvesterResult rs = new HarvesterResult();

        for (DataciteHarvesterParams.DOIValue doi: params.getDoiImport()) {
            importDOI(rs, dataverseRequest, harvestingClient, hdLogger, doi);
        }

        return rs;
    }

    // -------------------- PRIVATE --------------------

    private void importDOI(HarvesterResult rs, DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, DataciteHarvesterParams.DOIValue doi) throws ImportException {
        try {
            DatasetDTO dto = dataciteDatasetMapper.toDataset(dataCiteRestApiClient.findDoi(doi.getAuthority(), doi.getId()));
            importService.doImportHarvestedDataset(dataverseRequest, harvestingClient, doi.getFull(), dto);
            rs.incrementHarvested();
        } catch (Exception e) {
            rs.incrementFailed();
            String errorMessage = "Failed to import DOI "
                    + harvestingClient.getName()
                    + "; "
                    + e.getMessage();
            hdLogger.log(Level.SEVERE, errorMessage);
            throw new ImportException(errorMessage, e);
        }
    }
}
