package ru.ifmo.ctddev.isaev;

import org.apache.commons.cli.*;

import java.util.Arrays;
import java.util.Optional;


/**
 * @author iisaev
 */
public class Main {
    public static final Options OPTIONS = new Options();

    public static final String ALGORITHM_ARG = "algorithm";

    public static final String HELP_ARG = "help";

    static {
        OPTIONS.addOption(Option.builder("a")
                .longOpt(ALGORITHM_ARG)
                .hasArg()
                .argName("algorithmName")
                .desc(String.format("Algorithm to be used; Available options: %s (case-insensitive)", Arrays.toString(Algo.values())))
                .type(Algo.class)
                .build());
        OPTIONS.addOption(Option.builder("help")
                .desc("Print all available arguments")
                .build());
    }

    private enum Algo {
        MELIF,
        MELIFPLUS,
        PQMELIF,
        MAMELIF
    }

    private static <T extends Enum<T>> Optional<T> getEnumValue(CommandLine cl, String name, Class<T> clazz) {
        String enumConstantName = cl.getOptionValue(name);
        if (enumConstantName == null) {
            return Optional.empty();
        } else {
            return Optional.of(Enum.valueOf(clazz, enumConstantName.toUpperCase()));
        }
    }

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine arguments = parser.parse(OPTIONS, args);
            if (arguments.hasOption(HELP_ARG)) {
                HelpFormatter formatter = new HelpFormatter();
                String header = "Filter input dataset using MELIF-related algorithms\n\n";
                String footer = "\nPlease report issues at https://github.com/isae/parallel-feature-selection/issues";
                formatter.printHelp("java -jar <jar_name>", header, OPTIONS, footer, true);
            } else {
                Algo algo = getEnumValue(arguments, ALGORITHM_ARG, Algo.class).orElseGet(() -> {
                    System.out.println("No algorithm specified; PQMELIF will be used");
                    return Algo.PQMELIF;
                });
                System.out.println(algo);
            }
        } catch (ParseException exp) {
            System.err.println("Invalid usage: " + exp.getMessage());
        }
    }
}
