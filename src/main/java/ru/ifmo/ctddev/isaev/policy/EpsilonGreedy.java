package ru.ifmo.ctddev.isaev.policy;

import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * @author iisaev
 */
public class EpsilonGreedy implements BanditStrategy {
    private final double epsilon;
    private static final Random RANDOM = new Random();
    private final int[] visitedNumber;
    private final double[] visitedSum;

    public EpsilonGreedy(double epsilon, int arms) {
        this.epsilon = epsilon;
        if (epsilon < 0 | epsilon > 1) {
            throw new IllegalArgumentException(String.format("Invalid epsilon: %f", epsilon));
        }
        if (arms <= 0) {
            throw new IllegalArgumentException(String.format("Invalid arms: %d", arms));
        }
        this.visitedNumber = new int[arms];
        this.visitedSum = new double[arms];
    }


    @Override
    public void processPoint(Function<Integer, Double> action) {
        int armNumber;
        if (RANDOM.nextDouble() < epsilon) {
            armNumber = RANDOM.nextInt(visitedSum.length);
        } else {
            armNumber = IntStream.range(0, visitedNumber.length)
                    .mapToObj(i -> i)
                    .sorted(Comparator.comparingDouble(i -> visitedSum[i] / visitedNumber[i]))
                    .findFirst()
                    .get();
        }
        double result = action.apply(armNumber);
        ++visitedNumber[armNumber];
        visitedSum[armNumber] += result;
    }
}
