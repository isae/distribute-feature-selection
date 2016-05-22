package ru.ifmo.ctddev.isaev.executable;

import ru.ifmo.ctddev.isaev.filter.DatasetFilter;
import ru.ifmo.ctddev.isaev.filter.PreferredSizeFilter;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.splitter.DatasetSplitter;
import ru.ifmo.ctddev.isaev.splitter.OrderSplitter;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author iisaev
 */
public class LogsSearcher {
    public static void main(String[] args) throws FileNotFoundException {
        File logsDir = new File(args[0]);
        assert logsDir.exists();
        assert logsDir.isDirectory();
        File dataSetDir = new File(args[1]);
        assert dataSetDir.exists();
        assert dataSetDir.isDirectory();
        DataSetReader dataSetReader = new DataSetReader();

        PrintWriter writer = new PrintWriter("result.out");
        
        DatasetFilter datasetFilter = new PreferredSizeFilter(100);
        RelevanceMeasure[] measures = new RelevanceMeasure[]{new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        Arrays.asList(logsDir.listFiles()).stream()
                .filter(f -> !f.getName().startsWith("."))
                .forEach(file -> {
                    String dataSetName = file.getName().split("\\-")[0];
                    if (!dataSetName.equals("COMMON")) {
                        DataSet dataSet = dataSetReader.readCsv(dataSetDir.toPath().resolve(dataSetName).toFile());
                        List<Integer> order = IntStream.range(0, dataSet.getInstanceCount()).mapToObj(i -> i).collect(Collectors.toList());
                        Collections.shuffle(order);
                        DatasetSplitter tenFoldSplitter = new OrderSplitter(10, order);
                        System.out.println(dataSetName);
                        writer.println(dataSetName);
                        List<Point> points = new ArrayList<>();
                        try {
                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            reader.lines()
                                    .filter(str -> stringNeeded(str))
                                    .map(s -> {
                                        boolean f = true;
                                        return s.split("best\\spoint")[1].split("\\[")[1].split("\\]")[0];
                                    })
                                    .forEach(s -> {
                                        String[] spl = s.split("\\s*,\\s*");
                                        Point result = new Point(Double.valueOf(spl[0]), Double.valueOf(spl[1]), Double.valueOf(spl[2]), Double.valueOf(spl[3]));
                                        points.add(result);
                                    });
                            List<Double> basicScores = tenFoldSplitter.split(
                                    datasetFilter.filterDataSet(dataSet.toFeatureSet(), points.get(0), measures)
                            ).stream()
                                    .map(MultipleComparison::getF1Score)
                                    .collect(Collectors.toList());

                            List<Double> parallelScores = tenFoldSplitter.split(
                                    datasetFilter.filterDataSet(dataSet.toFeatureSet(), points.get(1), measures)
                            )
                                    .stream().map(MultipleComparison::getF1Score)
                                    .collect(Collectors.toList());
                            assert basicScores.size() == parallelScores.size();
                            System.out.format("Size: %d\n",basicScores.size());
                            System.out.println(Arrays.toString(basicScores.toArray()));
                            writer.println(Arrays.toString(basicScores.toArray()));
                            System.out.println(Arrays.toString(parallelScores.toArray()));
                            writer.println(Arrays.toString(parallelScores.toArray()));
                            writer.println();
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
        writer.close();
    }

    private static boolean stringNeeded(String str) {
        return str.contains("best point is ");
    }
}
