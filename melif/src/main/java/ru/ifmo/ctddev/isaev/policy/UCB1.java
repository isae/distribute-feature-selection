package ru.ifmo.ctddev.isaev.policy;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;


/**
 * @author iisaev
 */
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
                2 * log(tries) / (getVisitedNumber()[i])
        );
    }

    @Override
    public void processPoint(Collection lastTries, Function<Integer, Optional<Double>> action) {
        int arm;
        synchronized (getHolder()) {
            if (tries < getArms()) {
                arm = tries;
            } else {
                arm = IntStream.range(0, getArms())
                        .filter(i -> lastTries.size() != 0)
                        .mapToObj(i -> i)
                        .sorted(Comparator.comparingDouble(i -> -armCost(i)))
                        .findFirst()
                        .get();
            }
            ++tries;
            //TODO: maybe update visitedSum temporarily while reward is not computed yet?
        }
        action.apply(arm).ifPresent(reward -> {
            synchronized (getHolder()) {
                ++getVisitedNumber()[arm];
                getVisitedSum()[arm] += reward;
            }
        });

    }
}
