package model;

import com.google.gson.Gson;
import interpreter.TestHierarchy;

import static org.junit.Assert.assertEquals;

public class CustomJsonSerializerTests {

    @org.junit.Test
    public void testSimple() throws Exception {
        ZMI zmi = new ZMI();
        zmi.getAttributes().add("abc", new ValueInt(42L));
        Gson gson = CustomJsonSerializer.getSerializer();

        String result = gson.toJson(zmi);
        assertEquals("{\n" +
                "  \"type\": \"ZMI\",\n" +
                "  \"attributes\": {\n" +
                "    \"type\": \"AttributesMap\",\n" +
                "    \"values\": {\n" +
                "      \"abc\": {\n" +
                "        \"type\": \"ValueInt\",\n" +
                "        \"value\": 42\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", result);
    }

    @org.junit.Test
    public void testComplex() throws Exception {
        ZMI zmi = TestHierarchy.createTestHierarchy();
        Gson gson = CustomJsonSerializer.getSerializer();

        String result = gson.toJson(zmi);
        assertEquals("{\n" +
                "  \"type\": \"ZMI\",\n" +
                "  \"attributes\": {\n" +
                "    \"type\": \"AttributesMap\",\n" +
                "    \"values\": {\n" +
                "      \"owner\": {\n" +
                "        \"type\": \"ValueString\",\n" +
                "        \"value\": \"/uw/violet07\"\n" +
                "      },\n" +
                "      \"level\": {\n" +
                "        \"type\": \"ValueInt\",\n" +
                "        \"value\": 0\n" +
                "      },\n" +
                "      \"name\": {\n" +
                "        \"type\": \"ValueString\"\n" +
                "      },\n" +
                "      \"contacts\": {\n" +
                "        \"type\": \"ValueSet\",\n" +
                "        \"value\": []\n" +
                "      },\n" +
                "      \"cardinality\": {\n" +
                "        \"type\": \"ValueInt\",\n" +
                "        \"value\": 0\n" +
                "      },\n" +
                "      \"timestamp\": {\n" +
                "        \"type\": \"ValueTime\",\n" +
                "        \"value\": 1352488217342\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", result);
    }

}
