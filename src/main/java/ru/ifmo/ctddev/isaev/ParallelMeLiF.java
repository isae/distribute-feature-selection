package ru.ifmo.ctddev.isaev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.dataset.DataSetPair;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.dataset.InstanceDataSet;
import ru.ifmo.ctddev.isaev.feature.RelevanceMeasure;

import java.util.*;
import java.util.concurrent.*;


/**
 * @author iisaev
 */
public class ParallelMeLiF extends SimpleMeLiF {
    private final ExecutorService executorService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelMeLiF.class);

    public ExecutorService getExecutorService() {
        return executorService;
    }

    protected final Set<Point> visitedPoints = new ConcurrentSkipListSet<>();

    public ParallelMeLiF(AlgorithmConfig config, int threads) {
        super(config);
        executorService = Executors.newFixedThreadPool(threads);
    }

    protected double performCoordinateDescend(Point point, RelevanceMeasure[] measures) {
        double baseScore = getF1Score(point, measures);
        visitedPoints.add(new Point(point));

        boolean smthChanged = true;

        while (smthChanged) {
            smthChanged = false;
            double[] coordinates = point.getCoordinates();
            for (int i = 0; i < coordinates.length; i++) {
                final double finalBaseScore = baseScore;

                Point plusDelta = new Point(point);
                plusDelta.getCoordinates()[i] += config.getDelta();
                final Double[] plusDeltaScore = new Double[1];
                CountDownLatch latch = new CountDownLatch(2);
                executorService.submit(() -> {
                    plusDeltaScore[0] = visitPoint(plusDelta, measures, finalBaseScore);
                    latch.countDown();
                });

                Point minusDelta = new Point(point);
                minusDelta.getCoordinates()[i] -= config.getDelta();
                final Double[] minusDeltaScore = new Double[1];
                executorService.submit(() -> {
                    minusDeltaScore[0] = visitPoint(minusDelta, measures, finalBaseScore);
                    latch.countDown();
                });

                try {
                    latch.await();
                    if (plusDeltaScore[0] != null) {
                        baseScore = plusDeltaScore[0];
                        smthChanged = true;
                        break;
                    }
                    if (minusDeltaScore[0] != null) {
                        baseScore = minusDeltaScore[0];
                        smthChanged = true;
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new IllegalArgumentException("Waiting on latch interrupted!");
                }
            }
        }
        return baseScore;
    }

    protected double getF1Score(Point point, RelevanceMeasure[] measures) {
        FeatureDataSet filteredDs = datasetFilter.filterDataset(config.getInitialDataset().toFeatureSet(), config.getFeatureCount(), point, measures);
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<DataSetPair> dataSetPairs = datasetSplitter.splitRandomly(instanceDataSet, config.getTestPercent(), config.getFolds());
        CountDownLatch latch = new CountDownLatch(dataSetPairs.size());
        List<Double> f1Scores = Collections.synchronizedList(new ArrayList<>(dataSetPairs.size()));
        dataSetPairs.forEach(ds -> {
            executorService.submit(() -> {
                double score = getF1Score(ds);
                f1Scores.add(score);
                latch.countDown();
            });
        });
        try {
            latch.await();
            double result = f1Scores.stream().mapToDouble(d -> d).average().getAsDouble();
            LOGGER.debug("Point {}; F1 score: {}", Arrays.toString(point.getCoordinates()), result);
            return result;
        } catch (InterruptedException e) {
            throw new IllegalStateException("Waiting on latch interrupted! ", e);
        }
    }
}
