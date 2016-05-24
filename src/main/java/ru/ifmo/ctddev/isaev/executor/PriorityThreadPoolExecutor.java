package ru.ifmo.ctddev.isaev.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;


/**
 * @author iisaev
 */
public class PriorityThreadPoolExecutor extends ThreadPoolExecutor {
    public PriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, PriorityBlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PriorityThreadPoolExecutor.class);
    @Override
    public Future<?> submit(Runnable task) {
        LOGGER.error("Do not use submit without priority");
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        LOGGER.error("Do not use submit without priority");
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        LOGGER.error("Do not use submit without priority");
        throw new UnsupportedOperationException();
    }

    public <T> Future<T> submit(Runnable task, T result, double priority) {
        if (task == null) {
            throw new IllegalArgumentException("Task must not be null");
        }
        RunnableFuture<T> future = newTaskFor(task, result, priority);
        execute(future);
        return future;
    }

    public <T> Future<T> submit(Callable<T> task, double priority) {
        if (task == null) {
            throw new IllegalArgumentException("Task must not be null");
        }
        RunnableFuture<T> future = newTaskFor(task, priority);
        execute(future);
        return future;
    }

    private <T> RunnableFuture<T> newTaskFor(Runnable runnable, T result, double priority) {
        return new PriorityFutureTask<>(runnable, result, priority);
    }

    private <T> RunnableFuture<T> newTaskFor(Callable<T> callable, double priority) {
        return new PriorityFutureTask<>(callable, priority);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        LOGGER.error("Do not use newTaskFor without priority");
        throw new UnsupportedOperationException();
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        LOGGER.error("Do not use newTaskFor without priority");
        throw new UnsupportedOperationException();
    }
}
