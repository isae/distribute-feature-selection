package ru.ifmo.ctddev.isaev.policy;

import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;

/**
 * @author iisaev
 */
public class UCB1 extends BanditStrategy {

    private int tries = 0;

    public UCB1(int arms) {
        super(arms);
    }

    @Override
    public void processPoint(Function<Integer, Double> action) {
        int arm;
        if (tries < arms) {
            arm = tries;
        } else {
            arm = IntStream.range(0, arms)
                    .mapToObj(i -> i)
                    .sorted(Comparator.comparingDouble(i -> mu(i) + sqrt(2 * log(tries) / visitedNumber[i])))
                    .findFirst()
                    .get();
        }
        visitedSum[arm] += action.apply(arm);
        ++visitedNumber[arm];
        ++tries;
    }
}
