package edu.harvard.iq.dataverse.dataset.tab;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.datafile.page.FileDownloadHelper;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileCategory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class which is designed as a workaround for memory problems with keeping ViewScoped beans and all it's data in memory.
 */
@Stateless
public class DatasetFilesTabFacade {

    private DatasetVersionRepository datasetVersionRepository;
    private FileDownloadHelper fileDownloadHelper;
    private DatasetDao datasetDao;

    public DatasetFilesTabFacade() {
    }

    @Inject
    public DatasetFilesTabFacade(DatasetVersionRepository datasetVersionRepository, FileDownloadHelper fileDownloadHelper, DatasetDao datasetDao) {
        this.datasetVersionRepository = datasetVersionRepository;
        this.fileDownloadHelper = fileDownloadHelper;
        this.datasetDao = datasetDao;
    }

    public boolean isVersionContainsDownloadableFiles(Long datasetVersionId) {
        DatasetVersion dsv = datasetVersionRepository.findById(datasetVersionId)
                                                     .orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + datasetVersionId));

        return dsv.getFileMetadatas().stream()
                      .anyMatch(fm -> fileDownloadHelper.canUserDownloadFile(fm));
    }

    public boolean isVersionContainsNonDownloadableFiles(Long datasetVersionId) {
        DatasetVersion dsv = datasetVersionRepository.findById(datasetVersionId)
                                                     .orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + datasetVersionId));

        return dsv.getFileMetadatas().stream()
                  .anyMatch(fm -> !fileDownloadHelper.canUserDownloadFile(fm));
    }

    public DatasetVersion updateFileTagsAndCategories(Long datasetVersionId,
                                                      Collection<FileMetadata> selectedFiles,
                                                      Collection<String> selectedFileMetadataTags,
                                                      Collection<String> selectedDataFileTags) {
        DatasetVersion dsv = datasetVersionRepository.findById(datasetVersionId)
                                                     .orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + datasetVersionId));


        for (FileMetadata fmd : selectedFiles) {
            fmd.getCategories().clear();
            selectedFileMetadataTags.forEach(fmd::addCategoryByName);

            if (fmd.getDataFile().isTabularData()) {
                setTagsForTabularData(selectedDataFileTags, fmd);
            }
            dsv.getFileMetadatas().stream()
                          .filter(fileMetadata -> fileMetadata.getId().equals(fmd.getId()))
                          .forEach(fileMetadata -> {
                              fileMetadata.setCategories(fmd.getCategories());
                              fileMetadata.getDataFile().setTags(fmd.getDataFile().getTags());
                          });
        }

        return dsv;
    }

    public DatasetVersion updateTermsOfUse(Long datasetVersionId,
                                           FileTermsOfUse termsOfUse,
                                           List<FileMetadata> fetchedFileMetadata) {
        DatasetVersion dsv = datasetVersionRepository.findById(datasetVersionId)
                                                     .orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + datasetVersionId));


        for (FileMetadata fm : fetchedFileMetadata) {
            dsv.getFileMetadatas().stream()
                           .filter(fileMetadata -> fileMetadata.getId().equals(fm.getId()))
                           .forEach(fileMetadata -> fileMetadata.setTermsOfUse(termsOfUse.createCopy()));
        }

        return dsv;
    }

    public Set<String> fetchCategoriesByName(Long datasetVersionId) {
        DatasetVersion dsv = datasetVersionRepository.findById(datasetVersionId)
                                                     .orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + datasetVersionId));

        return dsv.getFileMetadatas()
                  .stream()
                  .filter(fileMetadata -> fileMetadata.getCategories() != null)
                  .flatMap(fileMetadata -> fileMetadata.getCategories().stream())
                  .map(DataFileCategory::getName)
                  .collect(Collectors.toSet());
    }

    public Dataset retrieveDataset(Long datasetId) {
        return datasetDao.find(datasetId);
    }

    public DatasetLock addDatasetLock(Long datasetId, DatasetLock.Reason reason, Long userId, String info) {
        return datasetDao.addDatasetLock(datasetId, reason, userId, info);
    }

    public int fileSize(Long dsvId) {
        return datasetVersionRepository.findById(dsvId)
                                       .orElseThrow(() -> new IllegalStateException("Provided dataset version id couldn't be found id: " + dsvId))
                                       .getFileMetadatas().size();
    }

    public List<DataFileCategory> retrieveDatasetFileCategories(Long datasetId) {
        return datasetDao.find(datasetId).getCategories();
    }

    public void removeDatasetFileCategories(Long datasetId, List<DataFileCategory> categoriesToRemove) {
        datasetDao.find(datasetId).getCategories().removeAll(categoriesToRemove);
    }

    private void setTagsForTabularData(Collection<String> selectedDataFileTags, FileMetadata fmd) {
        fmd.getDataFile().getTags().clear();

        selectedDataFileTags.forEach(selectedTag -> {
            DataFileTag tag = new DataFileTag();
            tag.setTypeByLabel(selectedTag);
            tag.setDataFile(fmd.getDataFile());
            fmd.getDataFile().addTag(tag);
        });
    }
}
