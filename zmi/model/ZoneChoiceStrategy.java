package model;

import java.util.Map;

public interface ZoneChoiceStrategy {
    PathName chooseZone(Map<PathName, ZMI> zmiMap, PathName current);
}
