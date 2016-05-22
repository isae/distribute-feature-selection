package ru.ifmo.ctddev.isaev.policy;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Comparator;
import java.util.Optional;
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

    private final double lambda;

    public UCB1(int arms) {
        this(arms, 1.0);
    }

    public UCB1(int arms, double lambda) {
        super(arms);
        this.lambda = lambda;
    }


    private double armCost(int i) {
        return mu(i) + sqrt(
                2 * log(tries) / (getVisitedNumber()[i] * lambda)
        );
    }

    @Override
    public void processPoint(Function<Integer, Optional<Double>> action) {
        int arm;
        synchronized (getHolder()) {
            if (tries < getArms()) {
                arm = tries;
            } else {
                arm = IntStream.range(0, getArms())
                        .mapToObj(i -> i)
                        .sorted(Comparator.comparingDouble(i -> -armCost(i)))
                        .findFirst()
                        .get();
            }
            ++getVisitedNumber()[arm];
            ++tries;
            //TODO: maybe update visitedSum temporarily while reward is not computed yet?
        }
        action.apply(arm).ifPresent(reward -> {
            synchronized (getHolder()) {
                getVisitedSum()[arm] += reward;
            }
        });

    }
}
