package model;

import static org.junit.Assert.*;

public class ValueStringTest {

    @org.junit.Test
    public void testRegexpr() throws Exception {
        assertTrue(new ValueString("beatkaxx").regExpr(new ValueString("([a-z]*)")).getValue());
        assertTrue(new ValueString("beatkaxx").regExpr(new ValueString("([a-z]*)atkax([a-z]*)")).getValue());
    }

}
