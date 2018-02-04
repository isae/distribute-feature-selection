package ru.ifmo.ctddev.isaev.policy;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class EpsilonGreedy extends BanditStrategy {
    private final double epsilon;

    private static final Random RANDOM = new Random();

    public EpsilonGreedy(double epsilon, int arms) {
        super(arms);
        this.epsilon = epsilon;
        if (epsilon < 0 | epsilon > 1) {
            throw new IllegalArgumentException(String.format("Invalid epsilon: %f", epsilon));
        }
    }

    @Override
    public void processPoint(Collection lastTries, Function<Integer, Optional<Double>> action) {
        int arm;
        if (RANDOM.nextDouble() < epsilon) {
            arm = RANDOM.nextInt(getArms());
        } else {
            arm = IntStream.range(0, getArms())
                    .mapToObj(i -> i)
                    .sorted(Comparator.comparingDouble(this::mu))
                    .findFirst()
                    .get();
        }
        double result = action.apply(arm).orElseThrow(() ->
                new IllegalStateException("Optional.EMPTY is not supported in eps-greedy")
        );
        ++getVisitedNumber()[arm];
        getVisitedSum()[arm] += result;
    }
}
