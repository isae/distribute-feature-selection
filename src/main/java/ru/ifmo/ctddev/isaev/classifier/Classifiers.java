package ru.ifmo.ctddev.isaev.classifier;


/**
 * @author iisaev
 */
public enum Classifiers {
    HOEFD(WekaHoeffdingTree.class),
    J48(WekaJ48.class),
    KNN(WekaKNN.class),
    LMT(WekaLMT.class),
    PERCEPTRON(WekaMultilayerPerceptron.class),
    PART(WekaPART.class),
    RAND_FOREST(WekaRandomForest.class),
    SVM(WekaSVM.class),
    NAIVE_BAYES(WekaNaiveBayes.class);

    private Class<? extends Classifier> typeToken;

    Classifiers(Class<? extends Classifier> typeToken) {
        this.typeToken = typeToken;
    }

    public Classifier newClassifier() {
        try {
            return typeToken.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to instantiate Classifier");
        }
    }
}
