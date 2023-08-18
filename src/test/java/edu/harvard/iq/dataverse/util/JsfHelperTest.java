/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class JsfHelperTest {
	
	enum TestEnum { Lorem, Ipsum, Dolor, Sit, Amet }
	
	@BeforeAll
	public static void setUpClass() {
	}
	
	@AfterAll
	public static void tearDownClass() {
	}
	
	@BeforeEach
	public void setUp() {
	}
	
	@AfterEach
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
