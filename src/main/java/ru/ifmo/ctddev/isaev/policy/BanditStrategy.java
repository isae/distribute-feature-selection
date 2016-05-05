package ru.ifmo.ctddev.isaev.policy;

import java.util.function.Function;

/**
 * @author iisaev
 */
public abstract class BanditStrategy {

    protected final int[] visitedNumber;
    protected final double[] visitedSum;
    protected final int arms;

    public BanditStrategy(int arms) {
        this.arms = arms;
        if (arms <= 0) {
            throw new IllegalArgumentException(String.format("Invalid arms: %d", arms));
        }
        this.visitedNumber = new int[arms];
        this.visitedSum = new double[arms];
    }

    public abstract void processPoint(Function<Integer, Double> action);
}
