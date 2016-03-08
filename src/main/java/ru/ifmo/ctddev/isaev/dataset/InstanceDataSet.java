package ru.ifmo.ctddev.isaev.dataset;

import java.util.List;


/**
 * @author iisaev
 */
public class InstanceDataSet implements DataSet {
    private final List<DataInstance> instances;

    protected InstanceDataSet(List<DataInstance> instances) {
        this.instances = instances;
    }

    @Override
    public FeatureDataSet toFeatureSet() {
        throw new UnsupportedOperationException("Method is not implemented");
    }

    @Override
    public InstanceDataSet toInstanceSet() {
        return this;
    }

    public List<DataInstance> getInstances() {
        return instances;
    }
}
