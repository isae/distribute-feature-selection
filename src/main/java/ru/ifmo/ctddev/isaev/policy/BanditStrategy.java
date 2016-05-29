package ru.ifmo.ctddev.isaev.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;


/**
 * @author iisaev
 */
public abstract class BanditStrategy {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

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

    public double[] getVisitedSumCopy() {
        return Arrays.copyOf(holder.visitedSum, getArms());
    }


    public int[] getVisitedNumberCopy() {
        return Arrays.copyOf(holder.visitedNumber, getArms());
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

    public abstract void processPoint(Collection lastTries, Function<Integer, Optional<Double>> action);

    protected double mu(int i) {
        return holder.visitedSum[i] / holder.visitedNumber[i];
    }
}
