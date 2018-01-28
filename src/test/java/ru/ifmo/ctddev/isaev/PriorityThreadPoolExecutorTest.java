package ru.ifmo.ctddev.isaev;

import org.junit.Test;

import java.util.concurrent.TimeUnit;


/**
 * @author iisaev
 */
public class PriorityThreadPoolExecutorTest {
    PriorityThreadPoolExecutor executor = new PriorityThreadPoolExecutor(4);

    @Test
    public void testPriorities() throws InterruptedException {
        executor.submit(() -> System.out.println(1), 1.0);
        executor.awaitTermination(2L, TimeUnit.SECONDS);
    }
}
