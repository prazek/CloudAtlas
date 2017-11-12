package model;

import static org.junit.Assert.*;

public class ValueDurationTest {

    @org.junit.Test
    public void testGetType() throws Exception {
        assertEquals(Type.PrimaryType.DURATION, new ValueDuration(42L).getType().getPrimaryType());
    }

    @org.junit.Test
    public void testGetDefaultValue() throws Exception {
        assertEquals(new ValueDuration(0L), new ValueDuration(42L).getDefaultValue());
    }

    @org.junit.Test
    public void testIsLowerThan() throws Exception {
        assertTrue(new ValueDuration(0L).isLowerThan(new ValueDuration(1L)).getValue());
        assertFalse(new ValueDuration(1L).isLowerThan(new ValueDuration(1L)).getValue());
        assertFalse(new ValueDuration(1L).isLowerThan(new ValueDuration(0L)).getValue());
    }

    @org.junit.Test
    public void testAddValue() throws Exception {
        assertEquals(new ValueDuration((1L)), new ValueDuration(0L).addValue(new ValueDuration(1L)));
        assertEquals(new ValueDuration((2L)), new ValueDuration(1L).addValue(new ValueDuration(1L)));
        assertEquals(new ValueDuration((3L)), new ValueDuration(1L).addValue(new ValueDuration(2L)));
        assertEquals(new ValueDuration((3L)), new ValueDuration(2L).addValue(new ValueDuration(1L)));
    }

    @org.junit.Test
    public void testSubtract() throws Exception {
        assertEquals(new ValueDuration((-1L)), new ValueDuration(0L).subtract(new ValueDuration(1L)));
        assertEquals(new ValueDuration((0L)), new ValueDuration(1L).subtract(new ValueDuration(1L)));
        assertEquals(new ValueDuration((-1L)), new ValueDuration(1L).subtract(new ValueDuration(2L)));
        assertEquals(new ValueDuration((1L)), new ValueDuration(2L).subtract(new ValueDuration(1L)));
    }

    @org.junit.Test
    public void testMultiply() throws Exception {

    }

    @org.junit.Test
    public void testDivide() throws Exception {

    }

    @org.junit.Test
    public void testModulo() throws Exception {

    }

    @org.junit.Test
    public void testNegate() throws Exception {
        assertEquals(new ValueDuration(-42L), new ValueDuration(42L).negate());
        assertEquals(new ValueDuration(0L), new ValueDuration(0L).negate());
    }

    @org.junit.Test
    public void testConvertTo() throws Exception {
        
    }
}
