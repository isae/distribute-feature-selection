package ru.ifmo.ctddev.isaev.melif.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.policy.BanditStrategy;
import ru.ifmo.ctddev.isaev.policy.UCB1;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.PriorityPoint;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.result.SelectionResult;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Implementation, that evaluates each point in separate thread
 *
 * @author iisaev
 */
public class MultiArmedBanditMeLiF extends FeatureSelectionAlgorithm {
    private final ExecutorService executorService;

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiArmedBanditMeLiF.class);

    private final BanditStrategy banditStrategy;

    public ExecutorService getExecutorService() {
        return executorService;
    }

    protected final Set<Point> visitedPoints = new ConcurrentSkipListSet<>();

    private final Map<Integer, PriorityBlockingQueue<PriorityPoint>> pointsQueues;

    public MultiArmedBanditMeLiF(AlgorithmConfig config, DataSet dataSet, int threads, int splitNumber) {
        super(config, dataSet);
        int dimension = config.getMeasures().length;

        Map<Integer, Point> spaces = generateStartingPoints(dimension, splitNumber);

        Map<Integer, PriorityBlockingQueue<PriorityPoint>> pointQueues = new HashMap<>();
        Comparator<PriorityPoint> priorityComparator = (o1, o2) -> (int) -Math.signum(o1.getPriority() - o2.getPriority());
        spaces.entrySet().stream().forEach(e -> {
            PriorityBlockingQueue<PriorityPoint> queue = new PriorityBlockingQueue<>(10, priorityComparator);
            queue.put(new PriorityPoint(e.getValue()));
            pointQueues.put(e.getKey(), queue);
        });
        this.pointsQueues = Collections.unmodifiableMap(pointQueues);
        this.executorService = Executors.newFixedThreadPool(threads);
        this.banditStrategy = new UCB1(pointQueues.size(), 1.0);
    }

    private Map<Integer, Point> generateStartingPoints(int dimension, int splitNumber) {
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
        Map<Integer, Point> pointsMap = new LinkedHashMap<>();
        points.stream().forEach(point -> pointsMap.put(counter[0]++, point));

        return Collections.unmodifiableMap(pointsMap);
    }

    private List<List<Double>> generateSpaceWithFixedGrid(int dimension, int splitParts) {
        if (dimension == 0) {
            List<List<Double>> result = new ArrayList<>();
            IntStream.range(0, splitParts).forEach(i -> result.add(new ArrayList<>()));
            return result;
        }
        double gridStep = 1.0 / splitParts; // from -1 to 1
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

    private List<Point> getNeighbours(Point point) {
        List<Point> points = new ArrayList<>();
        IntStream.range(0, config.getMeasures().length).forEach(i -> {
            Point plusDelta = new Point(point, coords -> coords[i] += config.getDelta());
            Point minusDelta = new Point(point, coords -> coords[i] -= config.getDelta());
            points.add(plusDelta);
            points.add(minusDelta);
        });
        return points;
    }

    class PointProcessingTask implements Runnable {

        Supplier<Boolean> stopCondition;

        RunStats runStats;

        public PointProcessingTask(Supplier<Boolean> stopCondition, RunStats runStats) {
            this.stopCondition = stopCondition;
            this.runStats = runStats;
        }

        @Override
        public void run() {
            if (stopCondition.get()) {
                logger.warn("Must stop; do nothing");
                return;
            }
            banditStrategy.processPoint(i -> {
                logger.warn("Requested point from queue {};\n {} visited", new Object[] {
                        i,
                        IntStream.range(0, banditStrategy.getArms()).mapToObj(j ->
                                "" + banditStrategy.getVisitedSum()[j] + "/" + banditStrategy.getVisitedNumber()[j]
                        ).map(FeatureSelectionAlgorithm.FORMAT::format).toArray()
                });
                PriorityBlockingQueue<PriorityPoint> queue = pointsQueues.get(i);
                try {
                    PriorityPoint point = queue.poll(1, TimeUnit.MILLISECONDS);
                    if (point == null) {//no points in queue for now
                        logger.warn("Queue {} is empty", i);
                        return Optional.empty();
                    }
                    if (visitedPoints.contains(point)) {
                        logger.warn("Point is already processed: {}", point);
                        return Optional.empty();
                    }

                    logger.info("Processing point {}", point);
                    SelectionResult res = getSelectionResult(point, runStats);
                    visitedPoints.add(point);
                    List<Point> neighbours = getNeighbours(point);
                    double award = res.getF1Score();
                    neighbours.forEach(p -> queue.add(new PriorityPoint(award, p.getCoordinates())));
                    return Optional.of(award);
                } catch (InterruptedException ignored) {
                    logger.warn("Queue poll is interrupted");
                    return Optional.empty();
                }
            });
            executorService.submit(new PointProcessingTask(stopCondition, runStats));
        }
    }

    public RunStats run() {
        return run(true);
    }

    public RunStats run(boolean shutdown) {
        RunStats runStats = new RunStats(config, dataSet, "MultiArmedBandit");

        CountDownLatch latch = new CountDownLatch(100);
        pointsQueues.values().forEach(queue -> executorService.submit(new PointProcessingTask(() -> {
            latch.countDown();
            return latch.getCount() == 0;
        }, runStats)));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        if (shutdown) {
            executorService.shutdownNow();
        }
        runStats.setFinishTime(LocalDateTime.now());
        return runStats;
    }
}
