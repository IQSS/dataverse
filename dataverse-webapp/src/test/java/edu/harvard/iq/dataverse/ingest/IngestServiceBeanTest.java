package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.common.files.mime.ApplicationMimeType;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.rdata.RDATAFileReader;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IngestServiceBeanTest {

    private static final String RSERVE_HOST = "host";
    private static final int RSERVE_PORT = 633;
    private static final String RSERVE_USER = "user";
    private static final String RSERVE_PASSWORD = "password";

    @InjectMocks
    private IngestServiceBean ingestServiceBean;

    @Mock
    private SettingsServiceBean settingsService;

    // -------------------- TESTS --------------------

    @Test
    public void getTabDataReaderByMimeType() {
        // given
        when(settingsService.getValueForKey(Key.RserveHost)).thenReturn(RSERVE_HOST);
        when(settingsService.getValueForKeyAsInt(Key.RservePort)).thenReturn(RSERVE_PORT);
        when(settingsService.getValueForKey(Key.RserveUser)).thenReturn(RSERVE_USER);
        when(settingsService.getValueForKey(Key.RservePassword)).thenReturn(RSERVE_PASSWORD);

        // when
        RDATAFileReader rdataFileReader =
                (RDATAFileReader) ingestServiceBean.getTabDataReaderByMimeType(ApplicationMimeType.RDATA.getMimeValue());

        // then
        assertThat(rdataFileReader)
                .extracting(RDATAFileReader::getRserveHost, RDATAFileReader::getRservePort,
                        RDATAFileReader::getRserveUser, RDATAFileReader::getRservePassword)
                .containsExactly(RSERVE_HOST, RSERVE_PORT,
                        RSERVE_USER, RSERVE_PASSWORD);
    }
}