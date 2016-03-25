package ru.ifmo.ctddev.isaev.classifier.weka;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.trees.J48;


/**
 * @author iisaev
 */
public class WekaJ48 extends WekaClassifier {

    @Override
    protected AbstractClassifier createClassifier() {
        J48 classifier = new J48();
        //TODO: tune
        return classifier;
    }
}
