package ru.ifmo.ctddev.isaev.splitter;

import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.dataset.DataSetPair;

import java.util.List;


/**
 * @author iisaev
 */
public interface DatasetSplitter {
    List<DataSetPair> split(DataSet original);
}
