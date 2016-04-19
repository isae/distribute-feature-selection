package ru.ifmo.ctddev.isaev.dataset;

/**
 * @author iisaev
 */
public class DataSetPair {
    private DataSet trainSet;

    private DataSet testSet;

    public DataSetPair(DataSet trainSet, DataSet testSet) {
        this.trainSet = trainSet;
        this.testSet = testSet;
    }

    public DataSet getTrainSet() {
        return trainSet;
    }

    public DataSet getTestSet() {
        return testSet;
    }
}
