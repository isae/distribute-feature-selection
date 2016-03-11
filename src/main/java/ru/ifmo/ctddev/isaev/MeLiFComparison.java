package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.dataset.DataSet;


/**
 * @author iisaev
 */
public class MeLiFComparison {
    public static void main(String[] args) {
        DataSetReader dataSetReader = new DataSetReader();
        DataSet dataSet = dataSetReader.readCsv(args[0]);
        AlgorithmConfig config = new AlgorithmConfig(0.1, 10, 20, dataSet, 100);
        new SimpleMeLiF(config).run();
    }
}
