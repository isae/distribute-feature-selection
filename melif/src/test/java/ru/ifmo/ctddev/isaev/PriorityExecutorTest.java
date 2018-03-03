package ru.ifmo.ctddev.isaev;

import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;


/**
 * @author iisaev
 */
public class PriorityExecutorTest {
    PriorityExecutor executor = new PriorityExecutor(4, true);

    @Test
    @Ignore
    public void testPriorities() throws InterruptedException {
        executor.submit(() -> System.out.println(1), 1.0);
        executor.awaitTermination(2L, TimeUnit.SECONDS);
    }
}
