package ru.ifmo.ctddev.isaev.result;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * @author iisaev
 */
public class Point implements Comparable<Point> {
    private final double[] coordinates;

    public Point(double... coordinates) {
        this.coordinates = coordinates.clone();
        double sum = DoubleStream.of(coordinates).sum();
        IntStream.range(0, coordinates.length).forEach(i -> this.coordinates[i] /= sum); // normalization
    }

    public Point(Point point) {
        this(point.getCoordinates().clone());
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    @Override
    public int compareTo(Point other) {
        for (int i = 0; i < coordinates.length; i++) {
            if (coordinates[i] - other.coordinates[i] > 0.001) {
                return 1;
            }
            if (coordinates[i] - other.coordinates[i] < -0.001) {
                return -1;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return Arrays.toString(coordinates);
    }
}
