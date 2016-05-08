package ru.ifmo.ctddev.isaev.policy;

import java.util.function.Function;

/**
 * @author iisaev
 */
public class UCBTuned extends BanditStrategy {

    private int tries = 0;

    public UCBTuned(int arms) {
        super(arms);
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public void processPoint(Function<Integer, Double> action) {
    }
}
