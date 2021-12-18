package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.globalid.DOIDataCiteRegisterService;
import edu.harvard.iq.dataverse.globalid.DOIDataCiteServiceBean;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class DOIDataCiteServiceBeanTest {

    @Mock
    private DOIDataCiteRegisterService doiDataCiteRegisterService;

    @Mock
    private SystemConfig systemConfig;

    @InjectMocks
    private DOIDataCiteServiceBean doiDataCiteServiceBean;

    @Test
    void deleteIdentifier_forReserved() throws Exception {
        //given
        final Dataset dataset = new Dataset();
        final String identifier = "doi:10.5072/FK2/BYM3IW";
        dataset.setGlobalId(new GlobalId(identifier));
        final HashMap<String, String> metadata = new HashMap<>();
        metadata.put("_status", "reserved");

        //when
        Mockito.when(doiDataCiteRegisterService.getMetadata(identifier)).thenReturn(metadata);
        doiDataCiteServiceBean.deleteIdentifier(dataset);

        //then
        Mockito.verify(doiDataCiteRegisterService, Mockito.times(1)).deleteIdentifier(identifier);
    }

    @Test
    void deleteIdentifier_forPublic() throws Exception {
        //given
        final Dataset dataset = new Dataset();
        final String identifier = "doi:10.5072/FK2/BYM3IW";
        dataset.setGlobalId(new GlobalId(identifier));
        final Map<String, String> metadata = Collections.singletonMap("_status", "public");

        //when
        Mockito.when(doiDataCiteRegisterService.getMetadata(identifier)).thenReturn(metadata);
        doiDataCiteServiceBean.deleteIdentifier(dataset);

        //then
        Mockito.verify(doiDataCiteRegisterService, Mockito.times(1))
               .deactivateIdentifier(Mockito.eq(identifier));
    }
}