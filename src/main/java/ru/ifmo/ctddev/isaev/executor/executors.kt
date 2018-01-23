package ru.ifmo.ctddev.isaev.executor

import org.slf4j.LoggerFactory
import java.util.concurrent.*

/**
 * @author iisaev
 */
class PriorityFutureTask<T> : FutureTask<T>, Runnable, Comparable<PriorityFutureTask<T>> {
    private val priority: Double

    constructor(callable: Callable<T>, priority: Double) : super(callable) {
        this.priority = priority
    }

    constructor(runnable: Runnable, result: T, priority: Double) : super(runnable, result) {
        this.priority = priority
    }

    override fun compareTo(other: PriorityFutureTask<T>): Int {
        return -java.lang.Double.compare(priority, other.priority)
    }
}

private val LOGGER = LoggerFactory.getLogger(PriorityThreadPoolExecutor::class.java)

class PriorityThreadPoolExecutor(corePoolSize: Int, maximumPoolSize: Int, keepAliveTime: Long, unit: TimeUnit, workQueue: PriorityBlockingQueue<Runnable>) : ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue) {
    override fun submit(task: Runnable): Future<*> {
        LOGGER.error("Do not use submit without priority")
        throw UnsupportedOperationException()
    }

    override fun <T> submit(task: Runnable, result: T): Future<T> {
        LOGGER.error("Do not use submit without priority")
        throw UnsupportedOperationException()
    }

    override fun <T> submit(task: Callable<T>): Future<T> {
        LOGGER.error("Do not use submit without priority")
        throw UnsupportedOperationException()
    }

    fun <T> submit(task: Runnable?, result: T, priority: Double): Future<T> {
        if (task == null) {
            throw IllegalArgumentException("Task must not be null")
        }
        val future = newTaskFor(task, result, priority)
        execute(future)
        return future
    }

    fun <T> submit(task: Callable<T>?, priority: Double): Future<T> {
        if (task == null) {
            throw IllegalArgumentException("Task must not be null")
        }
        val future = newTaskFor(task, priority)
        execute(future)
        return future
    }

    private fun <T> newTaskFor(runnable: Runnable, result: T, priority: Double): RunnableFuture<T> {
        return PriorityFutureTask(runnable, result, priority)
    }

    private fun <T> newTaskFor(callable: Callable<T>, priority: Double): RunnableFuture<T> {
        return PriorityFutureTask(callable, priority)
    }

    override fun <T> newTaskFor(runnable: Runnable, value: T): RunnableFuture<T> {
        LOGGER.error("Do not use newTaskFor without priority")
        throw UnsupportedOperationException()
    }

    override fun <T> newTaskFor(callable: Callable<T>): RunnableFuture<T> {
        LOGGER.error("Do not use newTaskFor without priority")
        throw UnsupportedOperationException()
    }
}