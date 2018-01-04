package model;

import java.util.Map;
import java.util.Random;

public class ExponentialRandomZoneChoiceStrategy implements ZoneChoiceStrategy {
    private Random random = new Random();
    @Override
    public PathName chooseZone(Map<PathName, ZMI> zmiMap, PathName current) {

        int height = current.getComponents().size();
        PathName chosenName = current;
        int i = 0;
        while (i < height) {
            if (random.nextBoolean())
                chosenName.levelUp();
            else
                break;
        }
        System.out.println("Chosen [" + chosenName + "] as base");
        return chosenName;
    }
}
