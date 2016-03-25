package ru.ifmo.ctddev.isaev.classifier.weka;

import ru.ifmo.ctddev.isaev.classifier.Classifier;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import weka.classifiers.AbstractClassifier;
import weka.core.Instances;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public abstract class WekaClassifier implements Classifier {

    private AbstractClassifier classifier;

    private Instances instances;

    private volatile boolean trained = false;

    protected abstract AbstractClassifier createClassifier();

    @Override
    public void train(DataSet trainDs) {
        if (trained) {
            throw new IllegalStateException("Classifier has been already trained");
        }
        try {
            trained = true;
            instances = datasetTransformer.toInstances(trainDs.toInstanceSet());
            classifier = createClassifier();
            classifier.buildClassifier(instances);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to train on given dataset", e);
        }
    }

    @Override
    public List<Double> test(DataSet testDs) {
        if (!trained) {
            throw new IllegalStateException("Classifier is not trained yet");
        }
        return testDs.toInstanceSet().getInstances().stream().map(datasetTransformer::toInstance).map(i -> {
            try {
                i.setDataset(instances);
                return classifier.classifyInstance(i);
            } catch (Exception e) {
                throw new IllegalArgumentException("Given dataset is not valid", e);
            }
        }).collect(Collectors.toList());
    }
}
