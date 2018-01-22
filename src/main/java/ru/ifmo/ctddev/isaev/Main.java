package ru.ifmo.ctddev.isaev;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.dataset.Feature;
import ru.ifmo.ctddev.isaev.executable.ParallelRunner;
import ru.ifmo.ctddev.isaev.feature.FitCriterion;
import ru.ifmo.ctddev.isaev.feature.RelevanceMeasure;
import ru.ifmo.ctddev.isaev.feature.SpearmanRankCorrelation;
import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty;
import ru.ifmo.ctddev.isaev.feature.measure.VDM;
import ru.ifmo.ctddev.isaev.filter.DataSetFilter;
import ru.ifmo.ctddev.isaev.filter.PreferredSizeFilter;
import ru.ifmo.ctddev.isaev.folds.FoldsEvaluator;
import ru.ifmo.ctddev.isaev.folds.SequentalEvaluator;
import ru.ifmo.ctddev.isaev.melif.MeLiF;
import ru.ifmo.ctddev.isaev.melif.impl.BasicMeLiF;
import ru.ifmo.ctddev.isaev.melif.impl.MultiArmedBanditMeLiF;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelMeLiF;
import ru.ifmo.ctddev.isaev.melif.impl.PriorityQueueMeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.splitter.OrderSplitter;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * @author iisaev
 */
public class Main {
    private static final Options OPTIONS = new Options();

    private static final String ALGORITHM_ARG = "algorithm";

    private static final String HELP_ARG = "help";

    private static final String INPUT_ARG = "input-file";

    private static final String OUTPUT_ARG = "output-file";

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelRunner.class);

    private static final DataSetReader READER = new DataSetReader();

    private static final String MEASURES_ARG = "measures";

    private static final String FEATURES_ARG = "features";

    private static final Integer DEFAULT_FEATURES_NUMBER = 250;

    private static final String TEST_SIZE_ARG = "test_size";

    private static final Integer DEFAULT_FOLDS_NUMBER = 20;

    private static final String CLASSIFIER_ARG = "classifier";

    private static final String STOP_ARG = "stop_size";

    private static final Integer DEFAULT_LATCH_SIZE = 75;

    private static final String THREADS_ARG = "threads";

    private static final String DELTA_ARG = "delta";

    private static final double DEFAULT_STEP_SIZE = 0.1;

    static {
        OPTIONS.addOption(Option.builder("a")
                .longOpt(ALGORITHM_ARG)
                .hasArg()
                .argName("algorithmName")
                .desc(String.format(
                        "Algorithm to be used; Available options are %s ; Default is %s",
                        Arrays.toString(Algo.values()), Algo.PQMELIF)
                ).type(Algo.class)
                .build());
        OPTIONS.addOption(Option.builder("c")
                .longOpt(CLASSIFIER_ARG)
                .hasArg()
                .argName("classifier name")
                .desc(String.format(
                        "Classification algorithm to be used; Available options are %s; Default is %s",
                        Arrays.toString(Classifiers.values()), Classifiers.SVM)
                ).type(Classifiers.class)
                .build());
        OPTIONS.addOption(Option.builder("i")
                .longOpt(INPUT_ARG)
                .hasArg()
                .argName("path")
                .desc("CSV - file, containing input dataset; First line must contain objects labels (0 or 1), " +
                        "other lines must contain a particular feature value for all objects")
                .build());
        OPTIONS.addOption(Option.builder("o")
                .longOpt(OUTPUT_ARG)
                .hasArg()
                .argName("path")
                .desc("Path to output file; Default is ${input_file_name}.out")
                .build());
        OPTIONS.addOption(Option.builder("m")
                .longOpt(MEASURES_ARG)
                .hasArgs()
                .argName("measures")
                .valueSeparator(',')
                .desc("Used feature measures, comma-separated; Available options are " + Arrays.toString(Measure.values()))
                .type(Measure.class)
                .build());
        OPTIONS.addOption(Option.builder("n")
                .longOpt(FEATURES_ARG)
                .hasArg()
                .argName("feature number")
                .desc("Number of features to select; Default is " + DEFAULT_FEATURES_NUMBER)
                .type(Integer.class)
                .build());
        /*OPTIONS.addOption(Option.builder("t")
                .longOpt(THREADS_ARG)
                .hasArg()
                .argName("threads number")
                .desc("Number of threads to work on")
                .type(Integer.class)
                .build());*/
        OPTIONS.addOption(Option.builder("stop")
                .longOpt(STOP_ARG)
                .hasArg()
                .argName("number of points")
                .desc("Number of points to visit; Applicable for PQMELIF and MAMELIF only; Default is " + DEFAULT_LATCH_SIZE)
                .type(Integer.class)
                .build());
        OPTIONS.addOption(Option.builder("d")
                .longOpt(DELTA_ARG)
                .hasArg()
                .argName("step size")
                .desc("Coordinate descent step size; Default is " + DEFAULT_STEP_SIZE)
                .type(Double.class)
                .build());
        OPTIONS.addOption(Option.builder("t")
                .longOpt(TEST_SIZE_ARG)
                .hasArg()
                .argName("test size")
                .desc("Percent of instances that will be used for test; Default is " + DEFAULT_FOLDS_NUMBER)
                .type(Integer.class)
                .build());
        OPTIONS.addOption(Option.builder("h")
                .longOpt(HELP_ARG)
                .desc("Print all available arguments")
                .build());
    }

    private enum Algo {
        MELIF,
        MELIF_PLUS,
        PQMELIF,
        MAMELIF
    }

    private enum Measure {
        VDM(new VDM()),
        FC(new FitCriterion()),
        SU(new SymmetricUncertainty()),
        SPEARMAN(new SpearmanRankCorrelation());

        private final RelevanceMeasure measure;

        Measure(RelevanceMeasure measure) {
            this.measure = measure;
        }

        public RelevanceMeasure getMeasure() {
            return measure;
        }
    }

    private static <T extends Enum<T>> Optional<T> getEnumValue(CommandLine cl, String name, Class<T> clazz) {
        String enumConstantName = cl.getOptionValue(name);
        if (enumConstantName == null) {
            return Optional.empty();
        } else {
            return Optional.of(Enum.valueOf(clazz, enumConstantName.toUpperCase()));
        }
    }

    private static <T extends Enum<T>> EnumSet<T> getEnumValues(CommandLine cl, String name, Class<T> clazz) {
        String[] enumConstantNames = cl.getOptionValues(name);
        if (enumConstantNames == null) {
            return EnumSet.noneOf(clazz);
        } else {
            return EnumSet.copyOf(Arrays.stream(enumConstantNames).map(String::toUpperCase)
                    .map(str -> Enum.valueOf(clazz, str))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
    }

    private static Point[] generateStartingPoints(RelevanceMeasure[] measures) {
        Stream<Point> zeros = IntStream.range(0, measures.length).mapToObj(i -> {
            double[] res = new double[measures.length];
            res[i] = 1.0;
            return new Point(res);
        });
        double[] ones = new double[measures.length];
        Arrays.fill(ones, 1.0);
        return Stream.concat(zeros, Stream.of(new Point(ones))).toArray(Point[]::new);
    }

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine arguments = parser.parse(OPTIONS, args);
            if (arguments.hasOption(HELP_ARG)) {
                HelpFormatter formatter = new HelpFormatter();
                String header = "Filter input dataset using MELIF family of algorithms\n\n";
                String footer = "\nPlease report issues at https://github.com/isae/parallel-feature-selection/issues";
                formatter.printHelp("java -jar <jar_name>", header, OPTIONS, footer, true);
            } else {
                Algo algo = getEnumValue(arguments, ALGORITHM_ARG, Algo.class).map(algorithm -> {
                    LOGGER.info("Selected algorithm {}", algorithm);
                    return algorithm;
                }).orElseGet(() -> {
                    LOGGER.info("No algorithm specified; PQMELIF will be used");
                    return Algo.PQMELIF;
                });

                String startTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH:mm"));
                MDC.put("fileName", startTimeString + "/COMMON");

                String inputFileName;
                if (arguments.getArgs().length > 0) {
                    inputFileName = arguments.getArgs()[0];
                } else {
                    inputFileName = arguments.getOptionValue(INPUT_ARG);
                }
                if (inputFileName == null) {
                    LOGGER.info("Input file is not provided");
                    return;
                }
                LOGGER.info("Input file: {}", inputFileName);

                String outputFileName = Optional.ofNullable(arguments.getOptionValue(OUTPUT_ARG)).orElse(inputFileName + ".out");
                LOGGER.info("Output file: {}", outputFileName);

                RelevanceMeasure[] measures;
                List<RelevanceMeasure> measureList = getEnumValues(arguments, MEASURES_ARG, Measure.class)
                        .stream()
                        .map(Measure::getMeasure)
                        .collect(Collectors.toList());
                if (measureList.isEmpty()) {
                    measures = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
                } else {
                    measures = measureList.toArray(new RelevanceMeasure[0]);
                }
                LOGGER.info("Using the following measures: {}", Arrays.toString(measures));

                Point[] points = generateStartingPoints(measures);

                int featuresToSelect = Optional.ofNullable(arguments.getOptionValue(FEATURES_ARG)).map(Integer::valueOf).orElse(DEFAULT_FEATURES_NUMBER);
                LOGGER.info("{} features to select", featuresToSelect);
                DataSetFilter dataSetFilter = new PreferredSizeFilter(featuresToSelect);

                int testPercent = Optional.ofNullable(arguments.getOptionValue(TEST_SIZE_ARG)).map(Integer::valueOf).orElse(DEFAULT_FOLDS_NUMBER);
                LOGGER.info("{} percent of data will be used for testing", testPercent);

                double delta = Optional.ofNullable(arguments.getOptionValue(DELTA_ARG)).map(Double::valueOf).orElse(DEFAULT_STEP_SIZE);
                LOGGER.info("Step size: {}", delta);

                int latchSize = Optional.ofNullable(arguments.getOptionValue(STOP_ARG)).map(Integer::valueOf).orElse(DEFAULT_LATCH_SIZE);
                if (EnumSet.of(Algo.PQMELIF, Algo.MAMELIF).contains(algo)) {
                    LOGGER.info("{} points will be visited", latchSize);
                }

                Classifiers classifier = getEnumValue(arguments, CLASSIFIER_ARG, Classifiers.class).map(algorithm -> {
                    LOGGER.info("Selected classifier {}", algorithm);
                    return algorithm;
                }).orElseGet(() -> {
                    LOGGER.info("No classifier specified; SVM will be used");
                    return Classifiers.SVM;
                });

                DataSet dataSet = READER.readCsv(inputFileName);
                List<Integer> order = IntStream.range(0, dataSet.getInstanceCount()).mapToObj(i -> i).collect(Collectors.toList());
                Collections.shuffle(order);
                FoldsEvaluator foldsEvaluator = new SequentalEvaluator(
                        classifier,
                        dataSetFilter, new OrderSplitter(testPercent, order), new ScoreCalculator()
                );

                AlgorithmConfig config = new AlgorithmConfig(delta, foldsEvaluator, measures);
                LocalDateTime startTime = LocalDateTime.now();
                MeLiF meLif;
                int threads;
                switch (algo) {
                    case MELIF:
                        meLif = new BasicMeLiF(config, dataSet);
                        break;
                    case MELIF_PLUS:
                        threads = 2 * points.length;
                        meLif = new ParallelMeLiF(config, dataSet, threads);
                        break;
                    case PQMELIF:
                        threads = Runtime.getRuntime().availableProcessors();
                        meLif = new PriorityQueueMeLiF(config, dataSet, threads);
                        break;
                    case MAMELIF:
                        threads = Runtime.getRuntime().availableProcessors();
                        meLif = new MultiArmedBanditMeLiF(config, dataSet, threads, 2);
                        break;
                    default:
                        throw new IllegalArgumentException("No such algorithm: " + algo);
                }

                RunStats runStats = meLif.run(algo.name(), points, latchSize);
                LOGGER.info("Visited {} points; best point is {} with score {}", new Object[] {
                        runStats.getVisitedPoints(),
                        runStats.getBestResult().getPoint(),
                        runStats.getBestResult().getF1Score()
                });

                PrintWriter printWriter = new PrintWriter(outputFileName);
                printWriter.println(
                        String.join(", ", dataSet.toFeatureSet().getClasses().stream().map(String::valueOf).collect(Collectors.toList()))
                );
                List<Feature> selectedFeatures = runStats.getBestResult().getSelectedFeatures();
                for (Feature feature : selectedFeatures) {
                    printWriter.println(
                            String.join(", ", feature.getValues().stream().map(String::valueOf).collect(Collectors.toList()))
                    );
                }
                printWriter.close();
            }
        } catch (ParseException exp) {
            System.err.println("Invalid usage: " + exp.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error: " + e.getMessage(), e);
        }
    }
}
