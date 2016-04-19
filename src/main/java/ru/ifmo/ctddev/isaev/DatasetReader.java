package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.dataset.Feature;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class DataSetReader {
    public FeatureDataSet readCsv(String path) {
        return readDataset(path, ",");
    }

    private FeatureDataSet readDataset(String path, String delimiter) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
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
            return new FeatureDataSet(features, classes, path);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File not found", e);
        }
    }
}
