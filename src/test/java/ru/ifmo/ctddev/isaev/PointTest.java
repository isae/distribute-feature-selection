package ru.ifmo.ctddev.isaev;

import org.junit.Assert;
import org.junit.Test;
import ru.ifmo.ctddev.isaev.point.Point;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;


public class PointTest {
    private DataSetReader dataSetReader = new DataSetReader();

    @Test
    public void testDistinctPoints() {
        List<Point> points = Arrays.asList(
                new Point(37.0 / 29, 37.0 / 29),
                new Point(237.0 / 46, 37.0 / 29),
                new Point(37.0 / 29, 237.0 / 46),
                new Point(237.0 / 46, 237.0 / 46)
        );
        Assert.assertEquals("First and last are the same", 3, new TreeSet<>(points).size());
    }

    @Test
    public void testEqualPoints() {
        List<Point> points = Arrays.asList(
                new Point(17.0 / 15, 17.0 / 15),
                new Point(34.0000239 / 30, 17.0 / 15),
                new Point(51.0000239 / 45, 17.0 / 15),
                new Point(68.0000239 / 60, 17.0 / 15)
        );
        Assert.assertEquals("All points are equal", 1, new TreeSet<>(points).size());
    }
}
