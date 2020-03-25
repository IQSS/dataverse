package edu.harvard.iq.dataverse.datafile.file;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.GenericDao;
import edu.harvard.iq.dataverse.datafile.file.exception.ProvenanceChangeException;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.provenance.UpdatesEntry;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.vavr.control.Try;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class SingleFileFacade {
    private static final Logger logger = Logger.getLogger(SingleFileFacade.class.getCanonicalName());

    private FileMetadataService fileMetadataService;
    private SettingsServiceBean settingsService;
    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;
    private GenericDao genericDao;
    private DatasetVersionServiceBean datasetVersionService;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public SingleFileFacade() {
    }

    @Inject
    public SingleFileFacade(FileMetadataService fileMetadataService, SettingsServiceBean settingsService,
                            EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService,
                            GenericDao genericDao, DatasetVersionServiceBean datasetVersionService) {
        this.fileMetadataService = fileMetadataService;
        this.settingsService = settingsService;
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
        this.genericDao = genericDao;
        this.datasetVersionService = datasetVersionService;
    }

    // -------------------- LOGIC --------------------

    /**
     * Saves provenance updates if the setting is enabled, and then updates the dataset.
     */
    public Try<Dataset> saveFileChanges(FileMetadata fileToModify,
                                   Map<String, UpdatesEntry> provUpdates) {

        DatasetVersion editVersion = fileToModify.getDatasetVersion().getDataset().getEditVersion();

        if (settingsService.isTrueForKey(SettingsServiceBean.Key.ProvCollectionEnabled)) {

            UpdatesEntry provEntry = provUpdates.get(fileToModify.getDataFile().getChecksumValue());

            if (provEntry != null) {

                Optional.ofNullable(provEntry.getProvFreeform())
                        .ifPresent(provFree -> fileMetadataService.updateFileMetadataWithProvFreeForm(fileToModify, provFree));

                Try.of(() -> fileMetadataService.manageProvJson(false, provEntry))
                        .getOrElseThrow(ex -> new ProvenanceChangeException("There was a problem with changing provenance file", ex));
            }


        }

        return Try.of(() -> datasetVersionService.updateDatasetVersion(editVersion, true))
                .onFailure(ex -> logger.log(Level.SEVERE, "Couldn't update dataset with id: " + editVersion.getDataset().getId(), ex));
    }
}
