package core;

import model.*;


import static org.junit.Assert.*;

public class AgentTests {

    @org.junit.Test
    public void installQuery() throws Exception {
        Agent agent = new Agent(new PathName("/uw"));

        Value res1 = agent.zone(new PathName("/uw")).getAttributes().getOrNull("res1");
        assertNull(res1);

        agent.installQuery("&abc", "SELECT 40 + 2 AS res1");
        res1 = agent.zone(new PathName("/uw")).getAttributes().getOrNull("res1");
        assertEquals(res1, new ValueInt(42L));

        AttributesMap queries = agent.getQueries();
        AttributesMap expected = new AttributesMap();
        expected.add("&abc", new ValueString("SELECT 40 + 2 AS res1"));
        assertEquals(expected.toString(), queries.toString());

        try {
            agent.installQuery("&new", "SELECT cpu_load AS x;");
            fail( "It should throw" );
        } catch (Exception ex) {
        }

        try {
            agent.installQuery("abc", "SELECT 42 AS res2");
            fail( "It should throw" );
        } catch (Exception ex) {
        }


        try {
            agent.installQuery("&abc", "SELECT 42 AS res2");
            fail( "It should throw" );
        } catch (Exception ex) {
        }

        try {
            agent.installQuery("&abc2", "SELECT 42");
            fail( "It should throw" );
        } catch (Exception ex) {
        }

        try {
            agent.installQuery("&abc3", "SELECT 42 AS res1");
            fail( "It should throw" );
        } catch (Exception ex) {
        }
        
    }

}


