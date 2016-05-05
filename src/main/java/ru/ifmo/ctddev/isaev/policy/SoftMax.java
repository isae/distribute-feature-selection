package ru.ifmo.ctddev.isaev.policy;

import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * @author iisaev
 */
public class SoftMax extends BanditStrategy {
    private final double tau;

    public SoftMax(double tau, int arms) {
        super(arms);
        this.tau = tau;
        if (tau < 0 | tau > 1) {
            throw new IllegalArgumentException(String.format("Invalid temperature param: %f", tau));
        }
    }

    @Override
    public void processPoint(Function<Integer, Double> action) {
        int armNumber;
        double expSum = IntStream.range(0, arms)
                .mapToDouble(i -> Math.exp(visitedSum[i] / (visitedNumber[i] * tau))).sum();
        armNumber = IntStream.range(0, arms)
                .mapToObj(i -> i)
                .sorted(Comparator.comparingDouble(i -> Math.exp(visitedSum[i] / (visitedNumber[i] * tau * expSum))))
                .findFirst()
                .get();
        double result = action.apply(armNumber);
        ++visitedNumber[armNumber];
        visitedSum[armNumber] += result;
    }
}
