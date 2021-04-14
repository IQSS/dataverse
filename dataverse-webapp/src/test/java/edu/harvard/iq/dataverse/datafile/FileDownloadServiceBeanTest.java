package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.util.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.faces.context.FacesContext;
import java.io.IOException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileDownloadServiceBeanTest {

    private final FileDownloadServiceBean fileDownloadService = new FileDownloadServiceBean();
    private FacesContext facesContext = FacesContextMocker.mockContext();

    @AfterEach
    public void cleanAfterTests() {
        facesContext.release();
    }

    @Test
    void redirectToDownloadWholeDataset() throws IOException {
        //given
        facesContext = FacesContextMocker.mockContext();

        DatasetVersion dsv = new DatasetVersion();
        dsv.setId(1L);
        Dataset dataset = new Dataset();
        dataset.setId(1L);
        dsv.setDataset(dataset);
        FileUtil.ApiBatchDownloadType defaultDownloadOption = FileUtil.ApiBatchDownloadType.DEFAULT;

        //execute
        fileDownloadService.redirectToDownloadWholeDataset(dsv, true, defaultDownloadOption);

        //assert
        Mockito.verify(facesContext.getExternalContext(), Mockito.times(1)).redirect("/api/datasets/1/versions/1/files/download?gbrecs=true");
    }

    @Test
    void redirectToDownloadWholeDataset_WithoutGbRecs() throws IOException {
        //given
        facesContext = FacesContextMocker.mockContext();

        DatasetVersion dsv = new DatasetVersion();
        dsv.setId(1L);
        Dataset dataset = new Dataset();
        dataset.setId(1L);
        dsv.setDataset(dataset);
        FileUtil.ApiBatchDownloadType defaultDownloadOption = FileUtil.ApiBatchDownloadType.DEFAULT;

        //execute
        fileDownloadService.redirectToDownloadWholeDataset(dsv, false, defaultDownloadOption);

        //assert
        Mockito.verify(facesContext.getExternalContext(), Mockito.times(1)).redirect("/api/datasets/1/versions/1/files/download");
    }

    @Test
    void redirectToDownloadWholeDataset_WithOriginalFormat() throws IOException {
        //given
        facesContext = FacesContextMocker.mockContext();

        DatasetVersion dsv = new DatasetVersion();
        dsv.setId(1L);
        Dataset dataset = new Dataset();
        dataset.setId(1L);
        dsv.setDataset(dataset);
        FileUtil.ApiBatchDownloadType downloadOption = FileUtil.ApiBatchDownloadType.ORIGINAL;

        //execute
        fileDownloadService.redirectToDownloadWholeDataset(dsv, false, downloadOption);

        //assert
        Mockito.verify(facesContext.getExternalContext(), Mockito.times(1)).redirect("/api/datasets/1/versions/1/files/download?format=original");
    }
}