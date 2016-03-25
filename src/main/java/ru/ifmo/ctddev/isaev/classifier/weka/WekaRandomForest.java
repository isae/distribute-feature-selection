package ru.ifmo.ctddev.isaev.classifier.weka;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.trees.RandomForest;


/**
 * @author iisaev
 */
public class WekaRandomForest extends WekaClassifier {

    @Override
    protected AbstractClassifier createClassifier() {
        RandomForest classifier = new RandomForest();
        //TODO: tune
        return classifier;
    }
}
