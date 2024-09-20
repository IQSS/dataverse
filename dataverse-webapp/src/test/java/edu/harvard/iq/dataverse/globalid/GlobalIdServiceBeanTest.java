package edu.harvard.iq.dataverse.globalid;

import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author michael
 */
public class GlobalIdServiceBeanTest {


    DOIEZIdServiceBean ezidServiceBean = new DOIEZIdServiceBean();
    DOIDataCiteServiceBean dataCiteServiceBean = new DOIDataCiteServiceBean();
    FakePidProviderServiceBean fakePidProviderServiceBean = new FakePidProviderServiceBean();
    HandlenetServiceBean hdlServiceBean = new HandlenetServiceBean();

    CommandContext ctxt;

    @Before
    public void setup() {
        ctxt = new TestCommandContext() {
            @Override
            public GlobalIdServiceBeanResolver globalIdServiceBeanResolver() {
                GlobalIdServiceBeanResolver resolver = new GlobalIdServiceBeanResolver(settings(), hdlServiceBean, ezidServiceBean, dataCiteServiceBean, fakePidProviderServiceBean);
                resolver.setup();
                return resolver;
            }
        };
    }

    /**
     * Test of getBean method, of class PersistentIdentifierServiceBean.
     */
    @Test
    public void testGetBean_String_CommandContext_OK() {
        ctxt.settings().setValueForKey(SettingsServiceBean.Key.DoiProvider, "EZID");
        assertEquals(ezidServiceBean,
                     GlobalIdServiceBean.getBean("doi", ctxt));

        ctxt.settings().setValueForKey(SettingsServiceBean.Key.DoiProvider, "DataCite");
        assertEquals(dataCiteServiceBean,
                     GlobalIdServiceBean.getBean("doi", ctxt));

        ctxt.settings().setValueForKey(SettingsServiceBean.Key.DoiProvider, "FAKE");
        assertEquals(fakePidProviderServiceBean,
                     GlobalIdServiceBean.getBean("doi", ctxt));

        assertEquals(hdlServiceBean,
                     GlobalIdServiceBean.getBean("hdl", ctxt));
    }

    @Test
    public void testGetBean_String_CommandContext_BAD() {
        ctxt.settings().setValueForKey(SettingsServiceBean.Key.DoiProvider, "non-existent-provider");
        assertNull(GlobalIdServiceBean.getBean("doi", ctxt));


        assertNull(GlobalIdServiceBean.getBean("non-existent-protocol", ctxt));
    }

    /**
     * Test of getBean method, of class PersistentIdentifierServiceBean.
     */
    @Test
    public void testGetBean_CommandContext() {
        ctxt.settings().setValueForKey(SettingsServiceBean.Key.Protocol, "doi");
        ctxt.settings().setValueForKey(SettingsServiceBean.Key.DoiProvider, "EZID");

        assertEquals(ezidServiceBean,
                     GlobalIdServiceBean.getBean("doi", ctxt));

        ctxt.settings().setValueForKey(SettingsServiceBean.Key.Protocol, "hdl");
        assertEquals(hdlServiceBean,
                     GlobalIdServiceBean.getBean("hdl", ctxt));
    }


}
