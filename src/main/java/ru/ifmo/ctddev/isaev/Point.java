package ru.ifmo.ctddev.isaev;

/**
 * @author iisaev
 */
public class Point implements Comparable<Point> {
    private final double[] coordinates;

    public Point(double... coordinates) {
        this.coordinates = coordinates;
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
}
