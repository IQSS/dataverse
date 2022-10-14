/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author michael
 */
@RunWith(Parameterized.class)
public class JsfHelperTest {
	
	enum TestEnum { Lorem, Ipsum, Dolor, Sit, Amet }
	
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

	public TestEnum inputEnum;
	public String inputString;
	public TestEnum defaultEnumValue;
	
	public JsfHelperTest(TestEnum inputEnum, String inputString, TestEnum defaultEnumValue) {
		this.inputEnum = inputEnum;
		this.inputString = inputString;
		this.defaultEnumValue = defaultEnumValue;
	}

	@Parameters
	public static Collection<Object[]> parameters() {
		return Arrays.asList (
			new Object[][] {
				{ TestEnum.Lorem, "Lorem", TestEnum.Dolor },
				{ TestEnum.Lorem, "Lorem   ", TestEnum.Dolor },
				{ TestEnum.Dolor, null, TestEnum.Dolor },
				{ TestEnum.Dolor, "THIS IS A BAD VALUE", TestEnum.Dolor },
			}
		);
	}

	/**
	 * Test of enumValue method, of class JsfHelper.
	 */
	@Test
	public void testEnumValue() {
		System.out.println("enumValue");
		JsfHelper instance = new JsfHelper();

		assertEquals( inputEnum, instance.enumValue(inputString, TestEnum.class, defaultEnumValue) );
	}
	
}
