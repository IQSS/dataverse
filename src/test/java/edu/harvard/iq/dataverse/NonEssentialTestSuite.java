/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author BMartinez
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({DatasetFieldTypeTest.class, SlowTest.class, TestClassTest.class, EMailValidatorTest.class, PersistentIdentifierServiceBeanTest.class, TestClass.class, DatasetFieldValidatorTest.class, DatasetFieldValueValidatorTest.class, DataFileServiceBeanTest.class, GlobalIdTest.class, DatasetVersionTest.class, SlowTestSuit.class, DatasetTest.class, UserNameValidatorTest.class})
public class NonEssentialTestSuite {
    
}
