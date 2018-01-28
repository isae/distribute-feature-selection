package ru.ifmo.ctddev.isaev.result;

/**
 * @author iisaev
 */
public class PriorityPoint extends Point {
    private double priority;

    public PriorityPoint(double priority, double... coordinates) {
        super(coordinates);
        this.priority = priority;
    }

    public PriorityPoint(Point value) {
        this(1.0, value.getCoordinates());
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }
}
