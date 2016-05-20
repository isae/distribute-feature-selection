package ru.ifmo.ctddev.isaev.executor;

import java.util.concurrent.*;


/**
 * @author iisaev
 */
public class PriorityThreadPoolExecutor extends ThreadPoolExecutor {
    public PriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, PriorityBlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    public Future<?> submit(Runnable task) {
        throw new UnsupportedOperationException("Use overloads with priority");
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        throw new UnsupportedOperationException("Use overloads with priority");
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        throw new UnsupportedOperationException("Use overloads with priority");
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
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        throw new UnsupportedOperationException("Use overloads with priority");
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        throw new UnsupportedOperationException("Use overloads with priority");
    }
}
