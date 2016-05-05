package ru.ifmo.ctddev.isaev.policy;

import java.util.function.Function;

/**
 * @author iisaev
 */
public interface BanditStrategy {
    void processPoint(Function<Integer, Double> action);
}
