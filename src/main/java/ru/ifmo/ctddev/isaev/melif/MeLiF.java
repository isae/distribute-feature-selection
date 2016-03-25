package ru.ifmo.ctddev.isaev.melif;

import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;


/**
 * @author iisaev
 */
public interface MeLiF {
    RunStats run(Point[] points);
}
