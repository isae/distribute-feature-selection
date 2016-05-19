package ru.ifmo.ctddev.isaev.melif.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.PriorityPoint;
import ru.ifmo.ctddev.isaev.result.RunStats;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Implementation, that evaluates each point in separate thread
 *
 * @author iisaev
 */
public class MultiArmedBanditMeLiF extends FeatureSelectionAlgorithm implements Runnable {
    private final ExecutorService executorService;

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiArmedBanditMeLiF.class);

    public ExecutorService getExecutorService() {
        return executorService;
    }

    protected final Set<Point> visitedPoints = new ConcurrentSkipListSet<>();

    private final Map<Integer, Point> spaces;

    private final Map<Integer, PriorityBlockingQueue<PriorityPoint>> pointsQueues;

    public MultiArmedBanditMeLiF(AlgorithmConfig config, DataSet dataSet, int threads, int splitNumber) {
        this(config, dataSet, Executors.newFixedThreadPool(threads), splitNumber);
    }

    public MultiArmedBanditMeLiF(AlgorithmConfig config, DataSet dataSet, ExecutorService executorService, int splitNumber) {
        super(config, dataSet);
        int dimension = config.getMeasures().length;
        this.executorService = executorService;
        Set<Point> points = new TreeSet<>();

        double[] allEqual = new double[dimension];
        Arrays.fill(allEqual, 1.0);
        points.add(new Point(allEqual));

        IntStream.range(0, dimension).forEach(dim -> {
            double[] coordinates = new double[dimension];
            coordinates[dim] = 1.0;
            points.add(new Point(coordinates));
        });

        List<List<Double>> space = generateSpaceWithFixedGrid(dimension, splitNumber);
        space.stream()
                .map(l -> new Point(l.stream().mapToDouble(d -> d).toArray()))
                .forEach(points::add);

        final int[] counter = {0};
        Map<Integer, Point> pointsMap = new TreeMap<>();
        points.stream().forEach(point -> pointsMap.put(counter[0]++, point));

        this.spaces = Collections.unmodifiableMap(Collections.synchronizedMap(pointsMap));

        Map<Integer, PriorityBlockingQueue<PriorityPoint>> pointQueues = new HashMap<>();
        Comparator<PriorityPoint> priorityComparator = (o1, o2) -> (int) Math.signum(o1.getPriority() - o2.getPriority());
        spaces.entrySet().stream().forEach(e -> {
            PriorityBlockingQueue<PriorityPoint> queue = new PriorityBlockingQueue<>(10, priorityComparator);
            queue.put(new PriorityPoint(e.getValue()));
            pointQueues.put(e.getKey(), queue);
        });
        this.pointsQueues = Collections.unmodifiableMap(pointQueues);
    }

    private List<List<Double>> generateSpaceWithFixedGrid(int dimension, int splitParts) {
        if (dimension == 0) {
            List<List<Double>> result = new ArrayList<>();
            IntStream.range(0, splitParts).forEach(i -> result.add(new ArrayList<>()));
            return result;
        }
        double gridStep = 1.0 / splitParts;
        return IntStream.range(0, splitParts)
                .mapToDouble(i -> 1.0 - gridStep * i)
                .mapToObj(d -> d)
                .flatMap(d -> {
                    List<List<Double>> dimBelow = generateSpaceWithFixedGrid(dimension - 1, splitParts);
                    return dimBelow.stream().map(l -> {
                        l.add(d);
                        return l;
                    });
                }).collect(Collectors.toList());
    }


    public RunStats run(boolean shutdown) {
        return null;
    }

    public void run() {
        run(true);
    }
}
