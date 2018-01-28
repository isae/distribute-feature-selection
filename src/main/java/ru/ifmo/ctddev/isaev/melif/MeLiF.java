package ru.ifmo.ctddev.isaev.melif;

import ru.ifmo.ctddev.isaev.point.RunStats;
import ru.ifmo.ctddev.isaev.point.Point;


/**
 * @author iisaev
 */
public interface MeLiF {
    RunStats run(Point[] points);

    RunStats run(String name, Point[] points, int pointsToVisit);

    default RunStats runUntilNoImproveOnLastN(String name, Point[] points, int lastN) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
