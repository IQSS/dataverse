/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.util;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author michael
 */
public class BitSetTest {
	
	enum TestEnum {
		Hello, World, This, Is, A, Test;
	}
	
	public BitSetTest() {
	}
	
	@BeforeAll
	public static void setUpClass() {
	}
	
	@AfterAll
	public static void tearDownClass() {
	}
	
	BitSet sut;
	@BeforeEach
	public void setUp() {
		sut = new BitSet();
	}
	
	@AfterEach
	public void tearDown() {
	}

	/**
	 * Test of set method, of class BitSet.
	 */
	@Test
	public void testSet() {
		for ( short i : BitSet.allIndices() ) {
			sut.set(i);
			assertTrue( sut.isSet(i) );
		}
	}
    
    @Test
    public void testSetByParameter() {
        BitSet tSut = BitSet.emptySet();
        List<Integer> indices = Arrays.asList(0,1,4,6,7,8,20,31);
        indices.forEach( i -> assertFalse(tSut.isSet(i)) );
        indices.forEach( i -> tSut.set(i,true) );
        indices.forEach( i -> assertTrue(tSut.isSet(i)) );
        indices.forEach( i -> tSut.set(i,false) );
        indices.forEach( i -> assertFalse(tSut.isSet(i)) );
        assertTrue( tSut.isEmpty() );
    }
    
	/**
	 * Test of unset method, of class BitSet.
	 */
	@Test
	public void testUnset() {
		sut = BitSet.fullSet();
		for ( short i : BitSet.allIndices() ) {
			sut.unset(i);
			assertFalse( sut.isSet(i) );
		}
	}

	/**
	 * Test of copy method, of class BitSet.
	 */
	@Test
	public void testCopy() {
		sut = new BitSet( new java.util.Random().nextLong() );
		assertEquals( sut.getBits(), sut.copy().getBits() );
	}

	/**
	 * Test of union method, of class BitSet.
	 */
	@Test
	public void testUnion() {
		BitSet sut1 = randomSet();
		BitSet sut2 = randomSet();
		sut = sut1.copy().union(sut2);
		for ( short i : BitSet.allIndices() ) {
			if ( sut.isSet(i) ) {
				assertTrue( sut1.isSet(i) || sut2.isSet(i) );
			} else {
				assertFalse( sut1.isSet(i) && sut2.isSet(i) );
			}
		}
	}

	/**
	 * Test of intersect method, of class BitSet.
	 */
	@Test
	public void testIntersect() {
		BitSet sut1 = randomSet();
		BitSet sut2 = randomSet();
		sut = sut1.copy().intersect(sut2);
		for ( short i : BitSet.allIndices() ) {
			if ( sut.isSet(i) ) {
				assertTrue(sut1.isSet(i) && sut2.isSet(i), "expected true at idx " + i);
			} else {
				assertFalse(sut1.isSet(i) && sut2.isSet(i), "expected false at idx " + i);
			}
		}
	}

	/**
	 * Test of xor method, of class BitSet.
	 */
	@Test
	public void testXor() {
		BitSet sut1 = randomSet();
		BitSet sut2 = randomSet();
		sut = sut1.copy().xor(sut2);
		for ( short i : BitSet.allIndices() ) {
			if ( sut.isSet(i) ) {
				assertTrue(sut1.isSet(i) ^ sut2.isSet(i), "expected true at idx " + i);
			} else {
				assertFalse(sut1.isSet(i) ^ sut2.isSet(i), "expected false at idx " + i);
			}
		}
	}
	
	@Test
	public void testAsEnumSet() {
		EnumSet<TestEnum> est = EnumSet.of(TestEnum.Hello, TestEnum.This, TestEnum.Test);
		
		sut = BitSet.from(est);
		assertEquals( est, sut.asSetOf(TestEnum.class) );
	}
	
	/**
	 * Test of getBits method, of class BitSet.
	 */
	@Test
	public void testGetBits() {
		sut.set(0);
		sut.set(1);
		sut.set(2);
		assertEquals( 0b111, sut.getBits() );
	}
	
	
	private BitSet randomSet() {
		return new BitSet( new java.util.Random().nextLong() );
	}
}
