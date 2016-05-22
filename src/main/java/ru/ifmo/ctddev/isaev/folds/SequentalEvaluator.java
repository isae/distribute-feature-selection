package ru.ifmo.ctddev.isaev.folds;

import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.filter.DataSetFilter;
import ru.ifmo.ctddev.isaev.melif.impl.FeatureSelectionAlgorithm;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.result.SelectionResult;
import ru.ifmo.ctddev.isaev.splitter.DataSetSplitter;


/**
 * @author iisaev
 */
public class SequentalEvaluator extends FoldsEvaluator {

    public SequentalEvaluator(Classifiers classifiers, DataSetFilter dataSetFilter, DataSetSplitter dataSetSplitter) {
        super(classifiers, dataSetSplitter, dataSetFilter);
    }


    public SelectionResult getSelectionResult(DataSet dataSet, Point point, RunStats stats) {
        FeatureDataSet filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.getMeasures());
        double f1Score = getF1Score(dataSet, point, stats.getMeasures());
        logger.debug("Point {}; F1 score: {}", point, FeatureSelectionAlgorithm.FORMAT.format(f1Score));
        SelectionResult result = new SelectionResult(filteredDs.getFeatures(), point, f1Score);
        stats.updateBestResult(result);
        return result;
    }

    @Override
    public String getName() {
        return "Seq";
    }
}
