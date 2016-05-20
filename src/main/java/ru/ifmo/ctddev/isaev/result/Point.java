package ru.ifmo.ctddev.isaev.result;

import ru.ifmo.ctddev.isaev.melif.impl.FeatureSelectionAlgorithm;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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

    public Point(Point point, Consumer<double[]> consumer) {
        this(point);
        consumer.accept(coordinates);
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
        List<String> doubles = DoubleStream.of(coordinates).mapToObj(FeatureSelectionAlgorithm.FORMAT::format).collect(Collectors.toList());
        return "[" + String.join(", ", doubles) + "]";
    }
}
