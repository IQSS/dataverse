package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataset.difference.DatasetFileTermDifferenceItem;
import edu.harvard.iq.dataverse.dataset.difference.LicenseDifferenceFinder;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.TermsOfUseRepository;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Class which is designed as a workaround for memory problems with keeping ViewScoped beans and all it's data in memory.
 */
@Stateless
public class DatasetPageFacade {

    private DatasetVersionRepository dsvRepository;
    private LicenseDifferenceFinder licenseDifferenceFinder;
    private DataFileServiceBean dataFileService;
    private TermsOfUseRepository termsOfUseRepository;
    private DatasetDao datasetDao;
    private DataverseDao dataverseDao;
    private DatasetVersionRepository datasetVersionRepository;

    public DatasetPageFacade() {
    }

    @Inject
    public DatasetPageFacade(DatasetVersionRepository dsvRepository, LicenseDifferenceFinder licenseDifferenceFinder,
                             DataFileServiceBean dataFileService, TermsOfUseRepository termsOfUseRepository,
                             DatasetDao datasetDao, DataverseDao dataverseDao, DatasetVersionRepository datasetVersionRepository) {
        this.dsvRepository = dsvRepository;
        this.licenseDifferenceFinder = licenseDifferenceFinder;
        this.dataFileService = dataFileService;
        this.termsOfUseRepository = termsOfUseRepository;
        this.datasetDao = datasetDao;
        this.dataverseDao = dataverseDao;
        this.datasetVersionRepository = datasetVersionRepository;
    }

    public boolean isLatestDatasetWithAnyFilesIncluded(Long datasetVersionId) {
        Optional<DatasetVersion> dsv = dsvRepository.findById(datasetVersionId);

        DatasetVersion datasetVersion = dsv.orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + datasetVersionId));

        return !datasetVersion.getFileMetadatas().isEmpty();
    }

    public List<DatasetFileTermDifferenceItem> loadFilesTermDiffs(Long workingDatasetVersionId, Long releasedDatasetVersionId) {
        DatasetVersion workingVersion = dsvRepository.findById(workingDatasetVersionId)
                                                     .orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + workingDatasetVersionId));

        DatasetVersion releasedVersion = dsvRepository.findById(releasedDatasetVersionId)
                                                     .orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + releasedDatasetVersionId));

        return licenseDifferenceFinder.getLicenseDifference(workingVersion.getFileMetadatas(), releasedVersion.getFileMetadatas());
    }

    public boolean isSameTermsOfUseForAllFiles(Long datasetVersionId) {
        DatasetVersion dsv = dsvRepository.findById(datasetVersionId)
                                                     .orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + datasetVersionId));

        if (dsv.getFileMetadatas().isEmpty()) {
            return true;
        }
        FileTermsOfUse firstTermsOfUse = dsv.getFileMetadatas().get(0).getTermsOfUse();

        for (FileMetadata fileMetadata : dsv.getFileMetadatas()) {
            if (!dataFileService.isSameTermsOfUse(firstTermsOfUse, fileMetadata.getTermsOfUse())) {
                return false;
            }
        }

        return true;
    }

    public boolean isMinorUpdate(Long datasetVersionId) {
        DatasetVersion dsv = dsvRepository.findById(datasetVersionId)
                                          .orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + datasetVersionId));

        return dsv.isMinorUpdate();
    }

    public Optional<FileTermsOfUse> getTermsOfUseOfFirstFile(Long datasetVersionId) {
        return termsOfUseRepository.retrieveFirstFileTermsOfUse(datasetVersionId);
    }

    public Dataset retrieveDataset(Long datasetId) {
        return datasetDao.find(datasetId);
    }

    public Dataset findByGlobalId(String globalId) {
        return datasetDao.findByGlobalId(globalId);
    }

    public List<Dataverse> filterDataversesForLinking(String query, DataverseRequest dataverseRequest, Dataset dataset) {
        return dataverseDao.filterDataversesForLinking(query, dataverseRequest, dataset);
    }

    public void assignDatasetThumbnailByNativeQuery(Dataset dataset, DataFile datafile) {
        datasetDao.assignDatasetThumbnailByNativeQuery(dataset, datafile);
    }

    public int getFileSize(Long dsvId) {
        return datasetVersionRepository.findById(dsvId)
                                .orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + dsvId))
                                .getFileMetadatas().size();
    }

}
