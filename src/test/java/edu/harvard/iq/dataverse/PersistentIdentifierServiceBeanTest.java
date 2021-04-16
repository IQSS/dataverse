/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.pidproviders.FakePidProviderServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class PersistentIdentifierServiceBeanTest {
    
    
    DOIEZIdServiceBean ezidServiceBean = new DOIEZIdServiceBean();
    DOIDataCiteServiceBean dataCiteServiceBean = new DOIDataCiteServiceBean();
    FakePidProviderServiceBean fakePidProviderServiceBean = new FakePidProviderServiceBean();
    HandlenetServiceBean hdlServiceBean = new HandlenetServiceBean();
    
    CommandContext ctxt;
    
    @Before
    public void setup() {
        ctxt = new TestCommandContext(){
            @Override
            public HandlenetServiceBean handleNet() {
                return hdlServiceBean;
            }

            @Override
            public DOIDataCiteServiceBean doiDataCite() {
                return dataCiteServiceBean;
            }

            @Override
            public DOIEZIdServiceBean doiEZId() {
                return ezidServiceBean;
            }

            @Override
            public FakePidProviderServiceBean fakePidProvider() {
                return fakePidProviderServiceBean;
            }
            
        };
    }
    
    /**
     * Test of getBean method, of class PersistentIdentifierServiceBean.
     */
    @Test
    public void testGetBean_String_CommandContext_OK() {
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.DoiProvider, "EZID");
        assertEquals(ezidServiceBean, 
                     GlobalIdServiceBean.getBean("doi", ctxt));
        
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.DoiProvider, "DataCite");
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
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.DoiProvider, "non-existent-provider");
        assertNull(GlobalIdServiceBean.getBean("doi", ctxt));
        
        
        assertNull(GlobalIdServiceBean.getBean("non-existent-protocol", ctxt));
    }

    /**
     * Test of getBean method, of class PersistentIdentifierServiceBean.
     */
    @Test
    public void testGetBean_CommandContext() {
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.Protocol, "doi");
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.DoiProvider, "EZID");
        
        assertEquals(ezidServiceBean, 
                     GlobalIdServiceBean.getBean("doi", ctxt));
        
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.Protocol, "hdl");
        assertEquals(hdlServiceBean, 
                     GlobalIdServiceBean.getBean("hdl", ctxt));
    }

   
}
