package ru.ifmo.ctddev.isaev.classifier.weka;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.rules.PART;


/**
 * @author iisaev
 */
public class WekaPART extends WekaClassifier {

    @Override
    protected AbstractClassifier createClassifier() {
        PART classifier = new PART();
        //TODO: tune
        return classifier;
    }
}
