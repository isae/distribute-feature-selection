package ru.ifmo.ctddev.isaev.dataset;

/**
 * @author iisaev
 */
public interface DataSet {
    FeatureDataSet toFeatureSet();
    InstanceDataSet toInstanceSet();
}
