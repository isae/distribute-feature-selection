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

    private final int generation;

    public Point(double... coordinates) {
        this(0, coordinates);
    }

    public Point(int gen, double... coordinates) {
        this(gen, (d) -> {
        }, coordinates);
    }

    public Point(int gen, Consumer<double[]> consumer, double... coordinates) {
        this.coordinates = coordinates.clone();
        consumer.accept(this.coordinates);
        //double modulus = Math.sqrt(DoubleStream.of(coordinates).map(d -> d * d).sum());
        double modulus = DoubleStream.of(coordinates).sum();
        IntStream.range(0, coordinates.length).forEach(i -> this.coordinates[i] /= modulus); // normalization
        this.generation = gen;
    }

    public Point(Point point) {
        this(point.getCoordinates().clone());
    }

    public Point(Point point, Consumer<double[]> consumer) {
        this.coordinates = point.getCoordinates().clone();
        consumer.accept(coordinates);
        //double modulus = Math.sqrt(DoubleStream.of(coordinates).map(d -> d * d).sum());
        double modulus = DoubleStream.of(coordinates).sum();
        IntStream.range(0, coordinates.length).forEach(i -> this.coordinates[i] /= modulus); // normalization
        this.generation = point.generation + 1;
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
        return "[" + String.join(", ", doubles) + "]/" + (generation != 0 ? generation : "");
    }
}
