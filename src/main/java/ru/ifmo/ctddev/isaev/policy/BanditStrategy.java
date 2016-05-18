package ru.ifmo.ctddev.isaev.policy;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.Function;

/**
 * @author iisaev
 */
@NotThreadSafe
public abstract class BanditStrategy {
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

    private final Holder holder;

    public int[] getVisitedNumber() {
        return holder.visitedNumber;
    }

    public double[] getVisitedSum() {
        return holder.visitedSum;
    }

    public int getArms() {
        return holder.arms;
    }

    public Holder getHolder() {
        return holder;
    }

    public BanditStrategy(int arms) {
        if (arms <= 0) {
            throw new IllegalArgumentException(String.format("Invalid arms: %d", arms));
        }

        holder = new Holder(arms);
    }

    public abstract void processPoint(Function<Integer, Double> action);

    protected double mu(int i) {
        return holder.visitedSum[i] / holder.visitedNumber[i];
    }
}
