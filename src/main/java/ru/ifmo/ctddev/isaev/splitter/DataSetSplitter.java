package ru.ifmo.ctddev.isaev.splitter;

import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.dataset.DataSetPair;

import java.util.List;


/**
 * @author iisaev
 */
public abstract class DataSetSplitter {
    protected final org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final int testPercent;

    public int getTestPercent() {
        return testPercent;
    }

    public DataSetSplitter(int testPercent) {
        this.testPercent = testPercent;
        logger.info("Initialized dataset splitter with test percent {}", testPercent);
    }

    public abstract List<DataSetPair> split(DataSet original);
}
