package ru.ifmo.ctddev.isaev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.dataset.Feature;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class DataSetReader {

    protected final Logger logger = LoggerFactory.getLogger(DataSetReader.class);

    public FeatureDataSet readCsv(String path) {
        return readDataset(new File(path), ",");
    }

    public FeatureDataSet readCsv(File file) {
        return readDataset(file, ",");
    }

    private FeatureDataSet readDataset(File file, String delimiter) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            final boolean[] firstLine = {true};
            List<Integer> classes = new ArrayList<>();
            List<Feature> features = new ArrayList<>();
            final int[] counter = {0};
            reader.lines().filter(l -> !l.contains("NaN")).forEach(line -> {
                List<Integer> parsedRow = Arrays.stream(line.split(delimiter))
                        .mapToInt(Integer::valueOf)
                        .boxed()
                        .collect(Collectors.toList());
                if (firstLine[0]) {
                    firstLine[0] = false;
                    classes.addAll(parsedRow);
                } else {
                    features.add(new Feature("feature" + (++counter[0]), parsedRow));
                }
            });
            logger.debug("Read dataset by path {}; {} classes; {} features", new Object[] {file.getAbsoluteFile(), classes.size(), features.size()});
            return new FeatureDataSet(features, classes, file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File not found", e);
        }
    }
}
