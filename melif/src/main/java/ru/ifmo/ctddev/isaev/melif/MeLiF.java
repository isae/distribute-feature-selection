package ru.ifmo.ctddev.isaev.melif;

import org.jetbrains.annotations.NotNull;
import ru.ifmo.ctddev.isaev.point.Point;
import ru.ifmo.ctddev.isaev.results.RunStats;


/**
 * @author iisaev
 */
public interface MeLiF {
    RunStats run(@NotNull Point[] points);

    RunStats run(@NotNull String name, @NotNull Point[] points, int pointsToVisit);

    default RunStats runUntilNoImproveOnLastN(String name, Point[] points, int lastN) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
