package ru.ifmo.ctddev.isaev.melif;

import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;


/**
 * @author iisaev
 */
public interface MeLiF {
    RunStats run(Point[] points, RelevanceMeasure[] measures);
}
