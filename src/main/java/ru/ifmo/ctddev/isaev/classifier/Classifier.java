package ru.ifmo.ctddev.isaev.classifier;

import ru.ifmo.ctddev.isaev.DataSetTransformer;
import ru.ifmo.ctddev.isaev.dataset.DataSet;

import java.util.List;


/**
 * @author iisaev
 */
public interface Classifier {
    DataSetTransformer datasetTransformer = new DataSetTransformer();

    void train(DataSet trainDs);

    List<Double> test(DataSet testDs);
}
