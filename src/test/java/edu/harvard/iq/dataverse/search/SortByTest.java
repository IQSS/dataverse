package edu.harvard.iq.dataverse.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SortByTest {

    private String field;
    private String order;
    private SortBy instance;

    @BeforeEach
    public void setUp() {
        this.field = "field";
        this.order = SortBy.ASCENDING;
        this.instance = new SortBy(field, order);
    }

    @AfterEach
    public void tearDown() {
        this.field = null;
        this.order = null;
        this.instance = null;
    }

    @Test
    public void testToString() {
        String expected = "SortBy{field=" + this.field + ", order=" + this.order + "}";
        String actual = this.instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetField() {
        String expected = this.field;
        String actual = this.instance.getField();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetOrder() {
        String expected = this.order;
        String actual = this.instance.getOrder();
        assertEquals(expected, actual);
    }

    @Test
    public void testHashCodeIdentityOfSameObject() {
        // Whenever it is invoked on the same object more than once during an execution of
        // a Java application, the hashCode method must consistently return the same
        // integer, ...
        // according to:
        // https://docs.oracle.com/javase/7/donulli/java/lang/Object.html#hashCode()

        int firstHash = this.instance.hashCode();
        int secondHash = this.instance.hashCode();
        assertEquals(firstHash, secondHash);
    }

    @Test
    public void testHashCodeIdentityOfDifferentObjects() {
        // If two objects are equal according to the equals(Object) method, then calling
        // the hashCode method on each of the two objects must produce the same integer
        // result.
        // according to:
        // https://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#hashCode()

        SortBy secondInstance = new SortBy(this.field, this.order);

        int firstHash = this.instance.hashCode();
        int secondHash = secondInstance.hashCode();
        assertEquals(firstHash, secondHash);
    }

    @Test
    public void testEqualsWithSameObject() {
        assertTrue(this.instance.equals(this.instance));
    }

    @Test
    public void testEqualsWithNull() {
        assertFalse(this.instance.equals(null));
    }

    @Test
    public void testEqualsWithAnotherClass() {
        assertFalse(this.instance.equals(new String("some string")));
    }

    @Test
    public void testEqualsWithAnotherButEqualObject() {
        SortBy secondInstance = new SortBy(this.field, this.order);
        assertTrue(this.instance.equals(secondInstance));
    }

}
