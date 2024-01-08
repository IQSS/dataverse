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
public class PidProviderTest {
    
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
    
 
}
