package ru.ifmo.ctddev.isaev.executor;

import java.util.Comparator;
import java.util.PriorityQueue;


/**
 * @author iisaev
 */
public class PriorityQueueTest {
    static class Tst{
        int value;

        public Tst(int value) {
            this.value = value;
        }
    }
    public static void main(String[] args) {
        Comparator<Tst> comparator = (o1, o2) -> -Integer.compare(o1.value,o2.value);

        PriorityQueue<Tst> queue = new PriorityQueue<>(50,comparator);
        queue.add(new Tst(7));
        queue.add(new Tst(2));
        queue.add(new Tst(4));
        queue.add(new Tst(3));
        queue.add(new Tst(5));
        queue.add(new Tst(6));
        while (!queue.isEmpty()){
            System.out.println(queue.poll().value);
        }
    }
}
