package ru.ifmo.ctddev.isaev.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;


/**
 * @author iisaev
 */
public class PriorityFutureTask<T> extends FutureTask<T> implements Runnable, Comparable<PriorityFutureTask<T>> {
    private final double priority;

    public PriorityFutureTask(Callable<T> callable, double priority) {
        super(callable);
        this.priority = priority;
    }

    @Override
    public int compareTo(PriorityFutureTask<T> other) {
        return -Double.compare(priority, other.priority);
    }

    public PriorityFutureTask(Runnable runnable, T result, double priority) {
        super(runnable, result);
        this.priority = priority;
    }
}
