/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactoryBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author michael
 */
@ExtendWith(MockitoExtension.class)
public class PersistentIdentifierProviderTest {
    
    @Mock
    private SettingsServiceBean settingsServiceBean;

    @InjectMocks
    PidProviderFactoryBean pidProviderFactoryBean  = new PidProviderFactoryBean();
    
    CommandContext ctxt;
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ctxt = new TestCommandContext(){
            @Override
            public PidProviderFactoryBean pidProviderFactory() {
                return pidProviderFactoryBean;
            }
        };
    }
    
    /**
     * Test of getBean method, of class PersistentIdentifierServiceBean.
     */
    /*
    @Test
    public void testGetBean_String_CommandContext_OK() {
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.DoiProvider, "EZID");
        Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DoiProvider, "")).thenReturn("EZID");
        
        assertEquals(ezidServiceBean, 
                     PidProvider.getBean("doi", ctxt));
        
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.DoiProvider, "DataCite");
        Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DoiProvider, "")).thenReturn("DataCite");

        assertEquals(dataCiteServiceBean, 
                     PidProvider.getBean("doi", ctxt));

        ctxt.settings().setValueForKey(SettingsServiceBean.Key.DoiProvider, "FAKE");
        Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DoiProvider, "")).thenReturn("FAKE");

        assertEquals(fakePidProviderServiceBean,
                PidProvider.getBean("doi", ctxt));

        assertEquals(hdlServiceBean, 
                     PidProvider.getBean("hdl", ctxt));
        
        assertEquals(permaLinkServiceBean, 
                PidProvider.getBean("perma", ctxt));
    }
    */
    /*
     @Test
    public void testGetBean_String_CommandContext_BAD() {
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.DoiProvider, "non-existent-provider");
        assertNull(PidProvider.getBean("doi", ctxt));
        
        
        assertNull(PidProvider.getBean("non-existent-protocol", ctxt));
    }
*/
    /**
     * Test of getBean method, of class PersistentIdentifierServiceBean.
     */
    /*
    @Test
    public void testGetBean_CommandContext() {
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.Protocol, "doi");
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.DoiProvider, "EZID");
        Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DoiProvider, "")).thenReturn("EZID");
        
        assertEquals(ezidServiceBean, 
                     PidProvider.getBean("doi", ctxt));
        
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.Protocol, "hdl");
        assertEquals(hdlServiceBean, 
                     PidProvider.getBean("hdl", ctxt));
        
        ctxt.settings().setValueForKey( SettingsServiceBean.Key.Protocol, "perma");
        assertEquals(permaLinkServiceBean, 
                     PidProvider.getBean("perma", ctxt));
    }

*/   
}
