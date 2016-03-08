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
            List<List<Double>> features = new ArrayList<>();
            reader.lines().forEach(line -> {
                if (firstLine[0]) {
                    firstLine[0] = false;
                    classes.addAll(
                            Arrays.stream(line.split(delimiter))
                                    .mapToInt(Integer::valueOf)
                                    .boxed()
                                    .collect(Collectors.toList())
                    );
                } else {
                    List<Double> feature = Arrays.stream(line.split(delimiter))
                            .mapToDouble(Double::valueOf)
                            .boxed()
                            .collect(Collectors.toList());
                    features.add(feature);
                }
            });
            return new Dataset(features, classes);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File not found", e);
        }
    }
}
