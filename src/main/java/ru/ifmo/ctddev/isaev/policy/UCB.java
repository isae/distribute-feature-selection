package ru.ifmo.ctddev.isaev.policy;

import java.util.function.Function;

/**
 * @author iisaev
 */
public class UCB extends BanditStrategy {

    public UCB(int arms) {
        super(arms);
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void processPoint(Function<Integer, Double> action) {
        throw new IllegalArgumentException("Not implemented");
    }
}
