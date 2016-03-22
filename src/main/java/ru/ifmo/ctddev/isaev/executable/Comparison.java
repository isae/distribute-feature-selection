package ru.ifmo.ctddev.isaev.executable;

/**
 * @author iisaev
 */
public class Comparison {

    protected static double getSpeedImprovementPercent(long prevSeconds, long curSeconds) {
        long diff = prevSeconds - curSeconds;
        return (double) diff / prevSeconds * 100;
    }

}
