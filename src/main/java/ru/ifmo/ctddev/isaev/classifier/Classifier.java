package ru.ifmo.ctddev.isaev.classifier;

import ru.ifmo.ctddev.isaev.DatasetTransformer;
import ru.ifmo.ctddev.isaev.dataset.DataSet;

import java.util.List;


/**
 * @author iisaev
 */
public interface Classifier {
    DatasetTransformer datasetTransformer = new DatasetTransformer();

    void train(DataSet trainDs);

    List<Double> test(DataSet testDs);
}
