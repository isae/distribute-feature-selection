package ru.ifmo.ctddev.isaev.classifier.weka;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.functions.LinearRegression;


/**
 * @author iisaev
 */
@Deprecated
public class WekaLinearRegression extends WekaClassifier {

    @Override
    protected AbstractClassifier createClassifier() {
        LinearRegression classifier = new LinearRegression();
        //TODO: tune
        return classifier;
    }
}
