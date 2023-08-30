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
	
	static Stream<Arguments> parameters() {
		return Stream.of(
			Arguments.of(TestEnum.Lorem, "Lorem", TestEnum.Dolor),
			Arguments.of(TestEnum.Lorem, "Lorem   ", TestEnum.Dolor),
			Arguments.of(TestEnum.Dolor, null, TestEnum.Dolor),
			Arguments.of(TestEnum.Dolor, "THIS IS A BAD VALUE", TestEnum.Dolor )
		);
	}

	/**
	 * Test of enumValue method, of class JsfHelper.
	 */
	@ParameterizedTest
	@MethodSource("parameters")
	public void testEnumValue(TestEnum inputEnum, String inputString, TestEnum defaultEnumValue) {
		System.out.println("enumValue");
		JsfHelper instance = new JsfHelper();

		assertEquals( inputEnum, instance.enumValue(inputString, TestEnum.class, defaultEnumValue) );
	}
	
}
