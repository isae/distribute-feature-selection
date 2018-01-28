package ru.ifmo.ctddev.isaev.melif.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ktddev.isaev.AlgorithmConfig;
import ru.ifmo.ktddev.isaev.DataSet;
import ru.ifmo.ktddev.isaev.FoldsEvaluator;

import java.text.DecimalFormat;


/**
 * @author iisaev
 */
public class FeatureSelectionAlgorithm {

    public static final DecimalFormat FORMAT = new DecimalFormat("#.###");

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final FoldsEvaluator foldsEvaluator;

    protected final AlgorithmConfig config;

    protected final DataSet dataSet;

    public FeatureSelectionAlgorithm(AlgorithmConfig config, DataSet dataSet) {
        this.config = config;
        this.foldsEvaluator = config.getFoldsEvaluator();
        this.dataSet = dataSet;
    }
}
