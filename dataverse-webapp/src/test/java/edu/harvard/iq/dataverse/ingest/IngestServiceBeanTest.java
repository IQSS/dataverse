package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.common.files.mime.ApplicationMimeType;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.rdata.RDATAFileReader;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IngestServiceBeanTest {

    @InjectMocks
    private IngestServiceBean ingestServiceBean;

    @Mock
    private SettingsServiceBean settingsService;

    private final String RSERVE_HOST = "host";
    private final String RSERVE_USER = "user";
    private final String RSERVE_PASSWORD = "password";
    private final int RSERVE_PORT = 633;

    @Test
    public void getTabDataReaderByMimeType() {
        //when
        Mockito.when(settingsService.getValueForKey(SettingsServiceBean.Key.RserveHost)).thenReturn(RSERVE_HOST);
        Mockito.when(settingsService.getValueForKey(SettingsServiceBean.Key.RserveUser)).thenReturn(RSERVE_USER);
        Mockito.when(settingsService.getValueForKey(SettingsServiceBean.Key.RservePassword)).thenReturn(RSERVE_PASSWORD);
        Mockito.when(settingsService.getValueForKeyAsInt(SettingsServiceBean.Key.RservePort)).thenReturn(RSERVE_PORT);

        RDATAFileReader rdataFileReader =
                (RDATAFileReader) ingestServiceBean.getTabDataReaderByMimeType(ApplicationMimeType.RDATA.getMimeValue());

        //then
        Assert.assertEquals(RSERVE_HOST, rdataFileReader.getRserveHost());
        Assert.assertEquals(RSERVE_PASSWORD, rdataFileReader.getRservePassword());
        Assert.assertEquals(RSERVE_USER, rdataFileReader.getRserveUser());
        Assert.assertEquals(RSERVE_PORT, rdataFileReader.getRservePort());
    }
}