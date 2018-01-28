package ru.ifmo.ctddev.isaev.melif.impl;

import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSet;
import ru.ifmo.ctddev.isaev.SelectionResult;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;

import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;


/**
 * Very slow implementation that traverses all points recursively
 *
 * @author iisaev
 */
public class MeLifStar extends ParallelMeLiF {

    public MeLifStar(AlgorithmConfig config, DataSet dataSet, int threads) {
        super(config, dataSet, new ForkJoinPool(threads));
    }

    protected SelectionResult performCoordinateDescend(Point point, RunStats runStats) {
        SelectionResult result = foldsEvaluator.getSelectionResult(dataSet,point, runStats);
        visitedPoints.add(point);
        return processChildren(result, runStats);
    }

    private SelectionResult processChildren(SelectionResult parent, RunStats runStats) {
        List<Point> neighbours =parent.getPoint().getNeighbours(config.getDelta());
        CountDownLatch latch = new CountDownLatch(neighbours.size());
        Queue<SelectionResult> results = new ConcurrentLinkedQueue<>();
        neighbours.forEach(p -> {
            if (!visitedPoints.contains(p)) {
                visitedPoints.add(p);
                getExecutorService().submit(() -> {
                    results.add(foldsEvaluator.getSelectionResult(dataSet,p, runStats));
                    latch.countDown();
                });
            } else {
                latch.countDown();
            }
        });
        try {
            latch.await();
            return Stream.concat(
                    Stream.of(parent),
                    results.stream()
                            .filter(res -> res.getScore() > parent.getScore())
                            .map(ep -> processChildren(ep, runStats))
            ).min(Comparator.comparingDouble(SelectionResult::getScore)).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Operation was interrupted", e);
        }
    }
}
