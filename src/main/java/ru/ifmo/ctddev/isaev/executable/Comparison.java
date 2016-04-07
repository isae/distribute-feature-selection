package ru.ifmo.ctddev.isaev.executable;

/**
 * @author iisaev
 */
public class Comparison {

    protected static double getSpeedImprovementPercent(long prevSeconds, long curSeconds) {
        long diff = prevSeconds - curSeconds;
        return (double) diff / prevSeconds * 100;
    }

    protected static double getPercentImprovement(double prev, double cur) {
        double diff = prev - cur;
        return diff / prev * 100;
    }

}
