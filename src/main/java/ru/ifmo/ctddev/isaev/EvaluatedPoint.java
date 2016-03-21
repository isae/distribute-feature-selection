package ru.ifmo.ctddev.isaev;

/**
 * @author iisaev
 */
public class EvaluatedPoint extends Point {

    private final SelectionResult result;

    public EvaluatedPoint(Point point, SelectionResult result) {
        super(point);
        this.result = result;
    }

    public SelectionResult getResult() {
        return result;
    }
}
