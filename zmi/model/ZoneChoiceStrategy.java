package model;

import java.util.Map;

// TODO make an interface and add more strategies
public class ZoneChoiceStrategy {

    public PathName chooseZone(Map<PathName, ZMI> zmiMap, PathName current) {
        ZMI zmi = zmiMap.get(new PathName("/uw/")); // TODO not always /uw/
        for (Map.Entry<PathName, ZMI> z: zmiMap.entrySet()) {
            if (z.getValue() == zmi) {
                return z.getKey();
            }
        }
        throw new RuntimeException("Zone not found");
    }
}
