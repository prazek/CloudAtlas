package model;

import com.google.gson.Gson;

import static org.junit.Assert.assertEquals;

public class CustomJsonSerializerTests {

    @org.junit.Test
    public void testRegexpr() throws Exception {
        ZMI zmi = new ZMI();
        zmi.getAttributes().add("abc", new ValueInt(42L));
        Gson gson = CustomJsonSerializer.getSerializer();

        String result = gson.toJson(zmi);
        assertEquals("{ \"type\"", result);
    }
}
