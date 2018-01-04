package model;


import java.util.Map;

public class RoundRobinZoneChoiceStrategy implements ZoneChoiceStrategy {
    int currentLevel = 0;

    @Override
    public PathName chooseZone(Map<PathName, ZMI> zmiMap, PathName current) {
        int height = current.getComponents().size();
        if (currentLevel++ == height - 1)
            currentLevel = 0;

        PathName chosenName = current;
        for (int i = 0; i < currentLevel; i++)
            chosenName.levelUp();

        System.out.println("Chosen [" + chosenName + "] as base");
        return chosenName;
    }
}
