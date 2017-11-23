package core;

import interpreter.TestHierarchy;
import model.ZMI;

import java.text.ParseException;

public class ZMIConfig {

    public static ZMI getZMIConfiguration() throws ParseException, java.net.UnknownHostException {
        return TestHierarchy.createTestHierarchy();
    }

}
