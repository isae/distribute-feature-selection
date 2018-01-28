package ru.ifmo.ctddev.isaev

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.*

/**
 * @author iisaev
 */

fun fail(): Nothing = throw UnsupportedOperationException("Not implemented")

interface PriorityTask : Comparable<PriorityTask> {
    fun priority(): Double
    fun increasePriority(priorityIncrement: Double)
    override fun compareTo(other: PriorityTask): Int {
        return -java.lang.Double.compare(priority(), other.priority())
    }
}

class PriorityFutureTask<T>(callable: Callable<T>, private var priority: Double) : FutureTask<T>(callable), Runnable, PriorityTask {
    override fun increasePriority(priorityIncrement: Double) {
        priority += priorityIncrement
    }

    override fun priority(): Double = priority
}

class LinearSearchBlockingQueue<T>(private val capacity: Int) : BlockingQueue<T> {
    private val list = LinkedList<T>()
    private val semaphore = Semaphore(0)

    override fun contains(element: T): Boolean = fail()

    override fun addAll(elements: Collection<T>): Boolean = fail()

    override fun clear() = fail()

    override fun element(): T = fail()

    override fun take(): T {
        semaphore.acquire()
        synchronized(list) {
            val maxIndex = list.mapIndexed { i, t -> Pair(i, t as PriorityTask) }
                    .maxBy { (_, t) -> t.priority() }?.first
            return list.removeAt(maxIndex!!)
        }
    }

    override fun removeAll(elements: Collection<T>): Boolean = fail()

    override fun add(element: T): Boolean = fail()

    override fun offer(e: T): Boolean {
        return if (remainingCapacity() != 0) {
            synchronized(list) {
                list.add(e)
            }
            semaphore.release()
            true
        } else {
            false
        }
    }

    override fun offer(e: T, timeout: Long, unit: TimeUnit): Boolean = offer(e)

    override fun iterator(): MutableIterator<T> = fail()

    override fun peek(): T = fail()

    override fun put(e: T) = fail()

    override fun isEmpty(): Boolean = list.isEmpty()

    override fun remove(element: T): Boolean = fail()

    override fun remove(): T = fail()

    override fun containsAll(elements: Collection<T>): Boolean = fail()

    override fun drainTo(c: MutableCollection<in T>): Int {
        synchronized(list) {
            c.addAll(list)
            val size = list.size
            list.clear()
            return size
        }
    }

    override fun drainTo(c: MutableCollection<in T>, maxElements: Int): Int = fail()

    override fun retainAll(elements: Collection<T>): Boolean = fail()

    override fun remainingCapacity(): Int = capacity - list.size

    override fun poll(timeout: Long, unit: TimeUnit): T = fail()

    override fun poll(): T = fail()

    override val size: Int
        get() = list.size
}

val LOGGER: Logger = LoggerFactory.getLogger(PriorityThreadPoolExecutor::class.java)

class PriorityThreadPoolExecutor(poolSize: Int)
    : ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, LinearSearchBlockingQueue<Runnable>(64)) {

    fun increaseTasksPriorities(priorityIncrement: Double) {
        queue.map { it as PriorityTask }
                .forEach { it.increasePriority(priorityIncrement) }
    }

    override fun submit(task: Runnable): Future<*> = fail()

    override fun <T> submit(task: Runnable, result: T): Future<T> = fail()

    override fun <T> submit(task: Callable<T>): Future<T> = fail()

    fun <T> submitWithPriority(task: Callable<T>, priority: Double): Future<T> {
        val future = PriorityFutureTask(task, priority)
        execute(future)
        return future
    }

    override fun <T> newTaskFor(runnable: Runnable, value: T): RunnableFuture<T> = fail()

    override fun <T> newTaskFor(callable: Callable<T>): RunnableFuture<T> = fail()
}