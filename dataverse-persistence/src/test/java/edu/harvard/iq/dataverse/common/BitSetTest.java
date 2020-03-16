/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author michael
 */
public class BitSetTest {

    enum TestEnum {
        Hello, World, This, Is, A, Test
    }

    private BitSet sut;

    @BeforeEach
    public void setUp() {
        sut = new BitSet();
    }

    @Test
    public void set() {
        for(short i : BitSet.allIndices()) {
            sut.set(i);
            assertTrue(sut.isSet(i));
        }
    }

    @Test
    public void set_byParameter() {
        BitSet tSut = BitSet.emptySet();
        List<Integer> indices = Arrays.asList(0, 1, 4, 6, 7, 8, 20, 31);
        indices.forEach(i -> assertFalse(tSut.isSet(i)));
        indices.forEach(i -> tSut.set(i, true));
        indices.forEach(i -> assertTrue(tSut.isSet(i)));
        indices.forEach(i -> tSut.set(i, false));
        indices.forEach(i -> assertFalse(tSut.isSet(i)));
        assertTrue(tSut.isEmpty());
    }

    @Test
    public void unset() {
        sut = BitSet.fullSet();
        for (short i : BitSet.allIndices()) {
            sut.unset(i);
            assertFalse(sut.isSet(i));
        }
    }

    @Test
    public void copy() {
        sut = new BitSet(new java.util.Random().nextLong());
        assertEquals(sut.getBits(), sut.copy().getBits());
    }

    @Test
    public void union() {
        //GIVEN
        BitSet sut1 = randomSet();
        BitSet sut2 = randomSet();

        //WHEN
        sut = sut1.copy().union(sut2);

        //THEN
        for (short i : BitSet.allIndices()) {
            assertEquals(sut1.isSet(i) || sut2.isSet(i), sut.isSet(i));
        }
    }

    @Test
    public void intersect() {
        //GIVEN
        BitSet sut1 = randomSet();
        BitSet sut2 = randomSet();

        //WHEN
        sut = sut1.copy().intersect(sut2);

        //THEN
        for (short i : BitSet.allIndices()) {
            assertEquals(sut1.isSet(i) && sut2.isSet(i), sut.isSet(i));
        }
    }

    @Test
    public void xor() {
        //GIVEN
        BitSet sut1 = randomSet();
        BitSet sut2 = randomSet();

        //WHEN
        sut = sut1.copy().xor(sut2);

        //THEN
        for (short i : BitSet.allIndices()) {
            assertEquals(sut1.isSet(i) ^ sut2.isSet(i), sut.isSet(i));
        }
    }

    @Test
    public void from_enumSet() {
        EnumSet<TestEnum> est = EnumSet.of(TestEnum.Hello, TestEnum.This, TestEnum.Test);

        sut = BitSet.from(est);
        assertEquals(est, sut.asSetOf(TestEnum.class));
    }

    @Test
    public void getBits() {
        //GIVEN
        sut.set(0);
        sut.set(1);
        sut.set(2);

        //WHEN & THEN
        assertEquals(0b111, sut.getBits());
    }

    private BitSet randomSet() {
        return new BitSet(new java.util.Random().nextLong());
    }
}
