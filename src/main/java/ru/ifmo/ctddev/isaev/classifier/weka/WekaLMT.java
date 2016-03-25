package ru.ifmo.ctddev.isaev.classifier.weka;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.trees.LMT;


/**
 * @author iisaev
 */
public class WekaLMT extends WekaClassifier {

    @Override
    protected AbstractClassifier createClassifier() {
        LMT classifier = new LMT();
        //TODO: tune
        return classifier;
    }
}
