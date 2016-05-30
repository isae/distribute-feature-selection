package ru.ifmo.ctddev.isaev.melif.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
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

import static java.lang.Math.log;
import static java.lang.Math.sqrt;


/**
 * Implementation, that evaluates each point in separate thread
 *
 * @author iisaev
 */
public class MultiArmedBanditMeLiF extends FeatureSelectionAlgorithm {
    private final ExecutorService executorService;

    private final int threads;

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiArmedBanditMeLiF.class);

    public ExecutorService getExecutorService() {
        return executorService;
    }

    protected final Set<Point> visitedPoints = new ConcurrentSkipListSet<>();

    private final Map<Integer, PriorityBlockingQueue<PriorityPoint>> pointsQueues;

    private final Holder holder;

    private int tries = 0;

    public MultiArmedBanditMeLiF(AlgorithmConfig config, DataSet dataSet, int threads, int splitNumber) {
        super(config, dataSet);
        this.threads = threads;
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
        holder = new Holder(pointQueues.size());
    }

    public int getPointQueuesNumber() {
        return pointsQueues.size();
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

    protected double mu(int i) {
        return holder.visitedSum[i] / holder.visitedNumber[i];
    }

    private double armCost(int i) {
        return mu(i) + sqrt(
                2 * log(tries) / (holder.visitedNumber[i])
        );
    }

    protected class Holder {
        protected final int[] visitedNumber;

        protected final double[] visitedSum;

        protected final int arms;

        public Holder(int arms) {
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
            if (runStats.getBestResult() != null && Math.abs(runStats.getBestResult().getF1Score() - 1.0) < 0.0001) {
                while (!stopCondition.get()) {
                    //do nothing
                }
                return;
            }
            if (stopCondition.get()) {
                logger.warn("Must stop; do nothing");
                return;
            }

            try {
                int arm;
                PriorityPoint point;
                synchronized (holder) {
                    if (tries < holder.arms) {
                        arm = tries;
                        point = pointsQueues.get(arm).poll(1, TimeUnit.MILLISECONDS);
                    } else {
                        synchronized (pointsQueues) {
                            arm = IntStream.range(0, holder.arms)
                                    .filter(i -> pointsQueues.get(i).size() != 0)
                                    .mapToObj(i -> i)
                                    .max(Comparator.comparingDouble(i -> armCost(i)))
                                    .get();
                            point = pointsQueues.get(arm).poll(1, TimeUnit.MILLISECONDS);
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
                List<Point> neighbours = getNeighbours(point);
                double award = res.getF1Score();
                neighbours.forEach(p -> {
                    if (!visitedPoints.contains(p)) {
                        pointsQueues.get(arm).add(new PriorityPoint(award, p.getCoordinates()));
                    }
                });
                synchronized (holder) {
                    ++holder.visitedNumber[arm];
                    holder.visitedSum[arm] += award;
                }
            } catch (InterruptedException ignored) {
                logger.warn("Queue poll is interrupted");
                return;
            }

            executorService.submit(new PointProcessingTask(stopCondition, runStats));
        }
    }

    public RunStats run(String name, int latchSize) {
        RunStats runStats = new RunStats(config, dataSet, name);
        logger.info("Started {} at {}", name, runStats.getStartTime());

        CountDownLatch latch = new CountDownLatch(latchSize);
        pointsQueues.values().forEach(queue -> executorService.submit(new PointProcessingTask(() -> {
            latch.countDown();
            return latch.getCount() == 0;
        }, runStats)));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        executorService.shutdownNow();
        LOGGER.info("Max score: {} at point {}",
                runStats.getBestResult().getF1Score(),
                runStats.getBestResult().getPoint().getCoordinates()
        );
        runStats.setFinishTime(LocalDateTime.now());
        LOGGER.info("Finished {} at {}", getClass().getSimpleName(), runStats.getFinishTime());
        LOGGER.info("Working time: {} seconds", runStats.getWorkTime());
        return runStats;
    }


    public RunStats run2(String name, int untilStop) {
        RunStats runStats = new RunStats(config, dataSet, name);
        logger.info("Started {} at {}", name, runStats.getStartTime());

        CountDownLatch latch = new CountDownLatch(1);
        pointsQueues.values().forEach(queue -> executorService.submit(new PointProcessingTask(() -> {
            if (latch.getCount() == 0) {
                return true;
            }
            if (runStats.getNoImprove() > untilStop) {
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
                runStats.getBestResult().getF1Score(),
                runStats.getBestResult().getPoint().getCoordinates()
        );
        runStats.setFinishTime(LocalDateTime.now());
        LOGGER.info("Finished {} at {}", getClass().getSimpleName(), runStats.getFinishTime());
        LOGGER.info("Working time: {} seconds", runStats.getWorkTime());
        return runStats;
    }
}
