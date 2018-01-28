package ru.ifmo.ctddev.isaev.melif.impl;

import kotlin.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSet;
import ru.ifmo.ctddev.isaev.LinearSearchPriorityBlockingQueue;
import ru.ifmo.ctddev.isaev.SelectionResult;
import ru.ifmo.ctddev.isaev.melif.MeLiF;
import ru.ifmo.ctddev.isaev.point.Point;
import ru.ifmo.ctddev.isaev.point.PriorityPoint;
import ru.ifmo.ctddev.isaev.results.RunStats;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;


/**
 * Implementation, that evaluates each point in separate thread
 *
 * @author iisaev
 */
public class MultiArmedBanditMeLiF extends FeatureSelectionAlgorithm implements MeLiF {
    private final ExecutorService executorService;

    private final int threads;

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiArmedBanditMeLiF.class);

    public ExecutorService getExecutorService() {
        return executorService;
    }

    protected final Set<PriorityPoint> visitedPoints = new ConcurrentSkipListSet<>();

    private final Map<Integer, LinearSearchPriorityBlockingQueue<PriorityPoint>> pointsQueues;

    private final Holder holder;

    private int tries = 0;

    public MultiArmedBanditMeLiF(AlgorithmConfig config, DataSet dataSet, int threads, int splitNumber) {
        super(config, dataSet);
        this.threads = threads;
        int dimension = config.getMeasures().length;

        Map<Integer, Point> spaces = generateStartingPoints(dimension, splitNumber);

        Map<Integer, LinearSearchPriorityBlockingQueue<PriorityPoint>> pointQueues = new HashMap<>();
        spaces.forEach((key, value) -> {
            LinearSearchPriorityBlockingQueue<PriorityPoint> queue = new LinearSearchPriorityBlockingQueue<>(64,
                    PriorityPoint::getPriority,
                    (point, increment) -> {
                        point.setPriority(point.getPriority() + increment);
                        return Unit.INSTANCE;
                    }
            );
            queue.offer(new PriorityPoint(value));
            pointQueues.put(key, queue);
        });
        this.pointsQueues = Collections.unmodifiableMap(pointQueues);
        this.executorService = Executors.newFixedThreadPool(threads);
        holder = new Holder(pointQueues.size());
    }

    public int getPointQueuesNumber() {
        return pointsQueues.size();
    }

    @Override
    public RunStats run(Point[] points) {
        return run("MultiArmedMeLiF", 75);
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
        points.forEach(point -> pointsMap.put(counter[0]++, point));

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
                .boxed()
                .flatMap(d -> {
                    List<List<Double>> dimBelow = generateSpaceWithFixedGrid(dimension - 1, splitParts);
                    return dimBelow.stream().peek(l -> l.add(d));
                }).collect(Collectors.toList());
    }

    private double mu(int i) {
        return holder.visitedSum[i] / holder.visitedNumber[i];
    }

    private double armCost(int i) {
        return mu(i) + sqrt(
                2 * log(tries) / (holder.visitedNumber[i])
        );
    }

    protected class Holder {
        final int[] visitedNumber;

        final double[] visitedSum;

        final int arms;

        Holder(int arms) {
            this.visitedNumber = new int[arms];
            this.visitedSum = new double[arms];
            this.arms = arms;
        }
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

            int arm;
            PriorityPoint point;
            synchronized (holder) {
                if (tries < holder.arms) {
                    arm = tries;
                    point = pointsQueues.get(arm).take();
                } else {
                    synchronized (pointsQueues) {
                        final Optional<Integer> bestArm = IntStream.range(0, holder.arms)
                                .filter(i -> !pointsQueues.get(i).isEmpty())
                                .boxed()
                                .max(Comparator.comparingDouble(MultiArmedBanditMeLiF.this::armCost));
                        if (!bestArm.isPresent()) {//no points in queue for now
                            logger.error("All queues are empty");
                            return;
                        }
                        arm = bestArm.get();
                        point = pointsQueues.get(arm).take();
                    }
                }
                ++tries;
            }
            if (point == null) {//no points in queue for now
                logger.error("Queue {} is empty", arm);
                return;
            }
            if (visitedPoints.contains(point)) {
                logger.warn("Point is already processed: {}", point);
                return;
            }

            logger.info("Processing point {} in queue {}", point, arm);
            SelectionResult res = foldsEvaluator.getSelectionResult(dataSet, point, runStats);
            visitedPoints.add(point);
            List<Point> neighbours = point.getNeighbours(config.getDelta());
            double award = res.getScore();
            neighbours.stream()
                    .map(p -> new PriorityPoint(award, p.getCoordinates()))
                    .filter(p -> !visitedPoints.contains(p))
                    .forEach(p -> pointsQueues.get(arm).offer(p));
            synchronized (holder) {
                ++holder.visitedNumber[arm];
                holder.visitedSum[arm] += award;
            }

            executorService.submit(new PointProcessingTask(stopCondition, runStats));
        }
    }

    @Override
    public RunStats run(String name, Point[] unused, int latchSize) {
        RunStats runStats = new RunStats(config, dataSet, name);
        logger.info("Started {} at {}", name, runStats.getStartTime());

        CountDownLatch latch = new CountDownLatch(latchSize);
        pointsQueues.values().forEach(queue -> executorService.submit(new PointProcessingTask(() -> {
            latch.countDown();
            if (runStats.getBestResult() != null && Math.abs(runStats.getBestResult().getScore() - 1.0) < 0.0001) {
                while (latch.getCount() != 0) {
                    latch.countDown();
                }
            }
            return latch.getCount() == 0;
        }, runStats)));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        executorService.shutdownNow();
        LOGGER.info("Max score: {} at point {}",
                runStats.getBestResult().getScore(),
                runStats.getBestResult().getPoint().getCoordinates()
        );
        runStats.setFinishTime(LocalDateTime.now());
        LOGGER.info("Finished {} at {}", getClass().getSimpleName(), runStats.getFinishTime());
        LOGGER.info("Working time: {} seconds", runStats.getWorkTime());
        return runStats;
    }

    public RunStats run(String name, int latchSize) {
        return run(name, null, latchSize);
    }


    public RunStats runUntilNoImproveOnLastN(String name, Point[] points, int lastN) {
        RunStats runStats = new RunStats(config, dataSet, name);
        logger.info("Started {} at {}", name, runStats.getStartTime());

        CountDownLatch latch = new CountDownLatch(1);
        pointsQueues.values().forEach(queue -> executorService.submit(new PointProcessingTask(() -> {
            if (runStats.getBestResult() != null && Math.abs(runStats.getBestResult().getScore() - 1.0) < 0.0001) {
                while (latch.getCount() != 0) {
                    latch.countDown();
                }
            }
            if (latch.getCount() == 0) {
                return true;
            }
            if (runStats.getNoImprove() > lastN) {
                latch.countDown();
                return true;
            } else {
                return false;
            }
        }, runStats)));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        executorService.shutdownNow();
        LOGGER.info("Max score: {} at point {}",
                runStats.getBestResult().getScore(),
                runStats.getBestResult().getPoint().getCoordinates()
        );
        runStats.setFinishTime(LocalDateTime.now());
        LOGGER.info("Finished {} at {}", getClass().getSimpleName(), runStats.getFinishTime());
        LOGGER.info("Working time: {} seconds", runStats.getWorkTime());
        return runStats;
    }
}
