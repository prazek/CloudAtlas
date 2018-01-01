package core;

import model.*;


import javax.xml.crypto.Data;

import static org.junit.Assert.*;

public class AgentTests {

    @org.junit.Test
    public void installQuery() throws Exception {
        Agent agent = new Agent(new PathName("/uw"));
        DatabaseService database = new DatabaseService(agent, null, null);

        Value res1 = database.zone(new PathName("/uw")).getAttributes().getOrNull("res1");
        assertNull(res1);

        database.installQuery("&abc", "SELECT 40 + 2 AS res1");
        res1 = database.zone(new PathName("/uw")).getAttributes().getOrNull("res1");
        assertEquals(res1, new ValueInt(42L));

        AttributesMap queries = database.getQueries();
        AttributesMap expected = new AttributesMap();
        expected.add("&abc", new ValueString("SELECT 40 + 2 AS res1"));
        assertEquals(expected.toString(), queries.toString());

        try {
            database.installQuery("&new", "SELECT cpu_load AS x;");
            fail( "It should throw" );
        } catch (Exception ex) {
        }

        try {
            database.installQuery("abc", "SELECT 42 AS res2");
            fail( "It should throw" );
        } catch (Exception ex) {
        }


        try {
            database.installQuery("&abc", "SELECT 42 AS res2");
            fail( "It should throw" );
        } catch (Exception ex) {
        }

        try {
            database.installQuery("&abc2", "SELECT 42");
            fail( "It should throw" );
        } catch (Exception ex) {
        }

        try {
            database.installQuery("&abc3", "SELECT 42 AS res1");
            fail( "It should throw" );
        } catch (Exception ex) {
        }
        
    }

}


