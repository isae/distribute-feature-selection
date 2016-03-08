package ru.ifmo.ctddev.isaev.classifier;

import ru.ifmo.ctddev.isaev.dataset.DataSet;
import weka.classifiers.functions.SMO;
import weka.core.Instances;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class SVM implements Classifier {

    private SMO svm = new SMO();

    @Override
    public void train(DataSet trainDs) {
        try {
            Instances data = datasetTransformer.toInstances(trainDs.toFeatureSet());
            svm = new SMO();
            svm.setBuildLogisticModels(true);
            svm.buildClassifier(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to train on given dataset", e);
        }
    }

    @Override
    public List<Double> test(DataSet testDs) {
        return testDs.toInstanceSet().getInstances().stream().map(datasetTransformer::toInstance).map(i -> {
            try {
                return svm.classifyInstance(i);
            } catch (Exception e) {
                throw new IllegalArgumentException("Given dataset is not valid", e);
            }
        }).collect(Collectors.toList());
    }
}
