package ru.ifmo.ctddev.isaev.classifier.weka;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.trees.HoeffdingTree;


/**
 * @author iisaev
 */
public class WekaHoeffdingTree extends WekaClassifier {

    @Override
    protected AbstractClassifier createClassifier() {
        HoeffdingTree classifier = new HoeffdingTree();
        //TODO: tune
        return classifier;
    }
}
