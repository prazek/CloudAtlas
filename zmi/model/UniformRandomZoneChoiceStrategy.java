package model;

import java.util.Map;
import java.util.Random;

public class UniformRandomZoneChoiceStrategy implements ZoneChoiceStrategy {
    Random random = new Random();

    @Override
    public PathName chooseZone(Map<PathName, ZMI> zmiMap, PathName current) {

        int height = current.getComponents().size();
        int choosedLevel = random.nextInt(height- 1);
        PathName chosenName = current;
        for (int i = 0; i < choosedLevel; i++)
            chosenName.levelUp();

        System.out.println("Chosen [" + chosenName + "] as base");
        return chosenName;
    }
}
