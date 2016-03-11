package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.dataset.DataSetPair;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author iisaev
 */
public class ParallelMeLiF extends SimpleMeLiF {
    private final ExecutorService executorService;

    protected final Set<Point> visitedPoints = new ConcurrentSkipListSet<>();

    public ParallelMeLiF(AlgorithmConfig config, int threads) {
        super(config);
        executorService = Executors.newFixedThreadPool(threads);
    }

    @Override
    protected Double visitPoint(Point point, double baseScore) {
        if (!visitedPoints.contains(point)) {
            try {
                double score = executorService.submit(() -> {
                    visitedPoints.add(new Point(point));
                    return getF1Score(point);
                }).get();
                if (score > baseScore) {
                    return score;
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException("Exception from executor service", e);
            }
        }
        return null;
    }

    @Override
    protected double getF1Score(DataSetPair dsPair) {
        try {
            return executorService.submit(() -> super.getF1Score(dsPair)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Exception from executor service", e);
        }
    }
}
