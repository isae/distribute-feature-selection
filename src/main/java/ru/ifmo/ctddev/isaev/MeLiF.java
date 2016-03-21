package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;


/**
 * @author iisaev
 */
public interface MeLiF {
    RunStats run(Point[] points, RelevanceMeasure[] measures);
}
