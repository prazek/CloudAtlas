package model;

public class ZoneChoiceStrategyFactory {

    public static ZoneChoiceStrategy getChoice(String strategyName) {
        if (strategyName.equals("RandomUniform"))
            return new UniformRandomZoneChoiceStrategy();
        if (strategyName.equals("RandomExponential"))
            return new ExponentialRandomZoneChoiceStrategy();
        if (strategyName.equals("RoundRobin"))
            return new RoundRobinZoneChoiceStrategy();
        throw new RuntimeException("Unknown strategy " + strategyName);
    }

}
