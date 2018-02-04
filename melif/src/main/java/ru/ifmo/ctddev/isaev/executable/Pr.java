package ru.ifmo.ctddev.isaev.executable;

/**
 * @author iisaev
 */
public class Pr<F, S> {
    private F basic;
    private S parallel;

    public Pr(F basic, S parallel) {
        this.basic = basic;
        this.parallel = parallel;
    }

    public F getBasic() {
        return basic;
    }

    public S getParallel() {
        return parallel;
    }
}
