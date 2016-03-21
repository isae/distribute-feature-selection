package ru.ifmo.ctddev.isaev;

import java.util.function.Consumer;


/**
 * @author iisaev
 */
public class OptimizationPoint extends Point {

    private final Point parent;

    public OptimizationPoint(Point parent, Consumer<double[]> diff) {
        super(parent.getCoordinates().clone());
        diff.accept(getCoordinates());
        this.parent = parent;
    }

    public Point getParent() {
        return parent;
    }
}
