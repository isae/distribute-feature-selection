package ru.ifmo.ctddev.isaev.dataset;

/**
 * @author iisaev
 */
public abstract class DataSet {
    private final String name;

    protected DataSet(String name) {
        this.name = name;
    }

    public abstract FeatureDataSet toFeatureSet();

    public abstract InstanceDataSet toInstanceSet();

    public abstract int getFeatureCount();

    public abstract int getInstanceCount();

    public String getName() {
        return name;
    }
}
