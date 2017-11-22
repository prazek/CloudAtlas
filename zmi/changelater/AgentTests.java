package changelater;

import model.PathName;
import model.Value;
import model.ValueInt;


import static org.junit.Assert.*;

public class AgentTests {

    @org.junit.Test
    public void installQuery() throws Exception {
        Agent agent = new Agent(new PathName("/uw"));

        Value res1 = agent.zone(new PathName("/uw")).getAttributes().getOrNull("res1");
        assertNull(res1);

        agent.installQuery("&abc", "SELECT 42 AS res1");
        res1 = agent.zone(new PathName("/uw")).getAttributes().getOrNull("res1");
        assertEquals(res1, new ValueInt(42L));


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

