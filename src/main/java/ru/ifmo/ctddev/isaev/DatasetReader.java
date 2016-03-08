package ru.ifmo.ctddev.isaev;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class DatasetReader {
    public Dataset readCsv(String path) {
        return readDataset(path, ",");
    }

    private Dataset readDataset(String path, String delimiter) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            final boolean[] firstLine = {true};
            List<Integer> classes = new ArrayList<>();
            List<Feature> features = new ArrayList<>();
            reader.lines().forEach(line -> {
                List<Integer> parsedRow = Arrays.stream(line.split(delimiter))
                        .mapToInt(Integer::valueOf)
                        .boxed()
                        .collect(Collectors.toList());
                if (firstLine[0]) {
                    firstLine[0] = false;
                    classes.addAll(parsedRow);
                } else {
                    features.add(new Feature(parsedRow));
                }
            });
            return new Dataset(features, classes);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File not found", e);
        }
    }
}
