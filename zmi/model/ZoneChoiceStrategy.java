package model;

import java.util.Map;
import java.util.Random;

// TODO make an interface and add more strategies
public class ZoneChoiceStrategy {
    Random random = new Random();

    public PathName chooseZone(Map<PathName, ZMI> zmiMap, PathName current) {

        int height = current.getComponents().size();
        int choosedLevel = random.nextInt(height- 1);
        PathName chosenName = current;
        for (int i = 0; i < choosedLevel; i++)
            chosenName.levelUp();

        System.out.println("Choosed [" + chosenName + "] as base");
        return chosenName;
    }
}
