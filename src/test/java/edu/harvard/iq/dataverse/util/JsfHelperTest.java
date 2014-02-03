/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.util;

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
public class JsfHelperTest {
	
	enum TestEnum { Lorem, Ipsum, Dolor, Sit, Amet }
	
	public JsfHelperTest() {
	}
	
	@BeforeClass
	public static void setUpClass() {
	}
	
	@AfterClass
	public static void tearDownClass() {
	}
	
	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
	}


	/**
	 * Test of enumValue method, of class JsfHelper.
	 */
	@Test
	public void testEnumValue() {
		System.out.println("enumValue");
		JsfHelper instance = new JsfHelper();
		
		assertEquals( TestEnum.Lorem, instance.enumValue("Lorem", TestEnum.class, TestEnum.Dolor) );
		assertEquals( TestEnum.Lorem, instance.enumValue("Lorem   ", TestEnum.class, TestEnum.Dolor) );
		assertEquals( TestEnum.Dolor, instance.enumValue(null, TestEnum.class, TestEnum.Dolor) );
		assertEquals( TestEnum.Dolor, instance.enumValue("THIS IS A BAD VALUE", TestEnum.class, TestEnum.Dolor) );
		
	}
	
}
