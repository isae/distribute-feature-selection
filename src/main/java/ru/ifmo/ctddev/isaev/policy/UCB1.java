package ru.ifmo.ctddev.isaev.policy;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;

/**
 * @author iisaev
 */
@ThreadSafe
public class UCB1 extends BanditStrategy {

    private int tries = 0;

    public UCB1(int arms) {
        super(arms);
    }

    @Override
    public void processPoint(Function<Integer, Double> action) {
        int arm;
        synchronized (getHolder()) {
            //if (tries < getArms()) {
            //  arm = tries;
            //} else {
            arm = IntStream.range(0, getArms())
                    .mapToObj(i -> i)
                    .sorted(Comparator.comparingDouble(i -> mu(i) + sqrt(2 * log(tries) / getVisitedNumber()[i])))
                    .findFirst()
                    .get();
            //}
            ++getVisitedNumber()[arm];
            ++tries;
            //TODO: maybe update visitedSum temporarily while reward is not computed yet
        }
        double reward = action.apply(arm);
        synchronized (getHolder()) {
            getVisitedSum()[arm] += reward;
        }
    }
}
