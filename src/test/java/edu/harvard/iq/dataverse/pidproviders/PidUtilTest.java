package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.DOIServiceBean;
import edu.harvard.iq.dataverse.DataFileCategoryServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObjectBuilder;
import javax.ws.rs.NotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.Before;
import org.junit.Ignore;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Useful for testing but requires DataCite credentials, etc.
 */
@RunWith(MockitoJUnitRunner.class)
public class PidUtilTest {
    @Mock
    private SettingsServiceBean settingsServiceBean;
    @InjectMocks
    private PermaLinkPidProviderServiceBean p = new PermaLinkPidProviderServiceBean();
    

    @Before public void initMocks() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Protocol)).thenReturn("perma");
        Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Authority)).thenReturn("DANSLINK");
        p.reInit();
    }
    
    @Ignore
    @Test
    public void testGetDoi() throws IOException {
        String username = System.getenv("DataCiteUsername");
        String password = System.getenv("DataCitePassword");
        String baseUrl = "https://api.test.datacite.org";
        GlobalId pid = new GlobalId(DOIServiceBean.DOI_PROTOCOL,"10.70122","QE5A-XN55", "/", DOIServiceBean.DOI_RESOLVER_URL, null);
        try {
            JsonObjectBuilder result = PidUtil.queryDoi(pid, baseUrl, username, password);
            String out = JsonUtil.prettyPrint(result.build());
            System.out.println("out: " + out);
        } catch (NotFoundException ex) {
            System.out.println("ex: " + ex);
        }
    }
    
    @Test
    public void testGetPermaLink() throws IOException {
        List<GlobalIdServiceBean> list = new ArrayList<GlobalIdServiceBean>();
        

        list.add(p);
        PidUtil.addAllToProviderList(list);
        GlobalId pid = new GlobalId(PermaLinkPidProviderServiceBean.PERMA_PROTOCOL,"DANSLINK","QE5A-XN55", "", p.getUrlPrefix(), PermaLinkPidProviderServiceBean.PERMA_PROVIDER_NAME);
        System.out.println(pid.asString());
        System.out.println(pid.asURL());
        
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid.asString());
        assertEquals(pid.asString(), pid2.asString());
        GlobalId pid3 = PidUtil.parseAsGlobalID(pid.asURL());
        assertEquals(pid.asString(), pid3.asString());
        
    }

}
